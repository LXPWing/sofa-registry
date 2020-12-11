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
package com.alipay.sofa.registry.server.session.remoting.handler;

import com.alipay.sofa.registry.common.model.ConnectId;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.remoting.RemotingException;
import com.alipay.sofa.registry.remoting.exchange.Exchange;
import com.alipay.sofa.registry.server.session.bootstrap.SessionServerConfig;
import com.alipay.sofa.registry.server.session.registry.Registry;
import com.alipay.sofa.registry.server.session.scheduler.ExecutorManager;
import com.alipay.sofa.registry.server.session.store.DataStore;
import com.alipay.sofa.registry.server.session.store.Interests;
import com.alipay.sofa.registry.server.session.store.Watchers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author shangyu.wh
 * @version $Id: ServerConnectionLisener.java, v 0.1 2017-11-30 15:04 shangyu.wh Exp $
 */
public class ClientNodeConnectionHandler extends AbstractServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("SESSION-CONNECT");

    @Autowired
    private Registry            sessionRegistry;

    @Autowired
    private DataStore           sessionDataStore;

    @Autowired
    private Interests           sessionInterests;

    @Autowired
    private Watchers            sessionWatchers;

    @Autowired
    private ExecutorManager     executorManager;

    @Autowired
    private SessionServerConfig sessionServerConfig;

    @Autowired
    private Exchange            boltExchange;

    @Override
    public HandlerType getType() {
        return HandlerType.LISENTER;
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        super.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        super.disconnected(channel);
        fireCancelClient(channel);
    }

    private void fireCancelClient(Channel channel) {
        //avoid block connect ConnectionEventExecutor thread pool
        executorManager.getConnectClientExecutor().execute(() -> {
            ConnectId connectId = ConnectId.of(channel.getRemoteAddress(), channel.getLocalAddress());
            if (checkCache(connectId)) {
                sessionRegistry.cancel(Collections.singletonList(connectId));
            }
        });
    }

    private boolean checkCache(ConnectId connectId) {
        boolean checkSub = checkSub(connectId);
        boolean checkPub = checkPub(connectId);
        boolean checkWatcher = checkWatcher(connectId);
        LOGGER.info("Client off checkCache connectId:{} result pub:{},sub:{},wat:{}", connectId,
            checkPub, checkSub, checkWatcher);
        return checkPub || checkSub || checkWatcher;
    }

    private boolean checkPub(ConnectId connectId) {
        Map pubMap = sessionDataStore.queryByConnectId(connectId);
        return pubMap != null && !pubMap.isEmpty();
    }

    private boolean checkSub(ConnectId connectId) {
        Map subMap = sessionInterests.queryByConnectId(connectId);
        return subMap != null && !subMap.isEmpty();
    }

    private boolean checkWatcher(ConnectId connectId) {
        Map subMap = sessionWatchers.queryByConnectId(connectId);
        return subMap != null && !subMap.isEmpty();
    }

}