package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeMonitorOutput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.service.ZrLinuxDeployService;
import cn.hongt.monitor.server.service.ZrLinuxRecordService;
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
class LinuxMonitorControllerTest {

    @Mock
    private ZrLinuxDeployService linuxDeployService;
    @Mock
    private ZrLinuxRecordService linuxRecordService;

    @InjectMocks
    private LinuxMonitorController linuxMonitorController;

    @Test
    void queryNodeList_shouldReturnSuccessResult() {
        List<String> expectedNodes = Arrays.asList("node1", "node2");
        when(linuxDeployService.queryNodeList()).thenReturn(expectedNodes);

        Result result = linuxMonitorController.queryNodeList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedNodes, result.getData());
        verify(linuxDeployService).queryNodeList();
    }

    @Test
    void queryNodeList_shouldHandleEmptyList() {
        when(linuxDeployService.queryNodeList()).thenReturn(Collections.emptyList());

        Result result = linuxMonitorController.queryNodeList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(Collections.emptyList(), result.getData());
    }

    @Test
    void queryNodeAndServers_shouldReturnSuccessResult() {
        String nodeName = "node1";
        List<SysLinuxDeployDO> expectedServers = Arrays.asList(
                SysLinuxDeployDO.builder().id("1").ipName("server1").build(),
                SysLinuxDeployDO.builder().id("2").ipName("server2").build()
        );
        when(linuxDeployService.queryNodeAndServers(eq(nodeName))).thenReturn(expectedServers);

        Result result = linuxMonitorController.queryNodeAndServers(nodeName);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedServers, result.getData());
        verify(linuxDeployService).queryNodeAndServers(nodeName);
    }

    @Test
    void queryServerList_shouldReturnSuccessResult() {
        List<SysLinuxDeployDO> expectedServers = Arrays.asList(
                SysLinuxDeployDO.builder().id("1").ipName("server1").build()
        );
        when(linuxDeployService.queryServerList()).thenReturn(expectedServers);

        Result result = linuxMonitorController.queryServerList();

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedServers, result.getData());
        verify(linuxDeployService).queryServerList();
    }

    @Test
    void queryNodeMonitor_shouldDelegateToService() {
        String nodeName = "node1";
        String ip = "192.168.1.1";
        NodeMonitorOutput expectedOutput = new NodeMonitorOutput();
        when(linuxRecordService.queryNodeMonitor(eq(nodeName), eq(ip))).thenReturn(expectedOutput);

        Result result = linuxMonitorController.queryNodeMonitor(nodeName, ip);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedOutput, result.getData());
        verify(linuxRecordService).queryNodeMonitor(nodeName, ip);
    }

    @Test
    void queryServerRecord_shouldDelegateToService() {
        HardWareMonitorInput input = new HardWareMonitorInput();
        List<HardresultOutput> expectedOutput = Arrays.asList(new HardresultOutput());
        when(linuxRecordService.queryServerRecord(any(HardWareMonitorInput.class))).thenReturn(expectedOutput);

        Result result = linuxMonitorController.queryServerRecord(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedOutput, result.getData());
        verify(linuxRecordService).queryServerRecord(input);
    }
}
