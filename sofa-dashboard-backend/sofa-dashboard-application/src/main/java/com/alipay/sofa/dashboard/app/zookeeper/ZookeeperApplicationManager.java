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
package com.alipay.sofa.dashboard.app.zookeeper;

import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import com.alipay.sofa.dashboard.model.AppModel;
import com.alipay.sofa.dashboard.model.Application;
import com.alipay.sofa.dashboard.spi.ApplicationManager;
import com.alipay.sofa.dashboard.utils.ObjectBytesUtil;
import com.alipay.sofa.dashboard.zookeeper.ZkCommandClient;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * 基于 Zookeeper 的应用管理器，Dashboard 会监控 Zookeeper apps 目录下的节点，其结构如下：
 *
 * -apps
 *  -instances
 *   -appName1
 *     -ip1
 *     -ip2
*    -appName2
 *     -ip3
 *
 * ZookeeperApplicationManager 会缓存一份应用数据在内存中，key 为当前应用名，value 为实例信息
 *
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/2/18 2:21 PM
 * @since:
 **/
@Component
public class ZookeeperApplicationManager implements ApplicationManager, InitializingBean {

    private static final Logger                  LOGGER       = LoggerFactory
                                                                  .getLogger(ZookeeperApplicationManager.class);
    @Autowired
    private ZkCommandClient                      zkCommandClient;

    private RestTemplate                         restTemplate = new RestTemplate();

