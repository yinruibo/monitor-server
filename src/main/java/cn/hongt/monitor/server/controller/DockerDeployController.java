package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.service.ZrDockerDeployService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Docker监控配置模块
 * @author yrb
 * @date 2024-08-15
 */
@Api(tags = "Docker监控配置模块")
@RestController
@RequestMapping("/dockerDeploy")
public class DockerDeployController {

    @Autowired
    private ZrDockerDeployService dockerDeployService;
    // 配置信息列表
    @PostMapping("/queryDeployList")
    @ApiOperation(value = "配置信息列表", httpMethod = "POST")
    public Result queryDeployList(@RequestBody DockerDeployInput input) {
        return ResultUtil.success(dockerDeployService.queryDeployList(input));
    }

    // 批量修改配置信息
    @PostMapping("/updateDeployList")
    @ApiOperation(value = "批量修改配置信息", httpMethod = "POST")
    public Result updateDeployList(@RequestBody List<SysDockerDeployEleDO> inputList) {
        dockerDeployService.updateDeployList(inputList);
        return ResultUtil.success();
    }

    // 批量删除配置信息
    @PostMapping("/deleteDeployList")
    @ApiOperation(value = "批量删除配置信息", httpMethod = "POST")
    public Result deleteDeployList(@RequestBody IdListInput input) {
        dockerDeployService.deleteDeployList(input);
        return ResultUtil.success();
    }

}
