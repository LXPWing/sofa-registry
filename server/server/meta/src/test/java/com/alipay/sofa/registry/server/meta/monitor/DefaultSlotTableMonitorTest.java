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
package com.alipay.sofa.registry.server.meta.monitor;

import com.alipay.sofa.registry.common.model.metaserver.inter.heartbeat.HeartbeatRequest;
import com.alipay.sofa.registry.common.model.metaserver.nodes.DataNode;
import com.alipay.sofa.registry.common.model.slot.*;
import com.alipay.sofa.registry.server.meta.AbstractTest;
import com.alipay.sofa.registry.server.meta.bootstrap.config.NodeConfig;
import com.alipay.sofa.registry.server.meta.monitor.impl.DefaultSlotTableMonitor;
import com.alipay.sofa.registry.server.meta.slot.manager.LocalSlotManager;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class DefaultSlotTableMonitorTest extends AbstractTest {

    private DefaultSlotTableMonitor monitor = new DefaultSlotTableMonitor();

    private LocalSlotManager        slotManager;

    private List<DataNode>          dataNodes;

    @Before
    public void beforeDefaultSlotTableMonitorTest() throws Exception {
        NodeConfig nodeConfig = mock(NodeConfig.class);
        when(nodeConfig.getLocalDataCenter()).thenReturn(getDc());
        slotManager = new LocalSlotManager(nodeConfig);
        dataNodes = Lists.newArrayList(new DataNode(randomURL(randomIp()), getDc()), new DataNode(
            randomURL(randomIp()), getDc()), new DataNode(randomURL(randomIp()), getDc()));
        slotManager.refresh(new SlotTableGenerator(dataNodes).createSlotTable());
        slotManager = spy(slotManager);
        monitor.setSlotManager(slotManager);
        slotManager.postConstruct();
        monitor.postConstruct();
    }

    @After
    public void afterDefaultSlotTableMonitorTest() throws Exception {
        monitor.preDestroy();
    }

    @Test
    public void testRecordSlotTable() {
        monitor.recordSlotTable();
        verify(slotManager, atLeast(1)).getSlotTable();
    }

    @Test
    public void testUpdate() {
        slotManager.refresh(randomSlotTable());
        verify(slotManager, atLeast(1)).getSlotTable();
    }

    @Test
    public void testIsSlotTableStable() {
        monitor.update(slotManager, slotManager.getSlotTable());
        Assert.assertFalse(monitor.isSlotTableStable());
        SlotTable slotTable = slotManager.getSlotTable();
        for(DataNode dataNode : dataNodes) {
            List<SlotStatus> slotStatuses = Lists.newArrayList();
            DataNodeSlot dataNodeSlot = slotTable.transfer(dataNode.getIp(), false).get(0);
            dataNodeSlot.getLeaders().forEach(slotId -> {
                slotStatuses.add(new SlotStatus(slotId, slotTable.getSlot(slotId).getLeaderEpoch(), SlotStatus.LeaderStatus.HEALTHY));
            });
            monitor.onHeartbeat(new HeartbeatRequest<DataNode>(dataNode, slotManager.getSlotTable().getEpoch(), getDc(),
                    System.currentTimeMillis(),
                    new SlotConfig.SlotBasicInfo(SlotConfig.SLOT_NUM, SlotConfig.SLOT_REPLICAS, SlotConfig.FUNC),
                    slotStatuses));
        }
        Assert.assertTrue(monitor.isSlotTableStable());

        final boolean[] unstable = {false};
        for(DataNode dataNode : dataNodes) {
            List<SlotStatus> slotStatuses = Lists.newArrayList();
            DataNodeSlot dataNodeSlot = slotTable.transfer(dataNode.getIp(), false).get(0);
            dataNodeSlot.getLeaders().forEach(slotId -> {
                if (!unstable[0]) {
                    slotStatuses.add(new SlotStatus(slotId, slotTable.getSlot(slotId).getLeaderEpoch(), SlotStatus.LeaderStatus.UNHEALTHY));
                    unstable[0] = true;
                } else {
                    slotStatuses.add(new SlotStatus(slotId, slotTable.getSlot(slotId).getLeaderEpoch(), SlotStatus.LeaderStatus.HEALTHY));
                }
            });
            monitor.onHeartbeat(new HeartbeatRequest<DataNode>(dataNode, slotManager.getSlotTable().getEpoch(), getDc(),
                    System.currentTimeMillis(),
                    new SlotConfig.SlotBasicInfo(SlotConfig.SLOT_NUM, SlotConfig.SLOT_REPLICAS, SlotConfig.FUNC),
                    slotStatuses));
        }
        Assert.assertFalse(monitor.isSlotTableStable());
    }

    @Test
    public void testOnHeartbeatWithPrevEpoch() {
        monitor.update(slotManager, slotManager.getSlotTable());
        Assert.assertFalse(monitor.isSlotTableStable());
        SlotTable slotTable = slotManager.getSlotTable();
        for(DataNode dataNode : dataNodes) {
            List<SlotStatus> slotStatuses = Lists.newArrayList();
            DataNodeSlot dataNodeSlot = slotTable.transfer(dataNode.getIp(), false).get(0);
            dataNodeSlot.getLeaders().forEach(slotId -> {
                slotStatuses.add(new SlotStatus(slotId, slotTable.getSlot(slotId).getLeaderEpoch(), SlotStatus.LeaderStatus.HEALTHY));
            });
            monitor.onHeartbeat(new HeartbeatRequest<DataNode>(dataNode, slotTable.getEpoch() - 1, getDc(),
                    System.currentTimeMillis(),
                    new SlotConfig.SlotBasicInfo(SlotConfig.SLOT_NUM, SlotConfig.SLOT_REPLICAS, SlotConfig.FUNC),
                    slotStatuses));
        }
        Assert.assertFalse(monitor.isSlotTableStable());
    }

    @Test
    public void testUpdateSlotTableThenIsStableShouldBeFalse() {
        monitor.update(slotManager, slotManager.getSlotTable());
        Assert.assertFalse(monitor.isSlotTableStable());
        SlotTable slotTable = slotManager.getSlotTable();
        for (DataNode dataNode : dataNodes) {
            List<SlotStatus> slotStatuses = Lists.newArrayList();
            DataNodeSlot dataNodeSlot = slotTable.transfer(dataNode.getIp(), false).get(0);
            dataNodeSlot.getLeaders().forEach(slotId -> {
                slotStatuses.add(new SlotStatus(slotId, slotTable.getSlot(slotId).getLeaderEpoch(), SlotStatus.LeaderStatus.HEALTHY));
            });
            monitor.onHeartbeat(new HeartbeatRequest<DataNode>(dataNode, slotManager.getSlotTable().getEpoch(), getDc(),
                    System.currentTimeMillis(),
                    new SlotConfig.SlotBasicInfo(SlotConfig.SLOT_NUM, SlotConfig.SLOT_REPLICAS, SlotConfig.FUNC),
                    slotStatuses));
        }
        Assert.assertTrue(monitor.isSlotTableStable());

        slotManager.refresh(new SlotTableGenerator(dataNodes).createSlotTable());
        monitor.update(slotManager, slotManager.getSlotTable());
        Assert.assertFalse(monitor.isSlotTableStable());
    }
}