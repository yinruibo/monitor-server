package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.InsertSysWarnDeployInput;
import cn.hongt.monitor.server.dto.input.SysWarnDeployInput;
import cn.hongt.monitor.server.dto.output.SysWarnDeployOutput;
import cn.hongt.monitor.server.service.SysWarnDeployService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysWarnDeployControllerTest {

    @Mock
    private SysWarnDeployService sysWarnDeployService;

    @InjectMocks
    private SysWarnDeployController sysWarnDeployController;

    @Test
    void insertWarnDep_shouldDelegateToService() {
        InsertSysWarnDeployInput input = new InsertSysWarnDeployInput();
        Result<String> expectedResult = ResultUtil.success("新增成功");
        when(sysWarnDeployService.insertWarnDep(any(InsertSysWarnDeployInput.class))).thenReturn(expectedResult);

        Result<String> result = sysWarnDeployController.insertWarnDep(input);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(sysWarnDeployService).insertWarnDep(input);
    }

    @Test
    void queryWarnDepList_shouldReturnSuccessResult() {
        SysWarnDeployInput input = new SysWarnDeployInput();
        SysWarnDeployOutput expectedOutput = SysWarnDeployOutput.builder().total(10).build();
        when(sysWarnDeployService.queryWarnDepList(any(SysWarnDeployInput.class))).thenReturn(expectedOutput);

        Result<SysWarnDeployOutput> result = sysWarnDeployController.queryWarnDepList(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedOutput, result.getData());
        verify(sysWarnDeployService).queryWarnDepList(input);
    }

    @Test
    void queryWarnDep_shouldReturnSuccessResult() {
        SysWarnDeployInput input = new SysWarnDeployInput();
        SysWarnDeployOutput expectedOutput = SysWarnDeployOutput.builder().total(5).build();
        when(sysWarnDeployService.queryWarnDep(any(SysWarnDeployInput.class))).thenReturn(expectedOutput);

        Result<SysWarnDeployOutput> result = sysWarnDeployController.queryWarnDep(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertEquals(expectedOutput, result.getData());
        verify(sysWarnDeployService).queryWarnDep(input);
    }

    @Test
    void updateWarnDep_shouldDelegateToService() {
        SysWarnDeployInput input = new SysWarnDeployInput();
        Result<String> expectedResult = ResultUtil.success("修改成功");
        when(sysWarnDeployService.updateWarnDep(any(SysWarnDeployInput.class))).thenReturn(expectedResult);

        Result<String> result = sysWarnDeployController.updateWarnDep(input);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(sysWarnDeployService).updateWarnDep(input);
    }

    @Test
    void deleteWarnDep_shouldDelegateToService() {
        SysWarnDeployInput input = new SysWarnDeployInput();
        Result<String> expectedResult = ResultUtil.success("删除成功");
        when(sysWarnDeployService.deleteWarnDep(any(SysWarnDeployInput.class))).thenReturn(expectedResult);

        Result<String> result = sysWarnDeployController.deleteWarnDep(input);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(sysWarnDeployService).deleteWarnDep(input);
    }

    @Test
    void warnDepStart_shouldDelegateToService() {
        String id = "warn-1";
        Integer code = 0;
        Result<String> expectedResult = ResultUtil.success("启动成功");
        when(sysWarnDeployService.warnDepStart(eq(id), eq(code))).thenReturn(expectedResult);

        Result<String> result = sysWarnDeployController.warnDepStart(id, code);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(sysWarnDeployService).warnDepStart(id, code);
    }

    @Test
    void warnDepStart_shouldHandleDisableCode() {
        String id = "warn-1";
        Integer code = 1;
        Result<String> expectedResult = ResultUtil.success("禁用成功");
        when(sysWarnDeployService.warnDepStart(eq(id), eq(code))).thenReturn(expectedResult);

        Result<String> result = sysWarnDeployController.warnDepStart(id, code);

        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(sysWarnDeployService).warnDepStart(id, code);
    }
}
