package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxDeployMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeployBatchUpdateServiceImplTest {

    @Mock
    private ZrDockerDeployMapper zrDockerDeployMapper;
    @Mock
    private ZrLinuxDeployMapper zrLinuxDeployMapper;

    @Test
    void updateDockerDeployList_shouldUseBatchUpdateWithSanitizedEntities() {
        ZrDockerDeployServiceImpl service = spy(new ZrDockerDeployServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", zrDockerDeployMapper);
        doReturn(true).when(service).updateBatchById(anyCollection(), eq(200));

        List<SysDockerDeployEleDO> inputList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("docker-1").ip("10.0.0.1").sort(1).build(),
                SysDockerDeployEleDO.builder().id("docker-2").dockerName(" ").taskName("").build(),
                SysDockerDeployEleDO.builder().dockerName("missing-id").build()
        );

        service.updateDeployList(inputList);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SysDockerDeployEleDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).updateBatchById(captor.capture(), eq(200));        List<SysDockerDeployEleDO> updateList = (List<SysDockerDeployEleDO>) captor.getValue();
        assertEquals(1, updateList.size());
        assertEquals("docker-1", updateList.get(0).getId());
        assertEquals("10.0.0.1", updateList.get(0).getIp());
        assertEquals(1, updateList.get(0).getSort());
        assertNull(updateList.get(0).getDockerName());
    }

    @Test
    void updateLinuxDeployList_shouldUseBatchUpdateWithSanitizedEntities() {
        ZrLinuxDeployServiceImpl service = spy(new ZrLinuxDeployServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", zrLinuxDeployMapper);
        doReturn(true).when(service).updateBatchById(anyCollection(), eq(200));

        List<SysLinuxDeployDO> inputList = Arrays.asList(
                SysLinuxDeployDO.builder().id("linux-1").ip("10.0.0.2").ipName("node-a").sort(2).build(),
                SysLinuxDeployDO.builder().id("linux-2").nodeName(" ").type("").build(),
                SysLinuxDeployDO.builder().ip("missing-id").build()
        );

        service.updateDeployList(inputList);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SysLinuxDeployDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).updateBatchById(captor.capture(), eq(200));        List<SysLinuxDeployDO> updateList = (List<SysLinuxDeployDO>) captor.getValue();
        assertEquals(1, updateList.size());
        assertEquals("linux-1", updateList.get(0).getId());
        assertEquals("10.0.0.2", updateList.get(0).getIp());
        assertEquals("node-a", updateList.get(0).getIpName());
        assertEquals(2, updateList.get(0).getSort());
        assertNull(updateList.get(0).getNodeName());
    }
}

