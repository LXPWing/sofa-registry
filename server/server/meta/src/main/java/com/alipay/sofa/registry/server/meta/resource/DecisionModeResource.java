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
package com.alipay.sofa.registry.server.meta.resource;

import com.alipay.sofa.registry.core.model.Result;
import com.alipay.sofa.registry.server.meta.bootstrap.config.MetaServerConfig;
import com.alipay.sofa.registry.server.meta.bootstrap.config.MetaServerConfig.DecisionMode;
import com.alipay.sofa.registry.server.meta.bootstrap.config.MetaServerConfigBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author shangyu.wh
 * @version $Id: DecisionModeResource.java, v 0.1 2018-02-01 16:50 shangyu.wh Exp $
 */
@Path("decisionMode")
public class DecisionModeResource {

    @Autowired
    private MetaServerConfig metaServerConfig;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Result changeDecisionMode(DecisionMode decisionMode) {
        ((MetaServerConfigBean) metaServerConfig).setDecisionMode(decisionMode);
        Result result = new Result();
        result.setSuccess(true);
        return result;
    }

}
