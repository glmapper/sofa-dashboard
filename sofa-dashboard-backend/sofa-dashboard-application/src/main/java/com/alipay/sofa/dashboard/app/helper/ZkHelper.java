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
package com.alipay.sofa.dashboard.app.helper;

import com.alipay.sofa.dashboard.app.ZookeeperApplicationManager;
import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.model.AppInfo;
import com.alipay.sofa.dashboard.zookeeper.ZkCommandClient;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/2/14 5:24 PM
 * @since:
 **/
@Component
public class ZkHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkHelper.class);

    @Autowired
    ZkCommandClient             zkCommandClient;
    @Autowired
    ZookeeperApplicationManager zookeeperApplicationManager;

    /**
     * 根据应用名获取当前应用的所有实例
     *
     * @param appName
     * @return
     * @throws Exception
     */
    public List<AppInfo> getArkAppFromZookeeper(String appName, String pluginName, String version) throws Exception {
        List<AppInfo> applications = new ArrayList<>();
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        // 根据应用名获取所有实例信息
        List<String> apps = curatorClient.getChildren().forPath(SofaDashboardConstants.SOFA_ARK_ROOT + SofaDashboardConstants.SEPARATOR + appName);
        // 遍历实例IP，生成应用元数据
        apps.forEach((ip) -> {
            try {
                AppInfo application = new AppInfo();
                application.setAppName(appName);
                application.setHostName(ip);
                // todo in optimized-to-ark-console
                application.setAppState("");
                applications.add(application);
            } catch (Exception e) {
                LOGGER.error("Failed to get ark app.", e);
            }
        });
        return applications;
    }

    /**
     * 获取当前应用实例的个数
     *
     * @return
     */
    public int getArkAppCount(String appName) throws Exception {
        CuratorFramework curatorClient = zkCommandClient.getCuratorClient();
        List<String> ips = curatorClient.getChildren().forPath(
            SofaDashboardConstants.SOFA_ARK_ROOT + SofaDashboardConstants.SEPARATOR + appName);
        return ips != null ? ips.size() : 0;
    }

}