    /**
     * 内存中缓存一份应用实例信息 key:appName  value
     **/
    private static Map<String, Set<Application>> applications = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 拉取应用信息
        initApplications();
        // 添加监听器
        TreeCache treeCache = new TreeCache(zkCommandClient.getCuratorClient(),
            SofaDashboardConstants.SOFA_BOOT_CLIENT_ROOT);
        addListener(treeCache);
    }

    private void initApplications() {
        try {
            String appsPath = SofaDashboardConstants.SOFA_BOOT_CLIENT_ROOT + SofaDashboardConstants.SOFA_BOOT_CLIENT_INSTANCE;
            List<String> appNames = zkCommandClient.getCuratorClient().getChildren().forPath(appsPath);
            if (appNames != null && !appNames.isEmpty()) {
                appNames.forEach((item) -> {
                    String instancePath = appsPath + SofaDashboardConstants.SEPARATOR + item;
                    try {
                        List<String> instances = zkCommandClient.getCuratorClient().getChildren().forPath(instancePath);
                        Set<Application> instanceList = new HashSet<>();
                        instances.forEach(instance -> {
                            String appInstance = appsPath + SofaDashboardConstants.SEPARATOR + item + SofaDashboardConstants.SEPARATOR + instance;
                            try {
                                byte[] bytes = zkCommandClient.getCuratorClient().getData().forPath(appInstance);
                                Application application = ObjectBytesUtil.convertFromBytes(bytes, Application.class);
                                instanceList.add(application);
                            } catch (Exception e) {
                                LOGGER.error("Error to get app instance from Zookeeper.", e);
                            }
                        });
                        applications.put(item, instanceList);
                    } catch (Exception e) {
                        LOGGER.error("Error to get instances from Zookeeper.", e);
                    }
                });
                LOGGER.info("dashboard client init success.current app count is " + applications.size());
            }
        } catch (Exception e) {
            LOGGER.error("Error to get applications from Zookeeper.", e);
        }
    }

    private void addListener(final TreeCache cache) throws Exception {
        TreeCacheListener listener = (client, event) -> {
            switch (event.getType()) {
                case NODE_ADDED:
                    doUpdateApplications(event);
                    break;
                case NODE_UPDATED:
                    doUpdateApplications(event);
                    break;
                case NODE_REMOVED:
                    doRemoveApplications(event);
                    break;
                case CONNECTION_LOST:
                    doRemoveApplications(event);
                    break;
                case CONNECTION_RECONNECTED:
                    // 服务端断线重连之后 ，重新从 zk 上去拉取应用信息
                    initApplications();
                    break;
                default:
                    break;
            }
        };
        cache.getListenable().addListener(listener);
        cache.start();
    }

    private void doUpdateApplications(TreeCacheEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboard client event type = {},data = {}", event.getType(),
                event.getData());
        }
        ChildData chileData = event.getData();
        try {
            byte[] data = event.getData().getData();
            if (data != null && chileData.getPath().contains(SofaDashboardConstants.COLON)) {
                Application application = ObjectBytesUtil.convertFromBytes(data, Application.class);
                if (application != null) {
                    String appName = application.getAppName();
                    Set<Application> apps = ZookeeperApplicationManager.applications.get(appName);
                    if (apps == null) {
                        apps = new HashSet<>();
                    }
                    apps.add(application);
                    applications.put(appName, apps);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error to update applications from zookeeper.", e);
        }
    }

    private void doRemoveApplications(TreeCacheEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboard client event type = {},data = {}", event.getType(),
                event.getData());
        }
        ChildData chileData = event.getData();
        if (chileData.getPath() == null) {
            return;
        }
        try {
            if (chileData.getPath().contains(SofaDashboardConstants.COLON)) {
                String[] paths = chileData.getPath().split(SofaDashboardConstants.SEPARATOR);
                // standard path is /apps/instance/appName/ip:port
                if (paths.length == 5) {
                    String appName = paths[3];
                    Set<Application> apps = ZookeeperApplicationManager.applications.get(appName);
                    if (apps == null) {
                        return;
                    }
                    for (Application application : apps) {
                        if (paths[4]
                            .equals((application.getHostName() + SofaDashboardConstants.COLON + application
                                .getPort()))) {
                            apps.remove(application);
                            break;
                        }
                    }
                    applications.put(appName, apps);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error to remove applications in memory", e);
        }
    }

    @Override
    public List<AppModel> applications() {
        List<AppModel> data = new ArrayList<>();
        if (applications != null && !applications.isEmpty()) {
            for (Set<Application> appList : applications.values()) {
                for (Application app : appList) {
                    AppModel appModel = new AppModel();
                    appModel.setHost(app.getHostName());
                    appModel.setPort(app.getPort());
                    appModel.setName(app.getAppName());
                    appModel.setState(app.getAppState());
                    data.add(appModel);
                }
            }
        }
        return data;
    }

    public List<Application> getApplicationByName(String appName) {
        return new ArrayList<>(applications.get(appName));
    }

    public Map<String, Set<Application>> getApplications() {
        return applications;
    }

    public String getAppState(String appName, String ip, String pluginName, String version) {
        try {
            Map<String, Set<Application>> applications = this.getApplications();
            Set<Application> instances = applications.get(appName);
            for (Application instance : instances) {
                // 兼容 "127.0.0.1" 和 真实地址，主要是本地环境下
                if (SofaDashboardConstants.LOCALHOST_IP.equals(ip)
                    || instance.getHostName().equals(ip)) {
                    Map result = restTemplate.getForObject(getBizStateUrl(instance), Map.class);
                    return parseStateFromMapJSON(result, pluginName, version);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ark biz state by restTemplate.", e);
        }
        return "";
    }

    private String parseStateFromMapJSON(Map result, String pluginName, String version) {
        if (result == null || result.isEmpty()) {
            return "";
        }
        if (result.get(SofaDashboardConstants.CODE) instanceof String) {
            String code = (String) result.get(SofaDashboardConstants.CODE);
            if (SofaDashboardConstants.SUCCESS.equals(code)) {
                Object bizInfos = result.get(SofaDashboardConstants.BIZ_INFOS);
                if (bizInfos instanceof ArrayList) {
                    List<Map> bizInfoList = (List<Map>) bizInfos;
                    for (Map item : bizInfoList) {
                        if (item.containsKey(SofaDashboardConstants.BIZ_NAME)
                            && item.containsKey(SofaDashboardConstants.BIZ_VERSION)) {
                            if (pluginName.equals(item.get(SofaDashboardConstants.BIZ_NAME))
                                && version.equals(item.get(SofaDashboardConstants.BIZ_VERSION))) {
                                return item.get(SofaDashboardConstants.BIZ_STATE) == null ? SofaDashboardConstants.EMPTY
                                    : item.get(SofaDashboardConstants.BIZ_STATE).toString();
                            }
                        }
                    }
                }
            }
        }
        return SofaDashboardConstants.EMPTY;
    }

    private String getBizStateUrl(Application instance) {
        String basePath = SpringBootVersion.getVersion().startsWith("1") ? SofaDashboardConstants.SEPARATOR
                                                                           + SofaDashboardConstants.HEALTH
            : SofaDashboardConstants.SEPARATOR + SofaDashboardConstants.ACTUATOR;
        return SofaDashboardConstants.HTTP_SCHEME + instance.getHostName()
               + SofaDashboardConstants.COLON + instance.getPort() + basePath
               + SofaDashboardConstants.SEPARATOR + SofaDashboardConstants.BIZ_STATE;
    }
}
