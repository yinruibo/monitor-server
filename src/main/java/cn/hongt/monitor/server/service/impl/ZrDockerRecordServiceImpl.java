package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.DateUtils;
import cn.hongt.monitor.server.common.utils.HardWareUtils;
import cn.hongt.monitor.server.common.utils.MetricUnitUtil;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.input.NodeDataInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.LinuxValueOutput;
import cn.hongt.monitor.server.dto.output.NodeDockerOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.mapper.ZrDockerRecordEleMapper;
import cn.hongt.monitor.server.service.ZrDockerRecordService;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZrDockerRecordServiceImpl  extends ServiceImpl<ZrDockerRecordEleMapper, ZrDockerRecordEleDO> implements ZrDockerRecordService {

    @Autowired
    private ZrDockerDeployMapper dockerDeployMapper;

    @Autowired
    @Qualifier("queryMonitorExecutor")
    private Executor queryMonitorExecutor;

    @Override
    public Result<List<NodeDockerOutput>> queryNodeDocker(NodeDataInput input) {
        // 查询 数据记录表中 最新数据时间的 SQL 如下
        List<NodeDockerOutput> resultList = new ArrayList<>();

        // 获取docker配置表
        Map<String,SysDockerDeployEleDO> dockerDepMap = new HashMap<>();
        List<SysDockerDeployEleDO> dockerDepList = dockerDeployMapper.selectList(new LambdaQueryWrapper<SysDockerDeployEleDO>()
                .eq(SysDockerDeployEleDO::getIp,input.getIp()).eq(SysDockerDeployEleDO::getType,WarnRecordEnum.docker_cpu.getCode())
                .eq(SysDockerDeployEleDO::getIsShow,0));
        if(dockerDepList.isEmpty()){
            return ResultUtil.success(resultList);
        }else {
            // 重复 key 保留第一条，防止数据库脏数据导致 Collectors.toMap 抛出 IllegalStateException
            dockerDepMap = dockerDepList.stream().collect(Collectors.toMap(
                    SysDockerDeployEleDO::getDockerName, Function.identity(), (existing, replacement) -> existing));
        }

        List<String> typeList = Arrays.asList(new String[]{WarnRecordEnum.docker_cpu.getCode(),WarnRecordEnum.docker_memory.getCode(),
                WarnRecordEnum.docker_IO_read.getCode(),WarnRecordEnum.docker_IO_write.getCode(),WarnRecordEnum.docker_network_up.getCode(),
                WarnRecordEnum.docker_network_down.getCode()});

        //todo 获取docker 所有镜像最新记录的 -最低的创建时间
        List<ZrDockerRecordEleDO> createTimeList = this.baseMapper.selectList(new QueryWrapper<ZrDockerRecordEleDO>()
            .select("docker_name as dockerName","type","max(create_time) as createTime")
            .eq("ip",input.getIp()).in("docker_name",dockerDepMap.keySet()).in("type",typeList)
            .groupBy("docker_name","type").orderByAsc("createTime").last("limit 3 offset 0"));
        if(createTimeList.isEmpty()){
            return ResultUtil.success(resultList);
        }
        // 获取指定时间内的所有镜像数据列表
        List<ZrDockerRecordEleDO> dockerRecordList = this.baseMapper.selectList(new QueryWrapper<ZrDockerRecordEleDO>()
                .eq("ip",input.getIp()).in("docker_name",dockerDepMap.keySet()).in("type",typeList)
                .ge("create_time",createTimeList.get(0).getCreateTime()));
        //todo 根据数据进行筛选获取最新创建时间的一条数据
        List<ZrDockerRecordEleDO> distinctList = dockerRecordList.stream().collect(Collectors.groupingBy(u -> u.getType()+"_"+u.getDockerName(),
                Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(ZrDockerRecordEleDO::getCreateTime)),Optional::get)
                )).values().stream().collect(Collectors.toList());
        //todo 将数据根据容器列表进行分组
        Map<String,List<ZrDockerRecordEleDO>> dockerImageMap = distinctList.stream().collect(Collectors.groupingBy(ZrDockerRecordEleDO::getDockerName));

        for(String dockerImage : dockerImageMap.keySet()){
            NodeDockerOutput output = new NodeDockerOutput();
            output.setNodeName(input.getNodeName());
            output.setIp(input.getIp());
            output.setDockerName(dockerImage);
            output.setTaskName(ObjectUtil.isNotNull(dockerDepMap.get(dockerImage)) ? dockerDepMap.get(dockerImage).getTaskName(): null);
            // 数据循环填充数值
            List<ZrDockerRecordEleDO> imageDataList = dockerImageMap.get(dockerImage);
            if(!imageDataList.isEmpty()){
                // 填充 CPU、内存等信息
                for(ZrDockerRecordEleDO record : imageDataList){
                    if(record.getType().equals(WarnRecordEnum.docker_cpu.getCode())){
                        output.setCpu(record.getDataRate()+record.getUnit());
                    }else if(record.getType().equals(WarnRecordEnum.docker_memory.getCode())){
                        output.setMemory(record.getDataRate()+record.getUnit());
                    }else if(record.getType().equals(WarnRecordEnum.docker_IO_read.getCode())){
                        output.setIORead(record.getDataRate()+record.getUnit());
                    }else if(record.getType().equals(WarnRecordEnum.docker_IO_write.getCode())){
                        output.setIOWrite(record.getDataRate()+record.getUnit());
                    }else if(record.getType().equals(WarnRecordEnum.docker_network_up.getCode())){
                        output.setNetworkUp(record.getDataRate()+record.getUnit());
                    }else if(record.getType().equals(WarnRecordEnum.docker_network_down.getCode())){
                        output.setNetworkDown(record.getDataRate()+record.getUnit());
                    }
                    output.setTimestamp(record.getCreateTime());
                }
                resultList.add(output);
            }
        }
        return ResultUtil.success(resultList);
    }

    @Override
    public Result<Map<String,Map<String,List<HardresultOutput>>>> queryDockerRecord(HardWareMonitorInput input) {
        Map<String,Map<String,List<HardresultOutput>>> resultMap = new HashMap<>();

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

        if(StringUtils.isBlank(input.getIp())
                || input.getDockerNameList() == null || input.getDockerNameList().isEmpty()
                || input.getTypeList() == null || input.getTypeList().isEmpty()){
            return ResultUtil.errorMsg("未传入IP、容器名称、要素等信息");
        }
        // 查询数据库最新时间，以确定 时间段的结束时间
        Date end = queryEndTime(input);
        if(end == null){
            return ResultUtil.success(resultMap);
        }

        // 使用 queryMonitorExecutor 线程池并行查询各 (dockerName, type) 组合的监控数据
        Date finalStart = start;
        Date finalEnd = end;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // 先收集所有 dockerName 对应的 typeMap，join 后再过滤空条目
        Map<String,Map<String,List<HardresultOutput>>> allDockerMap = new HashMap<>();

        for(String dockerName : input.getDockerNameList()){
            Map<String,List<HardresultOutput>> dockerTypeMap = new ConcurrentHashMap<>();
            allDockerMap.put(dockerName, dockerTypeMap);

            for(String type : input.getTypeList()){
                // 如果配置文件配置此 Type 为不可查询，则跳过
                SysDockerDeployEleDO dockerDeploy = StationConst.dockerDepMap.get(dockerName+"_"+type);
                if(dockerDeploy == null || dockerDeploy.getIsShow() == 1){
                    continue;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    LambdaQueryWrapper<ZrDockerRecordEleDO> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(ZrDockerRecordEleDO::getIp, input.getIp())
                            .between(ZrDockerRecordEleDO::getCreateTime, finalStart, finalEnd)
                            .orderByDesc(ZrDockerRecordEleDO::getCreateTime);
                    queryWrapper.eq(StringUtils.isNotBlank(dockerName), ZrDockerRecordEleDO::getDockerName, dockerName);
                    queryWrapper.eq(StringUtils.isNotBlank(type), ZrDockerRecordEleDO::getType, type);

                    List<ZrDockerRecordEleDO> hardWareList = this.baseMapper.selectList(queryWrapper);

                    List<LinuxValueOutput> serverlist = buildSeriesForQuery(hardWareList, type);

                    List<HardresultOutput> resultList = new ArrayList<>();
                    if(!serverlist.isEmpty()){
                        resultList = HardWareUtils.spiltMonitorData(serverlist, input);
                    }
                    dockerTypeMap.put(type, resultList);
                }, queryMonitorExecutor);

                futures.add(future);
            }
        }

        // 等待所有查询任务完成，设置 30 秒超时防止线程阻塞
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Docker监控数据并行查询超时(30s)，ip={}", input.getIp());
        } catch (Exception e) {
            log.error("Docker监控数据并行查询异常，ip={}", input.getIp(), e);
        }

        // 过滤掉空的 dockerTypeMap 条目（所有 type 均被跳过的情况）
        for(Map.Entry<String,Map<String,List<HardresultOutput>>> entry : allDockerMap.entrySet()){
            if(!entry.getValue().isEmpty()){
                resultMap.put(entry.getKey(), entry.getValue());
            }
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
        List<ZrDockerRecordEleDO> hardWareList = this.baseMapper.selectList(new LambdaQueryWrapper<ZrDockerRecordEleDO>()
                .eq(StringUtils.isNotBlank(input.getIp()),ZrDockerRecordEleDO::getIp, input.getIp())
                .orderByDesc(ZrDockerRecordEleDO::getCreateTime).last(StationConst.LIMIT_DATA_SERVER));
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

    private List<LinuxValueOutput> buildSeriesForQuery(List<ZrDockerRecordEleDO> hardWareList, String type) {
        if (hardWareList == null || hardWareList.isEmpty()) {
            return Collections.emptyList();
        }
        if (isRateMetric(type)) {
            List<Date> timeList = new ArrayList<>();
            List<Double> valuesInKb = new ArrayList<>();
            for (ZrDockerRecordEleDO hard : hardWareList) {
                try {
                    valuesInKb.add(MetricUnitUtil.toKbPerSecond(Double.parseDouble(hard.getDataRate()), hard.getUnit()));
                    timeList.add(hard.getCreateTime());
                } catch (NumberFormatException | NullPointerException e) {
                    log.warn("Docker监控数据格式异常，跳过: dockerName={}, dataRate={}", hard.getDockerName(), hard.getDataRate());
                }
            }
            return buildDisplaySeries(timeList, valuesInKb);
        }

        List<LinuxValueOutput> serverList = new ArrayList<>();
        for (ZrDockerRecordEleDO hard : hardWareList) {
            try {
                serverList.add(LinuxValueOutput.builder()
                    .time(hard.getCreateTime())
                    .values(Double.parseDouble(hard.getDataRate()))
                    .unit(hard.getUnit())
                    .build());
            } catch (NumberFormatException | NullPointerException e) {
                log.warn("Docker监控数据格式异常，跳过: dockerName={}, dataRate={}", hard.getDockerName(), hard.getDataRate());
            }
        }
        return serverList;
    }

    private List<LinuxValueOutput> buildDisplaySeries(List<Date> timeList, List<Double> valuesInKb) {
        if (valuesInKb.isEmpty()) {
            return Collections.emptyList();
        }
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
        return WarnRecordEnum.docker_IO_read.getCode().equals(type)
            || WarnRecordEnum.docker_IO_write.getCode().equals(type)
            || WarnRecordEnum.docker_network_up.getCode().equals(type)
            || WarnRecordEnum.docker_network_down.getCode().equals(type);
    }

}
