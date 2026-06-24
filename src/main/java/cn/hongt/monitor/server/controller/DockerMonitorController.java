package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.input.NodeDataInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeDockerOutput;
import cn.hongt.monitor.server.service.ZrDockerDeployService;
import cn.hongt.monitor.server.service.ZrDockerRecordService;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Docker服务集群监控
 * @author yrb
 * @date 2024-02-02
 */
@Api(tags = "Docker服务集群监控")
@RestController
@RequestMapping("/docker")
public class DockerMonitorController {

    @Autowired
    private ZrDockerRecordService zrDockerRecordService;
    @Autowired
    private ZrDockerDeployService zrDockerDeployService;

    // 节点信息监控
    @PostMapping("/queryNodeList")
    @ApiOperation(value = "节点信息列表", httpMethod = "POST")
    public Result queryNodeList() {
        return ResultUtil.success(zrDockerDeployService.queryNodeList());
    }

    // 查询-多节点-多服务器信息列表
    @PostMapping("/queryServersByNode")
    @ApiOperation(value = "单节点-多服务器信息列表", httpMethod = "POST")
    public Result queryNodeAndServers(@Validated @RequestBody NodeDataInput input) {
        return ResultUtil.success(zrDockerDeployService.queryNodeAndServers(input.getNodeName()));
    }

    // 服务器信息列表
    @PostMapping("/queryServerList")
    @ApiOperation(value = "查询服务器信息列表", httpMethod = "POST")
    public Result queryServerList() {
        return ResultUtil.success(zrDockerDeployService.queryServerList());
    }

    @PostMapping("/queryDockerNameList")
    @ApiOperation(value = "单服务器-Docker镜像信息列表", httpMethod = "POST")
    public Result queryDockerNameList(@Validated @RequestBody NodeDataInput input) {
        return ResultUtil.success(zrDockerDeployService.queryDockerNameList(input));
    }

    @PostMapping("/queryNodeDocker")
    @ApiOperation(value = "单节点-单IP-docker容器-最新监控查询", httpMethod = "POST")
    public Result<List<NodeDockerOutput>> queryNodeDocker(@Validated @RequestBody NodeDataInput input) {
        return zrDockerRecordService.queryNodeDocker(input);
    }

    @PostMapping("/queryDockerRecord")
    @ApiOperation(value = "容器监控历史记录查询", httpMethod = "POST")
    public Result<Map<String,Map<String,List<HardresultOutput>>>> queryDockerRecord(@Validated @RequestBody HardWareMonitorInput input) {
        return zrDockerRecordService.queryDockerRecord(input);
    }


}
