package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.schedule.CleanSchJob;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yrb
 * @date 2024/2/26
 * @Description: 定时清理表中的数据，只保留15天
 */
@Api(tags = "定时清理服务")
@RestController
@RequestMapping("/clean")
public class CleanController {

    @Autowired
    private CleanSchJob cleanSchJob;

    // zr_warn_record_ele、zr_linux_record_ele  表数据最大保留15天
    @GetMapping("/cleanWarnRecord")
    @ApiOperation(value = "定时删除-告警日志表", httpMethod = "GET")
    public void cleanWarnRecord() {
        cleanSchJob.cleanWarnRecord();
    }

    @GetMapping("/cleanLinuxRecord")
    @ApiOperation(value = "定时删除-Linux监控日志表", httpMethod = "GET")
    public void cleanLinuxRecord() {
        cleanSchJob.cleanLinuxRecord();
    }

    @GetMapping("/cleanDockerRecord")
    @ApiOperation(value = "定时删除-Docker容器日志表", httpMethod = "GET")
    public void cleanDockerRecord() {
        cleanSchJob.cleanDockerRecord();
    }
}
