package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.InsertSysWarnDeployInput;
import cn.hongt.monitor.server.dto.input.SysWarnDeployInput;
import cn.hongt.monitor.server.dto.output.SysWarnDeployOutput;
import cn.hongt.monitor.server.service.SysWarnDeployService;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author yrb
 * @date Created in 2022/5/9 14:53
 * @description 告警管理接口
 */
@RestController
@Slf4j
@Api(tags = "告警配置服务")
@RequestMapping("/sys")
public class SysWarnDeployController {

    @Autowired
    private SysWarnDeployService sysWarnDeployService;

    @PostMapping("/insertWarnDep")
    @ApiOperation(value = "新增告警配置", httpMethod = "POST")
    public Result<String> insertWarnDep(@Validated @RequestBody InsertSysWarnDeployInput insertSysWarnDeployInput) {
        return sysWarnDeployService.insertWarnDep(insertSysWarnDeployInput);
    }

    @PostMapping("/queryWarnDepList")
    @ApiOperation(value = "查询告警配置列表", httpMethod = "POST")
    public Result<SysWarnDeployOutput> queryWarnDepList(@Validated @RequestBody SysWarnDeployInput sysWarnDepInput) {
        return ResultUtil.success(sysWarnDeployService.queryWarnDepList(sysWarnDepInput));
    }

    @PostMapping("/queryWarnDep")
    @ApiOperation(value = "查询告警配置", httpMethod = "POST")
    public Result<SysWarnDeployOutput> queryWarnDep(@Validated @RequestBody SysWarnDeployInput sysWarnDepInput) {
        return ResultUtil.success(sysWarnDeployService.queryWarnDep(sysWarnDepInput));
    }

    @PostMapping("/updateWarnDep")
    @ApiOperation(value = "修改告警配置", httpMethod = "POST")
    public Result<String> updateWarnDep(@Validated @RequestBody SysWarnDeployInput sysWarnDepInput) {
        return sysWarnDeployService.updateWarnDep(sysWarnDepInput);
    }

    @PostMapping("/deleteWarnDep")
    @ApiOperation(value = "删除告警配置", httpMethod = "POST")
    public Result<String> deleteWarnDep(@Validated @RequestBody IdListInput input) {
        return sysWarnDeployService.deleteWarnDep(input);
    }

    @PostMapping("/warnDepStart")
    @ApiOperation(value = "告警配置类型启动", httpMethod = "POST")
    public Result<String> warnDepStart(@Validated @RequestBody SysWarnDeployInput input) {
        return sysWarnDeployService.warnDepStart(input);
    }

}
