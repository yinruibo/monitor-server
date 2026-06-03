package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.ZrWarnRecordListInput;
import cn.hongt.monitor.server.dto.output.WarnFaultOutput;
import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZrWarnRecordEleControllerTest {

    @Mock
    private ZrWarnRecordEleService zrWarnRecordEleService;

    @InjectMocks
    private ZrWarnRecordEleController zrWarnRecordEleController;

    @Test
    void queryWarnDepList_shouldReturnSuccessResult() {
        ZrWarnRecordListInput input = new ZrWarnRecordListInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("records", Arrays.asList("record1", "record2"));
        expectedData.put("total", 2);
        when(zrWarnRecordEleService.queryWarnDepList(any(ZrWarnRecordListInput.class))).thenReturn(expectedData);

        Result result = zrWarnRecordEleController.queryWarnDepList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
        verify(zrWarnRecordEleService).queryWarnDepList(input);
    }

    @Test
    void deleteWarnRecord_shouldCallServiceAndReturnSuccess() {
        List<String> ids = Arrays.asList("1", "2", "3");

        Result result = zrWarnRecordEleController.deleteWarnRecord(ids);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(zrWarnRecordEleService).deleteWarnRecord(ids);
    }

    @Test
    void deleteWarnRecord_shouldHandleEmptyIds() {
        List<String> ids = Collections.emptyList();

        Result result = zrWarnRecordEleController.deleteWarnRecord(ids);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        verify(zrWarnRecordEleService).deleteWarnRecord(ids);
    }

    @Test
    void queryWarnSignList_shouldReturnSuccessResult() {
        ZrWarnRecordListInput input = new ZrWarnRecordListInput();
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("sign1", 10);
        expectedData.put("sign2", 20);
        when(zrWarnRecordEleService.queryWarnSignList(any(ZrWarnRecordListInput.class))).thenReturn(expectedData);

        Result result = zrWarnRecordEleController.queryWarnSignList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedData, result.getData());
        verify(zrWarnRecordEleService).queryWarnSignList(input);
    }

    @Test
    void queryFaultList_shouldDelegateToService() {
        ZrWarnRecordListInput input = new ZrWarnRecordListInput();
        List<WarnFaultOutput> expectedOutput = Arrays.asList(new WarnFaultOutput());
        Result<List<WarnFaultOutput>> expectedResult = ResultUtil.success(expectedOutput);
        when(zrWarnRecordEleService.queryFaultList(any(ZrWarnRecordListInput.class))).thenReturn(expectedResult);

        Result<List<WarnFaultOutput>> result = zrWarnRecordEleController.queryFaultList(input);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrWarnRecordEleService).queryFaultList(input);
    }

    @Test
    void queryExport_shouldDelegateToService() {
        List<String> idList = Arrays.asList("1", "2");
        Result<String> expectedResult = ResultUtil.success("export-url");
        when(zrWarnRecordEleService.queryExport(eq(idList))).thenReturn(expectedResult);

        Result<String> result = zrWarnRecordEleController.queryExport(idList);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrWarnRecordEleService).queryExport(idList);
    }

    @Test
    void queryTimingUpdate_shouldDelegateToService() {
        Result<String> expectedResult = ResultUtil.success("更新成功");
        when(zrWarnRecordEleService.queryTimingUpdate()).thenReturn(expectedResult);

        Result<String> result = zrWarnRecordEleController.queryTimingUpdate();

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrWarnRecordEleService).queryTimingUpdate();
    }

    @Test
    void queryWarnNumber_shouldDelegateToService() {
        String ip = "192.168.1.1";
        Result<Integer> expectedResult = ResultUtil.success(5);
        when(zrWarnRecordEleService.queryWarnNumber(eq(ip))).thenReturn(expectedResult);

        Result<Integer> result = zrWarnRecordEleController.queryWarnNumber(ip);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(zrWarnRecordEleService).queryWarnNumber(ip);
    }
}
