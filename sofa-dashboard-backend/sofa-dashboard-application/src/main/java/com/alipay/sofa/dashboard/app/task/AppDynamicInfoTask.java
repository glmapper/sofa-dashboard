/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.dashboard.app.task;

import com.alipay.sofa.dashboard.app.actuator.ActuatorMonitorManager;
import com.alipay.sofa.dashboard.app.actuator.RestTemplateClient;
import com.alipay.sofa.dashboard.app.cache.DynamicActuatorDataCacheManager;
import com.alipay.sofa.dashboard.app.zookeeper.ZookeeperApplicationManager;
import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.model.Application;
import com.alipay.sofa.dashboard.model.monitor.DetailThreadInfo;
import com.alipay.sofa.dashboard.model.monitor.DetailsItem;
import com.alipay.sofa.dashboard.model.monitor.MemoryHeapInfo;
import com.alipay.sofa.dashboard.model.monitor.MemoryNonHeapInfo;
import com.alipay.sofa.dashboard.utils.DashboardUtil;
import com.alipay.sofa.dashboard.utils.FixedQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用动态数据定时调度器，主要用于同步应用监控数据中的实时动态数据，如内存使用、线程状态等
 *
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/9 5:26 PM
 * @since:
 **/
@Component
public class AppDynamicInfoTask {

    @Autowired
    private ZookeeperApplicationManager zookeeperApplicationManager;

    @Autowired
    private RestTemplateClient          restTemplateClient;

    @Scheduled(fixedRate = 15000)
    public void fetchAppDynamicInfo() {
        String currentDataKey = DashboardUtil.getCurrentDataKey();
        Map<String, Set<Application>> applications = zookeeperApplicationManager.getApplications();
        Set<String> appNames = applications.keySet();
        appNames.forEach((appName) -> {
            Set<Application> appInstances = applications.get(appName);
            appInstances.forEach((app) -> {
                doCalculateThreads(app, currentDataKey);
                doCalculateMemoryHeap(app, currentDataKey);
                doCalculateMemoryNonHeap(app, currentDataKey);
            });
        });
    }

    /**
     * 获取线程状态数据
     * @param app
     * @param time
     */
    private void doCalculateThreads(Application app, String time) {
        Map threadsLiveMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.threads.live");
        Map threadsDaemonMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.threads.daemon");
        Map threadsPeakMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.threads.peak");
        String appId = DashboardUtil.simpleEncode(app.getHostName(), app.getPort());
        Map<String, FixedQueue<DetailThreadInfo>> cacheDetailThreads = DynamicActuatorDataCacheManager
            .getCacheDetailThreads();
        FixedQueue<DetailThreadInfo> queue = cacheDetailThreads.get(appId);
        if (queue == null) {
            queue = new FixedQueue<>(4);
        }
        DetailThreadInfo threadInfo = new DetailThreadInfo();

        DetailsItem live = new DetailsItem();

        live.setTags("LIVE");
        live.setNums(parseMeasurements(threadsLiveMap).intValue());
        live.setTime(time);

        DetailsItem peak = new DetailsItem();
        peak.setTags("PEAK");
        peak.setNums(parseMeasurements(threadsPeakMap).intValue());
        peak.setTime(time);

        DetailsItem daemon = new DetailsItem();
        daemon.setTags("DAEMON");
        daemon.setNums(parseMeasurements(threadsDaemonMap).intValue());
        daemon.setTime(time);

        threadInfo.setLive(live);
        threadInfo.setPeak(peak);
        threadInfo.setDaemon(daemon);
        queue.offer(threadInfo);

        cacheDetailThreads.put(appId, queue);

    }

