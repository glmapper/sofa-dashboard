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
package com.alipay.sofa.dashboard.model.monitor;

import java.util.List;
import java.util.Map;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/7 9:22 PM
 * @since:
 **/
public class EnvironmentInfo {

    private List<String>                   activeProfiles;

    private List<PropertySourceDescriptor> propertySources;

    public List<String> getActiveProfiles() {
        return this.activeProfiles;
    }

    public List<PropertySourceDescriptor> getPropertySources() {
        return this.propertySources;
    }

    public void setActiveProfiles(List<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public void setPropertySources(List<PropertySourceDescriptor> propertySources) {
        this.propertySources = propertySources;
    }

    public static final class PropertySourceDescriptor {

        private String    name;

        private List<Map> properties;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Map> getProperties() {
            return properties;
        }

        public void setProperties(List<Map> properties) {
            this.properties = properties;
        }
    }
}