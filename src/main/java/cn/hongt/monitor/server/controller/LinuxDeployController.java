package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.LinuxDeployInput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.service.ZrLinuxDeployService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Linux监控配置模块
 * @author yrb
 * @date 2024-08-15
 */
@Api(tags = "Linux监控配置模块")
@RestController
@RequestMapping("/linuxDeploy")
public class LinuxDeployController {

    @Autowired
    private ZrLinuxDeployService linuxDeployService;
    // 配置信息列表
    @PostMapping("/queryDeployList")
    @ApiOperation(value = "配置信息列表", httpMethod = "POST")
    public Result queryDeployList(@RequestBody LinuxDeployInput input) {
        return ResultUtil.success(linuxDeployService.queryDeployList(input));
    }

    // 批量修改配置信息
    @PostMapping("/updateDeployList")
    @ApiOperation(value = "批量修改配置信息", httpMethod = "POST")
    public Result updateDeployList(@RequestBody List<SysLinuxDeployDO> inputList) {
        linuxDeployService.updateDeployList(inputList);
        return ResultUtil.success();
    }

    // 批量删除配置信息
    @PostMapping("/deleteDeployList")
    @ApiOperation(value = "批量删除配置信息", httpMethod = "POST")
    public Result deleteDeployList(@RequestBody IdListInput input) {
        linuxDeployService.deleteDeployList(input);
        return ResultUtil.success();
    }

}
