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
public class Win10CronJob {

    private static final String winCorn = "0 0/5 * * * ?";

    @Autowired
    private ZrWarnRecordEleService zrWarnRecordEleService;

    @Scheduled(cron = winCorn)
    @ApiOperation(value = "Win硬件CPU监控定时任务-后端执行", httpMethod = "POST")
    public Result warnWinCPUByScheduled() {
        if (!isWindowsOs()) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnWinCPUBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = winCorn)
    @ApiOperation(value = "Win硬件内存监控定时任务-后端执行", httpMethod = "POST")
    public Result warnWinMemoryByScheduled() {
        if (!isWindowsOs()) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnWinMemoryBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = winCorn)
    @ApiOperation(value = "Win硬件磁盘监控定时任务-后端执行", httpMethod = "POST")
    public Result warnWinDiskByScheduled() {
        if (!isWindowsOs()) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnWinDiskBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = winCorn)
    @ApiOperation(value = "Win硬件IO监控定时任务-后端执行", httpMethod = "POST")
    public Result warnWinIOByScheduled() {
        if (!isWindowsOs()) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnWinIOBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = winCorn)
    @ApiOperation(value = "Win硬件网卡监控定时任务-后端执行", httpMethod = "POST")
    public Result warnWinNetWorkByScheduled() {
        if (!isWindowsOs()) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnWinNetWorkBySch();
        return ResultUtil.success();
    }

    private boolean isWindowsOs() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("win");
    }
}
