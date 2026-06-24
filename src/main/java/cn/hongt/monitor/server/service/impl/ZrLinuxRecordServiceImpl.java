package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.*;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.input.NodeDataInput;
import cn.hongt.monitor.server.dto.output.NodeMonitorOutput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.service.ZrLinuxRecordService;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.LinuxValueOutput;
import cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO;
import cn.hongt.monitor.server.mapper.ZrLinuxDeployMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxRecordEleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ZrLinuxRecordServiceImpl extends ServiceImpl<ZrLinuxRecordEleMapper, ZrLinuxRecordEleDO> implements ZrLinuxRecordService {

    @Autowired
    private ZrLinuxDeployMapper linuxDeployMapper;

    @Autowired
    @Qualifier("queryMonitorExecutor")
    private Executor queryMonitorExecutor;

    @Override
    public Result<Map<String,List<HardresultOutput>>> queryServerRecord(HardWareMonitorInput input) {
        Map<String,List<HardresultOutput>> resultMap = new ConcurrentHashMap<>();

        if(StringUtils.isBlank(input.getStartTime())){
            return ResultUtil.errorMsg("未传入开始时间");
        }

        String startTime = input.getStartTime();
        // 硬件监控的开始时间
        Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        if("5".equals(startTime.substring(startTime.length()-1))){
            start = DateUtils.addDate(start,0,0,0,0,0,-5,0);
        }
        input.setStartTime(DateUtils.dateToString(start,DateUtils.dateType5));

        if( StringUtils.isBlank(input.getIp())|| input.getTypeList() == null || input.getTypeList().isEmpty()){
            return ResultUtil.errorMsg("未传入IP、要素等信息");
        }

        // 查询数据库最新时间，以确定 时间段的结束时间
        Date end = queryEndTime(input);
        if(end == null){
            return ResultUtil.success(resultMap);
        }

        // 使用 queryMonitorExecutor 线程池并行查询各 type 的监控数据
        Date finalStart = start;
        Date finalEnd = end;
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for(String type : input.getTypeList()){
            // 如果配置文件配置此 Type 为不可查询，则跳过
            SysLinuxDeployDO linuxDeploy = StationConst.linuxDepMap.get(type);
            if(linuxDeploy == null || linuxDeploy.getIsShow() == 1){
                continue;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                List<ZrLinuxRecordEleDO> hardWareList = this.baseMapper.selectList(
                    new QueryWrapper<ZrLinuxRecordEleDO>().lambda()
                        .eq(ZrLinuxRecordEleDO::getIp, input.getIp())
                        .eq(ZrLinuxRecordEleDO::getType, type)
                        .between(ZrLinuxRecordEleDO::getCreateTime, finalStart, finalEnd)
                        .orderByAsc(ZrLinuxRecordEleDO::getCreateTime));

                List<LinuxValueOutput> serverlist = buildSeriesForQuery(hardWareList, type);

                List<HardresultOutput> resultList = new ArrayList<>();
                if(!serverlist.isEmpty()){
                    resultList = HardWareUtils.spiltMonitorData(serverlist, input);
                }
                resultMap.put(type, resultList);
            }, queryMonitorExecutor);

            futures.add(future);
        }

        // 等待所有查询任务完成，设置 30 秒超时防止线程阻塞
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Linux监控数据并行查询超时(30s)，ip={}", input.getIp());
        } catch (Exception e) {
            log.error("Linux监控数据并行查询异常，ip={}", input.getIp(), e);
        }

        return ResultUtil.success(resultMap);
    }

    /*
     * 查询数据库最新时间，以确定 时间段的结束时间
     */
    private Date queryEndTime(HardWareMonitorInput input){
        //        前端传过来的开始时间
        Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        //        前端传过来 的结束时间
        Date end = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
        // 查询数据库最新时间   系统信息监测表（按 IP 过滤，避免返回其他 IP 的数据）
        List<ZrLinuxRecordEleDO> hardWareList = this.baseMapper.selectList(new LambdaQueryWrapper<ZrLinuxRecordEleDO>()
                .eq(StringUtils.isNotBlank(input.getIp()), ZrLinuxRecordEleDO::getIp, input.getIp())
                .orderByDesc(ZrLinuxRecordEleDO::getCreateTime).last(StationConst.LIMIT_DATA_SERVER));
        if(hardWareList.isEmpty()){
            return null;
        }
        if(hardWareList.get(0).getCreateTime().before(start)){
            return null;
        }
        // 如果数据库时间在传入时间段之间，重新定义结束时间
        if(hardWareList.get(0).getCreateTime().after(start) && hardWareList.get(0).getCreateTime().before(end)){
            input.setEndTime(DateUtils.dateToString(hardWareList.get(0).getCreateTime(),DateUtils.dateType5));
            end = hardWareList.get(0).getCreateTime();
        }
        return end;
    }

    @Override
    public NodeMonitorOutput queryNodeMonitor(NodeDataInput input) {
        NodeMonitorOutput output = new NodeMonitorOutput();
        List<NodeMonitorOutput.DiskOutput> diskList = new ArrayList<>();
        // 读取配置表获取Ip名称
        List<SysLinuxDeployDO> linuxDeployList = linuxDeployMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
                .eq(SysLinuxDeployDO::getIp,input.getIp()).isNotNull(SysLinuxDeployDO::getIpName).eq(SysLinuxDeployDO::getIsShow,0));
        if(linuxDeployList.isEmpty()){
            return output;
        }

        output.setNodeName(input.getNodeName());
        output.setIpName(linuxDeployList.get(0).getIpName());
        output.setIp(input.getIp());
        List<ZrLinuxRecordEleDO> linuxRecord = this.baseMapper.selectList(new LambdaQueryWrapper<ZrLinuxRecordEleDO>()
                .eq(ZrLinuxRecordEleDO::getIp,input.getIp()).orderByDesc(ZrLinuxRecordEleDO::getCreateTime).last("limit 10 offset 0"));
        for(ZrLinuxRecordEleDO record : linuxRecord){
            if(record.getType().equals(WarnRecordEnum.linux_cpu.getCode()) ||
                    record.getType().equals(WarnRecordEnum.win_cpu.getCode())){
                output.setCpu(record.getDataRate()+record.getUnit());
            }else if(record.getType().equals(WarnRecordEnum.linux_memory.getCode()) ||
                    record.getType().equals(WarnRecordEnum.win_memory.getCode())){
                output.setMemory(record.getDataRate()+record.getUnit());
            }else if(record.getType().contains(WarnRecordEnum.linux_IO_read.getCode()) ||
                    record.getType().contains(WarnRecordEnum.win_IO_read.getCode())){
                output.setIORead(record.getDataRate()+record.getUnit());
            }else if(record.getType().contains(WarnRecordEnum.linux_IO_write.getCode()) ||
                    record.getType().contains(WarnRecordEnum.win_IO_write.getCode())){
                output.setIOWrite(record.getDataRate()+record.getUnit());
            }else if(record.getType().contains(WarnRecordEnum.linux_disk.getCode()) ||
                    record.getType().contains(WarnRecordEnum.win_disk.getCode())){
                NodeMonitorOutput.DiskOutput diskOutput = new NodeMonitorOutput.DiskOutput();
                diskOutput.setDisk(record.getDataRate()+record.getUnit());
                diskOutput.setType(record.getType());
                diskList.add(diskOutput);
                output.setDiskList(diskList);
            }else if(record.getType().contains(WarnRecordEnum.linux_network_up.getCode()) ||
                        record.getType().contains(WarnRecordEnum.win_network_up.getCode())){
                output.setNetWorkUp(record.getDataRate()+record.getUnit());
            }else if(record.getType().contains(WarnRecordEnum.linux_network_down.getCode()) ||
                        record.getType().contains(WarnRecordEnum.win_network_down.getCode())){
                output.setNetWorkDown(record.getDataRate()+record.getUnit());
            }
            output.setTimestamp(record.getCreateTime());
        }
        return output;
    }

    private List<LinuxValueOutput> buildSeriesForQuery(List<ZrLinuxRecordEleDO> hardWareList, String type) {
        if (hardWareList == null || hardWareList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isRateMetric(type)) {
            List<Date> timeList = new ArrayList<>();
            List<Double> valuesInKb = new ArrayList<>();
            for (ZrLinuxRecordEleDO hard : hardWareList) {
                try {
                    valuesInKb.add(MetricUnitUtil.toKbPerSecond(Double.parseDouble(hard.getDataRate()), hard.getUnit()));
                    timeList.add(hard.getCreateTime());
                } catch (NumberFormatException | NullPointerException e) {
                    log.warn("Linux监控数据格式异常，跳过: type={}, dataRate={}", hard.getType(), hard.getDataRate());
                }
            }
            return buildDisplaySeries(timeList, valuesInKb);
        }

        List<LinuxValueOutput> serverList = new ArrayList<>();
        for (ZrLinuxRecordEleDO hard : hardWareList) {
            try {
                serverList.add(LinuxValueOutput.builder()
                    .time(hard.getCreateTime())
                    .values(Double.parseDouble(hard.getDataRate()))
                    .unit(hard.getUnit())
                    .build());
            } catch (NumberFormatException | NullPointerException e) {
                log.warn("Linux监控数据格式异常，跳过: type={}, dataRate={}", hard.getType(), hard.getDataRate());
            }
        }
        return serverList;
    }

    private List<LinuxValueOutput> buildDisplaySeries(List<Date> timeList, List<Double> valuesInKb) {
        if (valuesInKb.isEmpty()) {
            return Collections.emptyList();
        }
        // 根据区间峰值选择统一展示单位。
        String displayUnit = MetricUnitUtil.selectDisplayUnit(valuesInKb);
        List<Double> convertedValues = MetricUnitUtil.convertSeriesByDisplayUnit(valuesInKb, displayUnit);
        List<LinuxValueOutput> result = new ArrayList<>();
        for (int i = 0; i < convertedValues.size(); i++) {
            result.add(LinuxValueOutput.builder()
                .time(timeList.get(i))
                .values(convertedValues.get(i))
                .unit(displayUnit)
                .build());
        }
        return result;
    }

    private boolean isRateMetric(String type) {
        return WarnRecordEnum.linux_IO_read.getCode().equals(type)
            || WarnRecordEnum.linux_IO_write.getCode().equals(type)
            || WarnRecordEnum.linux_network_up.getCode().equals(type)
            || WarnRecordEnum.linux_network_down.getCode().equals(type)
            || WarnRecordEnum.win_IO_read.getCode().equals(type)
            || WarnRecordEnum.win_IO_write.getCode().equals(type)
            || WarnRecordEnum.win_network_up.getCode().equals(type)
            || WarnRecordEnum.win_network_down.getCode().equals(type);
    }


}
