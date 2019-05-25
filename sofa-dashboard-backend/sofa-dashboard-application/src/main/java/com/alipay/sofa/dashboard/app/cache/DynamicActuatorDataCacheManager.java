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
package com.alipay.sofa.dashboard.app.cache;

import com.alipay.sofa.dashboard.model.monitor.DetailThreadInfo;
import com.alipay.sofa.dashboard.model.monitor.MemoryHeapInfo;
import com.alipay.sofa.dashboard.model.monitor.MemoryNonHeapInfo;
import com.alipay.sofa.dashboard.utils.FixedQueue;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/24 10:23 AM
 * @since:
 **/
public class DynamicActuatorDataCacheManager {

    private static Map<String, FixedQueue<DetailThreadInfo>>  cacheDetailThreads = new ConcurrentHashMap<>();

    private static Map<String, FixedQueue<MemoryHeapInfo>>    cacheHeapMemory    = new ConcurrentHashMap<>();

    private static Map<String, FixedQueue<MemoryNonHeapInfo>> cacheNonHeapMemory = new ConcurrentHashMap<>();

    public static void setCacheDetailThreads(String key, FixedQueue<DetailThreadInfo> queue) {
        if (StringUtils.isEmpty(key) || queue == null) {
            return;
        }
        cacheDetailThreads.put(key, queue);
    }

    public static void setCacheHeapMemory(String key, FixedQueue<MemoryHeapInfo> queue) {
        if (StringUtils.isEmpty(key) || queue == null) {
            return;
        }
        cacheHeapMemory.put(key, queue);
    }

    public static void setCacheNonHeapMemory(String key, FixedQueue<MemoryNonHeapInfo> queue) {
        if (StringUtils.isEmpty(key) || queue == null) {
            return;
        }
        cacheNonHeapMemory.put(key, queue);
    }

    public static Map<String, FixedQueue<DetailThreadInfo>> getCacheDetailThreads() {
        return cacheDetailThreads;
    }

    public static Map<String, FixedQueue<MemoryHeapInfo>> getCacheHeapMemory() {
        return cacheHeapMemory;
    }

    public static Map<String, FixedQueue<MemoryNonHeapInfo>> getCacheNonHeapMemory() {
        return cacheNonHeapMemory;
    }
}
