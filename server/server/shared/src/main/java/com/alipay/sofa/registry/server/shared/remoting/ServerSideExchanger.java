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
package com.alipay.sofa.registry.server.shared.remoting;

import com.alipay.sofa.registry.common.model.store.URL;
import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.remoting.Channel;
import com.alipay.sofa.registry.remoting.Client;
import com.alipay.sofa.registry.remoting.Server;
import com.alipay.sofa.registry.remoting.exchange.Exchange;
import com.alipay.sofa.registry.remoting.exchange.NodeExchanger;
import com.alipay.sofa.registry.remoting.exchange.RequestException;
import com.alipay.sofa.registry.remoting.exchange.message.Request;
import com.alipay.sofa.registry.remoting.exchange.message.Response;
import com.alipay.sofa.registry.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Optional;

/**
 *
 * @author yuzhi.lyz
 * @version v 0.1 2020-12-18 11:40 yuzhi.lyz Exp $
 */
public abstract class ServerSideExchanger implements NodeExchanger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSideExchanger.class);

    @Autowired
    protected Exchange boltExchange;

    @Override
    public Response request(Request request) throws RequestException {
        final URL url = request.getRequestUrl();
        if (url == null) {
            throw new RequestException("null url", request);
        }
        return request(url, request);
    }

    public Response request(URL url, Request request) throws RequestException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("serverPort={} to client, url:{}, request body:{} ", getServerPort(), request.getRequestUrl(),
                    request.getRequestBody());
        }
        final Server server = boltExchange.getServer(getServerPort());
        if (server == null) {
            throw new RequestException("no server for " + getServerPort(), request);
        }
        final int timeout = request.getTimeout() != null ? request.getTimeout() : getRpcTimeout();
        Channel channel = null;
        if (url == null) {
            channel = choseChannel(server);
        } else {
            channel = server.getChannel(url);
        }

        if (channel == null || !channel.isConnected()) {
            throw new RequestException(getServerPort() + ", get channel error! channel with url:" + url
                    + " can not be null or disconnected!", request);
        }
        try {
            if (request.getCallBackHandler() != null) {
                server.sendCallback(channel, request.getRequestBody(), request.getCallBackHandler(), timeout);
                return () -> Response.ResultStatus.SUCCESSFUL;
            } else {
                final Object result = server.sendSync(channel, request.getRequestBody(), timeout);
                return () -> result;
            }
        } catch (Throwable e) {
            throw new RequestException(getServerPort() + ", Exchanger request error! Request url:" + url, request, e);
        }
    }

    private Channel choseChannel(Server server) {
        Collection<Channel> channels = server.getChannels();
        Optional<Channel> channelOptional = CollectionUtils.getRandom(channels);
        if (channelOptional.isPresent()) {
            Channel channel = channelOptional.get();
            if (channel.isConnected()) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public Client connectServer() {
        throw new UnsupportedOperationException();
    }

    public abstract int getRpcTimeout();

    public abstract int getServerPort();
}