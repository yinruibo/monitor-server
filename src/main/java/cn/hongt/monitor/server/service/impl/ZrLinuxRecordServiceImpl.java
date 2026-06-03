package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.DateUtils;
import cn.hongt.monitor.server.common.utils.MetricUnitUtil;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.NodeMonitorOutput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.service.ZrLinuxRecordService;
import cn.hongt.monitor.server.common.utils.HardWareUtils;
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
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class ZrLinuxRecordServiceImpl extends ServiceImpl<ZrLinuxRecordEleMapper, ZrLinuxRecordEleDO> implements ZrLinuxRecordService {

    @Autowired
    private ZrLinuxDeployMapper linuxDeployMapper;

    @Override
    public List<HardresultOutput> queryServerRecord(HardWareMonitorInput input) {
        List<HardresultOutput> resultList = new ArrayList<>();
        String startTime = input.getStartTime();
        //        硬件监控的开始时间
        Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        // 2022-12-08 14:49:10.119
        if("5".equals(startTime.substring(startTime.length()-1,startTime.length()))){
            start = DateUtils.addDate(start,0,0,0,0,0,-5,0);
        }
        input.setStartTime(DateUtils.dateToString(start,DateUtils.dateType5));
        // 查询数据库最新时间，以确定 时间段的结束时间
        Date end = queryEndTime(input);
        if(end == null || StringUtils.isBlank(input.getIp())){
            return resultList;
        }
        // 如果配置文件，配置此 Type为不可查询，则返回结果为 空
        List<SysLinuxDeployDO> linuxDeployList = linuxDeployMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
            .eq(SysLinuxDeployDO::getIp,input.getIp()).eq(SysLinuxDeployDO::getIsShow,0));
        if(linuxDeployList.isEmpty()){
            return resultList;
        }

        List<ZrLinuxRecordEleDO> hardWareList = this.baseMapper.selectList(new QueryWrapper<ZrLinuxRecordEleDO>().lambda()
            .eq(ZrLinuxRecordEleDO::getIp,input.getIp()).eq(ZrLinuxRecordEleDO::getType,input.getType())
                .between(ZrLinuxRecordEleDO::getCreateTime,start,end).orderByAsc(ZrLinuxRecordEleDO::getCreateTime));

        List<LinuxValueOutput> serverlist = buildSeriesForQuery(hardWareList, input.getType());

        if(!serverlist.isEmpty()){
            resultList = HardWareUtils.getLinuxInter(serverlist,input);
        }
        return resultList;
    }

    /*
     * 查询数据库最新时间，以确定 时间段的结束时间
     */
    private Date queryEndTime(HardWareMonitorInput input){
        //        前端传过来的开始时间
        Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        //        前端传过来 的结束时间
        Date end = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
        // 查询数据库最新时间   系统信息监测表
        List<ZrLinuxRecordEleDO> hardWareList = this.baseMapper.selectList(new LambdaQueryWrapper<ZrLinuxRecordEleDO>()
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
    public NodeMonitorOutput queryNodeMonitor(String nodeName,String ip) {
        NodeMonitorOutput output = new NodeMonitorOutput();
        List<NodeMonitorOutput.DiskOutput> diskList = new ArrayList<>();
        // 读取配置表获取Ip名称
        List<SysLinuxDeployDO> linuxDeployList = linuxDeployMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
                .eq(SysLinuxDeployDO::getIp,ip).isNotNull(SysLinuxDeployDO::getIpName).eq(SysLinuxDeployDO::getIsShow,0));
        if(linuxDeployList.isEmpty()){
            return output;
        }

        output.setNodeName(nodeName);
        output.setIpName(linuxDeployList.get(0).getIpName());
        output.setIp(ip);
        List<ZrLinuxRecordEleDO> linuxRecord = this.baseMapper.selectList(new LambdaQueryWrapper<ZrLinuxRecordEleDO>()
                .eq(ZrLinuxRecordEleDO::getIp,ip).orderByDesc(ZrLinuxRecordEleDO::getCreateTime).last("limit 10 offset 0"));
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
                } catch (NumberFormatException e) {
                    log.warn("Linux监控数据格式异常，跳过: type={}, dataRate={}", hard.getType(), hard.getDataRate());
                }
            }
            return buildDisplaySeries(timeList, valuesInKb);
        }

        List<LinuxValueOutput> serverList = new ArrayList<>();
        for (ZrLinuxRecordEleDO hard : hardWareList) {
            try {
                serverList.add(LinuxValueOutput.builder()
                    .Time(hard.getCreateTime())
                    .values(Double.parseDouble(hard.getDataRate()))
                    .unit(hard.getUnit())
                    .build());
            } catch (NumberFormatException e) {
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
                .Time(timeList.get(i))
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
