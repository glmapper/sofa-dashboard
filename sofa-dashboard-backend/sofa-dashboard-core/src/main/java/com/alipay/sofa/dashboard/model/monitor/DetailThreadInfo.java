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

/**
 * @author: guolei.sgl (guolei.sgl@antfin.com) 2019/5/9 5:07 PM
 * @since:
 **/
public class DetailThreadInfo {

    private DetailsItem live;
    private DetailsItem daemon;
    private DetailsItem peak;

    public DetailsItem getLive() {
        return live;
    }

    public void setLive(DetailsItem live) {
        this.live = live;
    }

    public DetailsItem getDaemon() {
        return daemon;
    }

    public void setDaemon(DetailsItem daemon) {
        this.daemon = daemon;
    }

    public DetailsItem getPeak() {
        return peak;
    }

    public void setPeak(DetailsItem peak) {
        this.peak = peak;
    }

}
