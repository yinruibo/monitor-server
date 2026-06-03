package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.service.DockerMetricCollectorService;
import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Slf4j
@Component
public class DockerCornJob {

    private static final String dockerCorn = "0 0/5 * * * ?";

    @Autowired
    private ZrWarnRecordEleService zrWarnRecordEleService;
    @Autowired
    private DockerMetricCollectorService dockerMetricCollectorService;


    @Scheduled(cron = dockerCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Docker容器监控-CPU、内存-定时任务-后端执行", httpMethod = "POST")
    public Result warnDockerBySchCpuMemory() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnDockerBySchCpuMemory();
        return ResultUtil.success();
    }

    @Scheduled(cron = dockerCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Docker容器监控-IO-定时任务-后端执行", httpMethod = "POST")
    public Result warnDockerBySchDiskIO() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnDockerIOBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = dockerCorn)  //逐5分钟执行一次任务(方法)
    @ApiOperation(value = "Docker容器监控-网卡-定时任务-后端执行", httpMethod = "POST")
    public Result warnDockerBySchNetwork() {
        //todo Windows系统启动不执行定时任务
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        zrWarnRecordEleService.warnDockerNetWorkBySch();
        return ResultUtil.success();
    }

    @Scheduled(cron = "0 0 * * * ?")
    @ApiOperation(value = "Docker容器监控-刷新容器订阅缓存", httpMethod = "POST")
    public Result refreshDockerSubscriptions() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase(Locale.ROOT).startsWith("win")) {
            return ResultUtil.success();
        }
        dockerMetricCollectorService.refreshContainerSubscriptions();
        return ResultUtil.success();
    }


}
