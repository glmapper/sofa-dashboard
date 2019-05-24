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
package com.alipay.sofa.dashboard.app.actuator;

import com.alipay.sofa.dashboard.constants.SofaDashboardConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/24 10:09 AM
 * @since:
 **/
@Component
public class RestTemplateClient {

    @Autowired
    private RestTemplate restTemplate;

    public Map doRequest(Object source, String path) {
        Map result;
        String targetRequest = buildTargetRestRequest(source, path);
        if (StringUtils.isEmpty(targetRequest)) {
            result = new HashMap();
        } else {
            result = restTemplate.getForObject(targetRequest, Map.class);
            if (result == null) {
                result = new HashMap();
            }
        }
        return result;
    }

    public String buildTargetRestRequest(Object source, String path) {
        if (source == null) {
            return null;
        }
        return SofaDashboardConstants.HTTP_SCHEME + String.valueOf(source) + path;
    }
}
