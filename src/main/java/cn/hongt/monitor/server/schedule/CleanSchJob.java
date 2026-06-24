package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO;
import cn.hongt.monitor.server.entity.ZrWarnRecordEleDO;
import cn.hongt.monitor.server.mapper.ZrDockerRecordEleMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxRecordEleMapper;
import cn.hongt.monitor.server.mapper.ZrWarnRecordEleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * @author yrb
 * @date 2024/2/26
 * @Description: 定时清理表中的数据，只保留15天
 */
@Slf4j
@Component
@Api(tags = "定时清理服务")
public class CleanSchJob {

    @Autowired
    private ZrWarnRecordEleMapper warnRecordEleMapper;
    @Autowired
    private ZrLinuxRecordEleMapper linuxRecordEleMapper;
    @Autowired
    private ZrDockerRecordEleMapper dockerRecordEleMapper;

    // zr_warn_record_ele、zr_linux_record_ele  表数据最大保留15天
    @Scheduled(cron = "0 0 2 * * ?")
    @ApiOperation(value = "定时删除-告警日志表", httpMethod = "POST")
    public void cleanWarnRecord() {
        ZrWarnRecordEleDO latestRecord = warnRecordEleMapper.selectOne(new LambdaQueryWrapper<ZrWarnRecordEleDO>()
                .orderByDesc(ZrWarnRecordEleDO::getCreateTime)
                .last("limit 1"));
        if (latestRecord != null && latestRecord.getCreateTime() != null) {
            Date date = new Date(latestRecord.getCreateTime().getTime() - StationConst.DATA_TIME_FIFTEEN);
            warnRecordEleMapper.delete(new LambdaQueryWrapper<ZrWarnRecordEleDO>().le(ZrWarnRecordEleDO::getCreateTime, date));
        }
    }

    @Scheduled(cron = "0 10 2 * * ?")
    @ApiOperation(value = "定时删除-Linux监控日志表", httpMethod = "POST")
    public void cleanLinuxRecord() {
        ZrLinuxRecordEleDO latestRecord = linuxRecordEleMapper.selectOne(new LambdaQueryWrapper<ZrLinuxRecordEleDO>()
                .orderByDesc(ZrLinuxRecordEleDO::getCreateTime)
                .last("limit 1"));
        if (latestRecord != null && latestRecord.getCreateTime() != null) {
            Date date = new Date(latestRecord.getCreateTime().getTime() - StationConst.DATA_TIME_FIFTEEN);
            linuxRecordEleMapper.delete(new LambdaQueryWrapper<ZrLinuxRecordEleDO>().le(ZrLinuxRecordEleDO::getCreateTime, date));
        }
    }

    @Scheduled(cron = "0 20 2 * * ?")
    @ApiOperation(value = "定时删除-Docker容器日志表", httpMethod = "POST")
    public void cleanDockerRecord() {
        ZrDockerRecordEleDO latestRecord = dockerRecordEleMapper.selectOne(new LambdaQueryWrapper<ZrDockerRecordEleDO>()
                .orderByDesc(ZrDockerRecordEleDO::getCreateTime)
                .last("limit 1"));
        if (latestRecord != null && latestRecord.getCreateTime() != null) {
            Date date = new Date(latestRecord.getCreateTime().getTime() - StationConst.DATA_TIME_FIFTEEN);
            dockerRecordEleMapper.delete(new LambdaQueryWrapper<ZrDockerRecordEleDO>().le(ZrDockerRecordEleDO::getCreateTime, date));
        }
    }
}
