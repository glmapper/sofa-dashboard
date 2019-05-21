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
package com.alipay.sofa.dashboard;

import com.alipay.sofa.dashboard.utils.DashboardUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/13 7:44 PM
 * @since:
 **/
public class DashboardUtilTest {

    @Test
    public void testEncode() {
        String encode = DashboardUtil.simpleEncode("127.0.0.1", 8080);
        String encodeLocal = DashboardUtil.simpleEncode("localhost", 8080);
        Assert.assertTrue(encodeLocal.equals(encode));
        String result = DashboardUtil.simpleDecode(encode);
        Assert.assertTrue(result.equals("127.0.0.1:8080"));
    }
}