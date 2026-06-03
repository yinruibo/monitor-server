package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeDockerOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.service.ZrDockerDeployService;
import cn.hongt.monitor.server.service.ZrDockerRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerMonitorControllerTest {

    @Mock
    private ZrDockerRecordService zrDockerRecordService;
    @Mock
    private ZrDockerDeployService zrDockerDeployService;

    @InjectMocks
    private DockerMonitorController dockerMonitorController;

    @Test
    void queryNodeList_shouldReturnSuccessResult() {
        List<String> expectedNodes = Arrays.asList("node1", "node2");
        when(zrDockerDeployService.queryNodeList()).thenReturn(expectedNodes);

        Result result = dockerMonitorController.queryNodeList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedNodes, result.getData());
        verify(zrDockerDeployService).queryNodeList();
    }

    @Test
    void queryNodeList_shouldHandleEmptyList() {
        when(zrDockerDeployService.queryNodeList()).thenReturn(Collections.emptyList());

        Result result = dockerMonitorController.queryNodeList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(Collections.emptyList(), result.getData());
    }

    @Test
    void queryNodeAndServers_shouldReturnSuccessResult() {
        String nodeName = "node1";
        List<SysDockerDeployEleDO> expectedServers = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("2").dockerName("app2").build()
        );
        when(zrDockerDeployService.queryNodeAndServers(eq(nodeName))).thenReturn(expectedServers);

        Result result = dockerMonitorController.queryNodeAndServers(nodeName);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedServers, result.getData());
        verify(zrDockerDeployService).queryNodeAndServers(nodeName);
    }

    @Test
    void queryServerList_shouldReturnSuccessResult() {
        List<SysDockerDeployEleDO> expectedServers = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build()
        );
        when(zrDockerDeployService.queryServerList()).thenReturn(expectedServers);

        Result result = dockerMonitorController.queryServerList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedServers, result.getData());
        verify(zrDockerDeployService).queryServerList();
    }

    @Test
    void queryDockerNameList_shouldReturnSuccessResult() {
        String nodeName = "node1";
        String ip = "192.168.1.1";
        List expectedList = Arrays.asList("app1", "app2");
        when(zrDockerDeployService.queryDockerNameList(eq(nodeName), eq(ip))).thenReturn(expectedList);

        Result result = dockerMonitorController.queryDockerNameList(nodeName, ip);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedList, result.getData());
        verify(zrDockerDeployService).queryDockerNameList(nodeName, ip);
    }

    @Test
    void queryNodeDocker_shouldDelegateToService() {
        String nodeName = "node1";
        String ip = "192.168.1.1";
        List<NodeDockerOutput> expectedOutput = Arrays.asList(new NodeDockerOutput());
        Result<List<NodeDockerOutput>> expectedResult = ResultUtil.success(expectedOutput);
        when(zrDockerRecordService.queryNodeDocker(eq(nodeName), eq(ip))).thenReturn(expectedResult);

        Result<List<NodeDockerOutput>> result = dockerMonitorController.queryNodeDocker(nodeName, ip);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrDockerRecordService).queryNodeDocker(nodeName, ip);
    }

    @Test
    void queryDockerRecord_shouldDelegateToService() {
        HardWareMonitorInput input = new HardWareMonitorInput();
        List<HardresultOutput> expectedOutput = Arrays.asList(new HardresultOutput());
        Result<List<HardresultOutput>> expectedResult = ResultUtil.success(expectedOutput);
        when(zrDockerRecordService.queryDockerRecord(any(HardWareMonitorInput.class))).thenReturn(expectedResult);

        Result<List<HardresultOutput>> result = dockerMonitorController.queryDockerRecord(input);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrDockerRecordService).queryDockerRecord(input);
    }
}