    private Number parseMeasurements(Map threadsLiveMap) {
        Object measurements = threadsLiveMap.get("measurements");
        if (measurements instanceof List) {
            List data = (List) measurements;
            if (data.size() > 0) {
                Object o = data.get(0);
                if (o instanceof Map) {
                    Object result = ((Map) o).get("value");
                    if (result instanceof Number) {
                        return ((Number) result);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * 获取堆内存数据
     * @param app
     * @param time
     */
    private void doCalculateMemoryHeap(Application app, String time) {
        String appId = DashboardUtil.simpleEncode(app.getHostName(), app.getPort());
        Map<String, FixedQueue<MemoryHeapInfo>> cacheHeapMemory = DynamicActuatorDataCacheManager
            .getCacheHeapMemory();
        FixedQueue<MemoryHeapInfo> queue = cacheHeapMemory.get(appId);
        if (queue == null) {
            queue = new FixedQueue<>(4);
        }
        Map committedMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.memory.committed?tag=area:heap");
        Number committed = parseMeasurements(committedMap);
        Map usedMap = restTemplateClient.doRequest(app.getHostName() + SofaDashboardConstants.COLON
                                                   + app.getPort(),
            "/actuator/metrics/jvm.memory.used?tag=area:heap");
        Number usedNum = parseMeasurements(usedMap);

        DetailsItem used = new DetailsItem();
        used.setTags("used");
        used.setNums(Math.round(usedNum.intValue() / (1024 * 1024 * 8)));
        used.setTime(time);

        DetailsItem size = new DetailsItem();
        size.setTags("size");
        size.setNums((committed.intValue() / (1024 * 1024 * 8)));
        size.setTime(time);

        MemoryHeapInfo heap = new MemoryHeapInfo();
        heap.setSize(size);
        heap.setUsed(used);
        queue.offer(heap);
        cacheHeapMemory.put(appId, queue);
    }

    /**
     * 获取非堆区数据
     * @param app
     * @param time
     */
    private void doCalculateMemoryNonHeap(Application app, String time) {
        String appId = DashboardUtil.simpleEncode(app.getHostName(), app.getPort());
        Map<String, FixedQueue<MemoryNonHeapInfo>> cacheNonHeapMemory = DynamicActuatorDataCacheManager
            .getCacheNonHeapMemory();
        FixedQueue<MemoryNonHeapInfo> queue = cacheNonHeapMemory.get(appId);

        if (queue == null) {
            queue = new FixedQueue<>(4);
        }

        Map committedMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.memory.committed?tag=area:nonheap");
        Number committed = parseMeasurements(committedMap);
        Map usedMap = restTemplateClient.doRequest(app.getHostName() + SofaDashboardConstants.COLON
                                                   + app.getPort(),
            "/actuator/metrics/jvm.memory.used?tag=area:nonheap");
        Number usedNum = parseMeasurements(usedMap);
        Map metaspaceMap = restTemplateClient.doRequest(
            app.getHostName() + SofaDashboardConstants.COLON + app.getPort(),
            "/actuator/metrics/jvm.memory.used?tag=area:nonheap&tag=id:Metaspace");
        Number metaspaceNum = parseMeasurements(metaspaceMap);

        DetailsItem used = new DetailsItem();
        used.setTags("used");
        used.setNums(Math.round(usedNum.intValue() / (8 * 1024 * 1024)));
        used.setTime(time);

        DetailsItem size = new DetailsItem();
        size.setTags("size");
        size.setNums(Math.round(committed.intValue() / (8 * 1024 * 1024)));
        size.setTime(time);

        DetailsItem metaspace = new DetailsItem();
        metaspace.setTags("metaspace");
        metaspace.setNums(Math.round(metaspaceNum.intValue() / (8 * 1024 * 1024)));
        metaspace.setTime(time);

        MemoryNonHeapInfo nonHeap = new MemoryNonHeapInfo();
        nonHeap.setMetaspace(metaspace);
        nonHeap.setSize(size);
        nonHeap.setUsed(used);
        queue.offer(nonHeap);
        cacheNonHeapMemory.put(appId, queue);
    }
}
