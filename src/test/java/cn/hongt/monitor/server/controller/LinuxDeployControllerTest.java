package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.LinuxDeployInput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.service.ZrLinuxDeployService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinuxDeployControllerTest {

    @Mock
    private ZrLinuxDeployService linuxDeployService;

    @InjectMocks
    private LinuxDeployController linuxDeployController;

    @Test
    void queryDeployList_shouldReturnSuccessResult() {
        LinuxDeployInput input = new LinuxDeployInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("records", Arrays.asList(
                SysLinuxDeployDO.builder().id("1").ipName("server1").build(),
                SysLinuxDeployDO.builder().id("2").ipName("server2").build()
        ));
        expectedData.put("total", 2);
        when(linuxDeployService.queryDeployList(any(LinuxDeployInput.class))).thenReturn(expectedData);

        Result result = linuxDeployController.queryDeployList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
        verify(linuxDeployService).queryDeployList(input);
    }

    @Test
    void queryDeployList_shouldHandleEmptyMap() {
        LinuxDeployInput input = new LinuxDeployInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("records", Collections.emptyList());
        expectedData.put("total", 0);
        when(linuxDeployService.queryDeployList(any(LinuxDeployInput.class))).thenReturn(expectedData);

        Result result = linuxDeployController.queryDeployList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
    }

    @Test
    void updateDeployList_shouldCallServiceAndReturnSuccess() {
        List<SysLinuxDeployDO> inputList = Arrays.asList(
                SysLinuxDeployDO.builder().id("1").ipName("server1").build(),
                SysLinuxDeployDO.builder().id("2").ipName("server2").build()
        );

        Result result = linuxDeployController.updateDeployList(inputList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(linuxDeployService).updateDeployList(inputList);
    }

    @Test
    void deleteDeployList_shouldCallServiceAndReturnSuccess() {
        List<String> idList = Arrays.asList("1", "2", "3");

        Result result = linuxDeployController.deleteDeployList(idList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(linuxDeployService).deleteDeployList(idList);
    }

    @Test
    void deleteDeployList_shouldHandleEmptyIdList() {
        List<String> idList = Collections.emptyList();

        Result result = linuxDeployController.deleteDeployList(idList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(linuxDeployService).deleteDeployList(idList);
    }
}
