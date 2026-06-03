package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Slf4j
@Component
public class LinuxCornJob {

    private static final String linuxCorn = "0 0/5 * * * ?";

    @Autowired
    private ZrWarnRecordEleService zrWarnRecordEleService;

    @Scheduled(cron = linuxCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Linux硬件CPU监控定时任务-后端执行", httpMethod = "POST")
    public Result warnLinuxCPUByScheduled() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnLinuxCPUBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = linuxCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Linux硬件内存监控定时任务-后端执行", httpMethod = "POST")
    public Result warnLinuxMemoryByScheduled() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnLinuxMemoryBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = linuxCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Linux硬件磁盘监控定时任务-后端执行", httpMethod = "POST")
    public Result warnLinuxDiskByScheduled() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnLinuxDiskBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = linuxCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Linux硬件IO监控定时任务-后端执行", httpMethod = "POST")
    public Result warnLinuxIOByScheduled() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnLinuxIOBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = linuxCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Linux硬件网卡监控定时任务-后端执行", httpMethod = "POST")
    public Result warnLinuxNetWorkByScheduled() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnLinuxNetWorkBySch();
        return ResultUtil.success();
    }
}
