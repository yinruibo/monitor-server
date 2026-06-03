package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.service.ZrDockerDeployService;
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
class DockerDeployControllerTest {

    @Mock
    private ZrDockerDeployService dockerDeployService;

    @InjectMocks
    private DockerDeployController dockerDeployController;

    @Test
    void queryDeployList_shouldReturnSuccessResult() {
        DockerDeployInput input = new DockerDeployInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("records", Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("2").dockerName("app2").build()
        ));
        expectedData.put("total", 2);
        when(dockerDeployService.queryDeployList(any(DockerDeployInput.class))).thenReturn(expectedData);

        Result result = dockerDeployController.queryDeployList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
        verify(dockerDeployService).queryDeployList(input);
    }

    @Test
    void queryDeployList_shouldHandleEmptyMap() {
        DockerDeployInput input = new DockerDeployInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("records", Collections.emptyList());
        expectedData.put("total", 0);
        when(dockerDeployService.queryDeployList(any(DockerDeployInput.class))).thenReturn(expectedData);

        Result result = dockerDeployController.queryDeployList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
    }

    @Test
    void updateDeployList_shouldCallServiceAndReturnSuccess() {
        List<SysDockerDeployEleDO> inputList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("2").dockerName("app2").build()
        );

        Result result = dockerDeployController.updateDeployList(inputList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(dockerDeployService).updateDeployList(inputList);
    }

    @Test
    void deleteDeployList_shouldCallServiceAndReturnSuccess() {
        List<String> idList = Arrays.asList("1", "2", "3");

        Result result = dockerDeployController.deleteDeployList(idList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(dockerDeployService).deleteDeployList(idList);
    }

    @Test
    void deleteDeployList_shouldHandleEmptyIdList() {
        List<String> idList = Collections.emptyList();

        Result result = dockerDeployController.deleteDeployList(idList);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(dockerDeployService).deleteDeployList(idList);
    }
}
