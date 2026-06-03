package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.service.ZrLinuxRecordService;
import cn.hongt.monitor.server.service.ZrLinuxDeployService;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Linux服务集群监控
 * @author yrb
 * @date 2024-02-02
 */
@Api(tags = "Linux服务集群监控")
@RestController
@RequestMapping("/linux")
public class LinuxMonitorController {

    @Autowired
    private ZrLinuxDeployService linuxDeployService;
    @Autowired
    private ZrLinuxRecordService linuxRecordService;
    // 节点信息监控
    @GetMapping("/queryNodeList")
    @ApiOperation(value = "节点信息列表", httpMethod = "GET")
    public Result queryNodeList() {
        return ResultUtil.success(linuxDeployService.queryNodeList());
    }

    // 查询-多节点-多服务器信息列表
    @GetMapping("/queryServersByNode")
    @ApiOperation(value = "单节点-多服务器信息列表", httpMethod = "GET")
    public Result queryNodeAndServers(@RequestParam("nodeName") String nodeName) {
        return ResultUtil.success(linuxDeployService.queryNodeAndServers(nodeName));
    }

    // 服务器信息列表
    @GetMapping("/queryServerList")
    @ApiOperation(value = "服务器信息列表", httpMethod = "GET")
    public Result queryServerList() {
        return ResultUtil.success(linuxDeployService.queryServerList());
    }

    // 节点监控
    @GetMapping("/queryNodeMonitor")
    @ApiOperation(value = "单节点-单IP-最新监控查询", httpMethod = "GET")
    public Result queryNodeMonitor(@RequestParam("nodeName") String nodeName,@RequestParam("ip") String ip) {
        return ResultUtil.success(linuxRecordService.queryNodeMonitor(nodeName,ip));
    }

    @PostMapping("/queryServerRecord")
    @ApiOperation(value = "服务器监控历史记录查询", httpMethod = "POST")
    public Result queryServerRecord(@Validated @RequestBody HardWareMonitorInput input) {
        return ResultUtil.success(linuxRecordService.queryServerRecord(input));
    }

}
