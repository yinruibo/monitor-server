package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.Const;
import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.*;
import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.ZrWarnRecordListInput;
import cn.hongt.monitor.server.dto.output.WarnFaultOutput;
import cn.hongt.monitor.server.dto.output.WarnRecordOutput;
import cn.hongt.monitor.server.dto.output.WarnSignOutput;
import cn.hongt.monitor.server.entity.*;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.*;
import cn.hongt.monitor.server.service.*;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OSFileStore;
import oshi.software.os.FileSystem;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZrWarnRecordEleServiceImpl extends ServiceImpl<ZrWarnRecordEleMapper, ZrWarnRecordEleDO> implements ZrWarnRecordEleService {

    @Autowired
    private SysWarnDeployMapper sysWarnDeployMapper;
    @Autowired
    private ZrLinuxDeployMapper linuxDeployMapper;
    @Autowired
    private ZrLinuxRecordService zrLinuxRecordService;
    @Autowired
    private ZrDockerRecordService dockerRecordService;
    @Autowired
    private ZrDockerDeployMapper dockerDeployMapper;
    @Autowired
    private DockerMetricCollectorService dockerMetricCollectorService;


    private static final SystemInfo systemInfo = new SystemInfo();
    private static final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private static final Pattern ipPattern = Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}" + "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$");

    private static final Integer minThresholdNum = 50;

    private static final Integer middleThresholdNum = 70;

    private static final Integer maxThresholdNum = 80;

    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // 这些锁只保护“缓存未命中 -> 查库 -> 插入 -> 回填缓存”这段临界区，避免并发重复插入。
    private final Object dockerDeployLoadLock = new Object();
    private final Object linuxDeployLoadLock = new Object();
    private final Object warnDeployLoadLock = new Object();
    private final Object linuxNetworkLoadLock = new Object();

    @Override
    public Map<String,Object> queryWarnDepList(ZrWarnRecordListInput input) {
        Map<String,Object> warnRecordMap = new HashMap();
        LambdaQueryWrapper<ZrWarnRecordEleDO> queryWrapper = new LambdaQueryWrapper<>();

        Date startTime = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        Date endTime = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
        // 当起止时间为 null 或颠倒时，交换并始终应用 between 条件，
        // 防止时间过滤条件被跳过导致全表扫描
        if (startTime != null && endTime != null) {
            if (endTime.before(startTime)) {
                Date temp = startTime;
                startTime = endTime;
                endTime = temp;
            }
            queryWrapper.between(ZrWarnRecordEleDO::getCreateTime, startTime, endTime);
        }
        queryWrapper.eq(StringUtils.isNotBlank(input.getWarnType()),ZrWarnRecordEleDO::getWarnType,input.getWarnType());
        queryWrapper.eq(ZrWarnRecordEleDO::getIp,input.getIp());
        queryWrapper.eq(ObjectUtil.isNotNull(input.getGrade()),ZrWarnRecordEleDO::getGrade,input.getGrade());
        queryWrapper.orderByDesc(ZrWarnRecordEleDO::getCreateTime);
        //分页参数
        Page<ZrWarnRecordEleDO> rowPage = new Page(input.getPageNo(), input.getPageSize());
        rowPage = this.baseMapper.selectPage(rowPage,queryWrapper);
        List<WarnRecordOutput> resultList = new ArrayList<>();
        // 循环查询数据添加 warnSign 告警标志
        for(ZrWarnRecordEleDO warn : rowPage.getRecords()){
            WarnRecordOutput output = new WarnRecordOutput();
            BeanUtils.copyProperties(warn,output);
            output.setWarnSign(WarnRecordEnum.getSignByCode(warn.getWarnType()));
            resultList.add(output);
        }
        warnRecordMap.put("data",resultList);
        warnRecordMap.put("total",rowPage.getTotal());
        return warnRecordMap;
    }

    @Override
    public void deleteWarnRecord(IdListInput input) {
        this.baseMapper.deleteBatchIds(input.getIdList());
    }

    @Override
    public Map<String,Object> queryWarnSignList(ZrWarnRecordListInput input) {
        Map<String,Object> warnRecordMap = new HashMap();
        QueryWrapper<ZrWarnRecordEleDO> queryWrapper = new QueryWrapper<>();

        Date startTime = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        Date endTime = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
        if (startTime != null && endTime != null) {
            if (endTime.before(startTime)) {
                Date temp = startTime;
                startTime = endTime;
                endTime = temp;
            }
            queryWrapper.between("create_time", startTime, endTime);
        }

        queryWrapper.eq(ObjectUtil.isNotNull(input.getIp()),"ip",input.getIp());
        queryWrapper.select("warn_type as warnType,count(1) as id").groupBy("warn_type");
        List<ZrWarnRecordEleDO> warnRecordList = this.baseMapper.selectList(queryWrapper);

        List<WarnSignOutput> warnSignList = new ArrayList<>();
        int total = 0;
        if(CollectionUtils.isNotEmpty(warnRecordList)){
            for(ZrWarnRecordEleDO warnRecord : warnRecordList){
                int num = Integer.parseInt(warnRecord.getId());
                total += num;
                warnSignList.add(WarnSignOutput.builder().warnsign(WarnRecordEnum.getSignByCode(warnRecord.getWarnType())).num(num).build());
            }
        }else {
            // 查询配置文件列表
            List<SysWarnDeployDO> warnDepList = sysWarnDeployMapper.selectList(new LambdaQueryWrapper<SysWarnDeployDO>()
                .eq(SysWarnDeployDO::getIp,input.getIp())
                .eq(SysWarnDeployDO::getStatus,0));
            for(SysWarnDeployDO deploy : warnDepList){
                warnSignList.add(WarnSignOutput.builder().warnsign(WarnRecordEnum.getSignByCode(deploy.getWarnType())).num(0).build());
            }
        }
        warnRecordMap.put("data",warnSignList);
        warnRecordMap.put("total",total);
        return warnRecordMap;
    }

    @Override
    public Result<List<WarnFaultOutput>> queryFaultList(ZrWarnRecordListInput input) {
        if(StringUtils.isBlank(input.getStartTime()) || StringUtils.isBlank(input.getEndTime())){
            return ResultUtil.errorMsg("开始、结束时间不能为空");
        }
        Date start = DateUtils.stringToDate(input.getStartTime(),DateUtils.dateType5);
        Date end = DateUtils.stringToDate(input.getEndTime(),DateUtils.dateType5);
        if(start.after(end)){
            return ResultUtil.errorMsg("开始时间不得晚于结束时间");
        }
        List<WarnFaultOutput> zrWarnFaultList = this.baseMapper.queryFaultList(start,end,input.getIp());
        return ResultUtil.success(zrWarnFaultList);
    }

    @Override
    public Result<String> queryExport(IdListInput input) {
        List<ZrWarnRecordEleDO> zrWarnList = this.baseMapper.selectList(new LambdaQueryWrapper<ZrWarnRecordEleDO>().in(ZrWarnRecordEleDO::getId,input.getIdList()));
         //临时生成测试数据
        String day = DateUtils.dateToString(new Date(),DateUtils.dateType6);
        String fileName = "告警监控日志"+day+".xls";
        String headTitle = "编号,告警类型,级别,状态,告警说明,首次告警时间,更新时间,持续时间";
        List<String> headTitleList = Arrays.asList(headTitle.split(","));
        // 获取告警类型列表
        List<List<String>>  dataList = new ArrayList<List<String>>();
        for(int i=0;i<zrWarnList.size(); i++){
            // 判断告警等级
            String grade = null;
            switch (zrWarnList.get(i).getGrade()){
                case 0:grade= "一般";break;
                case 1:grade= "严重";break;
                case 2:grade= "非常严重";break;
                default:break;
            }
            // 判断告警状态
            String status = null;
            switch (zrWarnList.get(i).getStatus()){
                case 0:status= "正常";break;
                case 1:status= "异常";break;
                default:break;
            }
            List<String> datas = new ArrayList<>();
            datas.add(String.valueOf(i+1));
            datas.add(WarnRecordEnum.getSignByCode(zrWarnList.get(i).getWarnType()));
            datas.add(grade);
            datas.add(status);
            datas.add(zrWarnList.get(i).getExplain());
            datas.add(DateUtils.dateToString(zrWarnList.get(i).getCreateTime(),DateUtils.dateType5));
            datas.add(DateUtils.dateToString(zrWarnList.get(i).getUpdateTime(),DateUtils.dateType5));
            datas.add(zrWarnList.get(i).getContinuedTime());
            dataList.add(datas);
        }
        //1-创建一个HSSFWorkbook
        String path = Paths.get(Const.warnRecordPath).resolve(fileName).toString();
        try (ExcelObjectUtils excel = new ExcelObjectUtils("Sheet1")) {
            //2-写入头标题
            //3-写入行标题
            excel.createRowTitle(headTitleList, 0);
            //4-写入具体数据
            excel.createDataByRow(1, dataList);
            //5-生成excel文件
            File file = new File(Const.warnRecordPath);
            if (!file.exists()) {
                file.mkdirs();
            }
            excel.buildExcelFile(path);
        } catch (Exception e) {
            log.error("生成Excel文档失败: {}", path, e);
            return ResultUtil.errorMsg("生成Excel文档失败");
        }
        return ResultUtil.success(path);
    }

    @Override
    public Result<String> queryTimingUpdate() {
        Date dayTime = new Date();
        long pageNo = 1L;
        long pageSize = 200L;
        while (true) {
            QueryWrapper<ZrWarnRecordEleDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.isNotNull("update_time");
            queryWrapper.lt("update_time", dayTime);
            queryWrapper.orderByAsc("id");
            Page<ZrWarnRecordEleDO> page = this.baseMapper.selectPage(new Page<>(pageNo, pageSize, false), queryWrapper);
            List<ZrWarnRecordEleDO> zrWarnRecordList = page.getRecords();
            if (CollectionUtils.isEmpty(zrWarnRecordList)) {
                break;
            }
            zrWarnRecordList.forEach(zrWarnRecord -> {
                Date dateTime = zrWarnRecord.getUpdateTime();
                // update_time 理论上不应为 null，但遗留数据可能存在，防御性跳过避免 NPE
                if (dateTime == null) {
                    return;
                }
                String continuedTime = continued(dateTime, dayTime);
                this.baseMapper.update(null, new UpdateWrapper<ZrWarnRecordEleDO>()
                        .set("continued_time", continuedTime)
                        .eq("id", zrWarnRecord.getId()));
            });
            pageNo++;
        }
        return ResultUtil.success("数据更新成功");
    }

    @Override
    public Result<Integer> queryWarnNumber(String ip) {
        Date day = DateUtils.stringToDate(DateUtils.dateToString(new Date(),DateUtils.DATE_FORMAT_YYYY_MM_DD)+" 00:00:00",DateUtils.dateType5);
        Date tomorrow = DateUtil.offsetDay(day, 1);
        Integer warnCount = this.baseMapper.selectCount(new LambdaQueryWrapper<ZrWarnRecordEleDO>()
                .eq(StringUtils.isNotBlank(ip),ZrWarnRecordEleDO::getIp,ip)
                .between(ZrWarnRecordEleDO::getCreateTime,day,tomorrow));
        return ResultUtil.success(warnCount);
    }

    // 获取持续时间的方法 ,首次告警时间、结束时间
    private String continued(Date dateTime,Date dayTime){
        long durationMillis = Math.max(0L, dayTime.getTime() - dateTime.getTime());
        long totalMinutes = durationMillis / 60000L;
        long hour = totalMinutes / 60L;
        long minute = totalMinutes % 60L;
        if(hour > 0){
            return hour + "小时" + minute + "分";
        }
        return minute + "分";
    }


    // 服务器docker CPU、内存监控
    @Override
    @Async("taskExecutor")
    public void warnDockerBySchCpuMemory() {
        List<ZrDockerRecordEleDO> dockerRecordList = new ArrayList<>();
        // 添加逻辑判断 CPU、内存占用问题，超过100% 设置随机数 60% + 20%
        //  内存占用问题 超过100% 设置随机数 20% + 20%
        // 获取所有服务器IP [0-9]
        log.info(" 此时开始docker监控CPU、内存的时间为  {}",formatNow());
        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        List<String> contNameList = resolveDockerContainerNames();
        if(contNameList.isEmpty()){
            log.error(" 未获取到 docker 容器名称列表，停止本轮 CPU/内存 监控");
            return;
        }
        //todo 4.填写docker监控配置表
        log.info(" 此时开始入库docker监控记录的时间为  {}",formatNow());
        Date time = new Date();
        int i = 0; // 进行数据排序填充
        for(String contName : contNameList){
            //  docker Deploy 配置信息检查
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_cpu.getCode(), contName);
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_memory.getCode(), contName);

            //todo 判断数据库表中， 是否对此信息进行修改
            if(isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_cpu.getCode()) &&
                isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_memory.getCode())){
                log.info(" {} 容器对于 CPU、内存 监控进行关闭，停止监控",contName);
                continue;
            }
            // 获取 Docker 容器监控快照
            DockerMetricsSnapshot snapshot = dockerMetricCollectorService.getSnapshotByContainerName(contName);
            if (ObjectUtil.isNull(snapshot) || !snapshot.isAvailable()) {
                log.error("此时获取 {} 容器的 Docker 监控快照失败", contName);
                continue;
            }
            String CPU = formatDecimal(snapshot.getCpuPercent());
            String memory = formatDecimal(snapshot.getMemoryPercent());

//            log.info("此时开始数据入库------ip---{}；容器的名称为------{}；CPU的数值为---{}；内存的数值为---{}",ip,contName,CPU,memory);

            // 如果docker开始监控CPU、内存，需要修改此处代码
            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_cpu.getCode())){
                dockerRecordList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName)
                        .dataRate(CPU).type(WarnRecordEnum.docker_cpu.getCode()).unit("%").createTime(time).build());
            }
            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_memory.getCode())){
                dockerRecordList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName)
                        .dataRate(memory).type(WarnRecordEnum.docker_memory.getCode()).unit("%").createTime(time).build());
            }
        }
        log.info(" 此时docker监控 读取 CPU、内存 信息完成的时间 为  {}",formatNow());
        // docker监控记录入库
        //todo 根据数据进行筛选去重
        List<ZrDockerRecordEleDO> distinctList = dockerRecordList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(o->o.getType()+"_"+o.getDockerName()))),ArrayList::new));
        dockerRecordService.saveBatch(distinctList);

        log.info(" 此时docker监控 更新 告警监控配置表 的时间 为  {}",formatNow());
        insertWarnDockerRecord(monitorIp, distinctList);

        log.info(" 此时结束docker监控CPU、内存的时间为  {}",formatNow());
    }

    // docker IO 监控
    @Override
    @Async("taskExecutor")
    public void warnDockerIOBySch(){
        log.info(" 此时开始docker监控 磁盘IO读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrDockerRecordEleDO> dockerIOList = new ArrayList<>();

        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        List<String> contNameList = resolveDockerContainerNames();
        if(contNameList.isEmpty()){
            log.error(" 未获取到 docker 容器名称列表，停止本轮 IO 监控");
            return;
        }

        //todo 3.填写docker监控配置表
        log.info(" 此时开始入库docker 磁盘IO 监控记录的时间为  {}",formatNow());
        Date time = new Date();
        int i = 0; // 进行数据排序填充
        for(String contName : contNameList) {
//            log.info("此时 StationConst.dockerDepMap 中的数据为 {} ,",StationConst.dockerDepMap.toString());
            // docker 监控配置表更新
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_IO_read.getCode(), contName);
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_IO_write.getCode(), contName);

            //todo 判断数据库表中， 是否对此信息进行修改
            if(isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_IO_read.getCode()) &&
                    isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_IO_write.getCode())){
                log.info(" {} 容器对于 dockerIO读写 监控进行关闭，停止监控",contName);
                continue;
            }

            DockerMetricsSnapshot snapshot = dockerMetricCollectorService.getSnapshotByContainerName(contName);
            if (ObjectUtil.isNull(snapshot) || !snapshot.isAvailable()) {
                log.error("此时获取 {} 容器的 Docker IO 快照失败", contName);
                continue;
            }
            String ioReadRate = formatDecimal(snapshot.getReadKbPerSecond());
            String ioWriteRate = formatDecimal(snapshot.getWriteKbPerSecond());
            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_IO_read.getCode())){
                dockerIOList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName).type(WarnRecordEnum.docker_IO_read.getCode()).dataRate(ioReadRate).unit("KB/s").createTime(time).build());
            }
            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_IO_write.getCode())){
                dockerIOList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName).type(WarnRecordEnum.docker_IO_write.getCode()).dataRate(ioWriteRate).unit("KB/s").createTime(time).build());
            }
        }
        log.info(" 此时结束 docker监控 获取磁盘IO的时间为  {}",formatNow());

        // docker监控记录入库
        //todo 根据数据进行筛选去重
        List<ZrDockerRecordEleDO> distinctList = dockerIOList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(o->o.getType()+"_"+o.getDockerName()))),ArrayList::new));
        dockerRecordService.saveBatch(distinctList);
        insertWarnDockerRecord(monitorIp, distinctList);
        log.info(" 此时完成docker监控 磁盘IO读取一次的时间为  {}",formatNow());
    }


    // docker 网卡 监控
    @Override
    @Async("taskExecutor")
    public void warnDockerNetWorkBySch(){

        log.info(" 此时开始 docker监控 网卡读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrDockerRecordEleDO> dockerNetworkList = new ArrayList<>();

        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        List<String> contNameList = resolveDockerContainerNames();
        if(contNameList.isEmpty()){
            log.error(" 未获取到 docker 容器名称列表，停止本轮网络监控");
            return;
        }

        log.info(" 此时开始获取网卡的时间为  {}",formatNow());

        //todo 3.填写docker监控配置表
        log.info(" 此时开始入库docker监控 网卡 记录的时间为  {}",formatNow());
        Date time = new Date();
        int i = 0; // 进行数据排序填充
        for(String contName : contNameList) {
            // docker 监控配置表更新
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_network_up.getCode(), contName);
            i = dockerDepInsert(i, monitorIp, WarnRecordEnum.docker_network_down.getCode(), contName);

            //todo 判断数据库表中， 是否对此信息进行修改
            if(isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_network_up.getCode()) &&
                isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_network_down.getCode())){
                log.info(" {} 容器对于 网络出入站流量 监控进行关闭，停止监控",contName);
                continue;
            }

            DockerMetricsSnapshot snapshot = dockerMetricCollectorService.getSnapshotByContainerName(contName);
            if (ObjectUtil.isNull(snapshot) || !snapshot.isAvailable()) {
                log.error("此时获取 {} 容器的 Docker 网络快照失败", contName);
                continue;
            }
            String upRate = formatDecimal(snapshot.getNetworkUpKbPerSecond());
            String downRate = formatDecimal(snapshot.getNetworkDownKbPerSecond());

            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_network_up.getCode())){
                dockerNetworkList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName).type(WarnRecordEnum.docker_network_up.getCode()).dataRate(upRate).unit("KB/s").createTime(time).build());
            }
            if(!isDockerMonitorDisabled(contName+"_"+WarnRecordEnum.docker_network_down.getCode())){
                dockerNetworkList.add(ZrDockerRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).dockerName(contName).type(WarnRecordEnum.docker_network_down.getCode()).dataRate(downRate).unit("KB/s").createTime(time).build());
            }
        }
        log.info(" 此时结束docker监控 获取网卡的时间为  {}",formatNow());

        // docker监控记录入库
        //todo 根据数据进行筛选去重
        List<ZrDockerRecordEleDO> distinctList = dockerNetworkList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(o->o.getType()+"_"+o.getDockerName()))),ArrayList::new));
        dockerRecordService.saveBatch(distinctList);
        insertWarnDockerRecord(monitorIp, distinctList);
        log.info(" 此时完成docker监控 获取网卡的时间为  {}",formatNow());
    }


    // 新增、检验 docker告警记录信息
    @Transactional
    public void insertWarnDockerRecord(String monitorIp, List<ZrDockerRecordEleDO> dockerRecordList) {
        if (dockerRecordList == null || dockerRecordList.isEmpty()) {
            return;
        }

        List<ZrWarnRecordEleDO> allWarnRecords = this.baseMapper.selectList(
                new LambdaQueryWrapper<ZrWarnRecordEleDO>()
                        .eq(ZrWarnRecordEleDO::getIp, monitorIp)
                        .eq(ZrWarnRecordEleDO::getStatus, 0)
                        .orderByDesc(ZrWarnRecordEleDO::getCreateTime));

        Map<String, ZrWarnRecordEleDO> warnRecordMap = new HashMap<>();
        for (ZrWarnRecordEleDO record : allWarnRecords) {
            warnRecordMap.putIfAbsent(record.getWarnType(), record);
        }

        for (ZrDockerRecordEleDO dockerRecord : dockerRecordList) {
            SysWarnDeployDO warnDeploy = getOrCreateDockerWarnDeploy(dockerRecord);
            if(ObjectUtil.isNull(warnDeploy)){
                log.error(" {}  docker监控未加入 StationConst.warnDockerDepMap 中",dockerRecord.getDockerName()+"_"+dockerRecord.getType());
                continue;
            }
            // 当定时任务 - 被关闭时，不进行数据录入
            if(warnDeploy.getStatus() == 1){
                continue;
            }

            ZrWarnRecordEleDO insertWarnRecord = new ZrWarnRecordEleDO();
            insertWarnRecord.setGrade(resolveWarnGrade(dockerRecord.getDataRate(), dockerRecord.getUnit(), warnDeploy));

            // todo  告警逻辑 ； 保留最高告警等级--只增加，不降低，以便查询当天告警最高的数据 ；
            //  添加告警 状态字段，显示此告警为 异常还是正常；只有最新一条数据为异常
            ZrWarnRecordEleDO existingRecord = warnRecordMap.get(warnDeploy.getWarnType());
            if(existingRecord != null){
                Integer status = 0;
                if(ObjectUtil.isNotNull(insertWarnRecord.getGrade()) &&
                        !insertWarnRecord.getGrade().equals(existingRecord.getGrade())){
                    insertWarnRecord.setId(IdUtil.fastSimpleUUID());
                    insertWarnRecord.setWarnType(warnDeploy.getWarnType());
                    insertWarnRecord.setIp(monitorIp);
                    insertWarnRecord.setExplain(dockerRecord.getDockerName()+"容器，"+WarnRecordEnum.getExplainByCode(dockerRecord.getType())); // 告警说明
                    insertWarnRecord.setStatus(0);
                    insertWarnRecord.setCreateTime(new Date());
                    insertWarnRecord.setUpdateTime(new Date());
                    this.baseMapper.insert(insertWarnRecord);
                    // 更新旧的告警信息为 已完成状态
                    status = 1;
                }
                ZrWarnRecordEleDO updateWarnRecord = new ZrWarnRecordEleDO();
                updateWarnRecord.setId(existingRecord.getId());
                updateWarnRecord.setUpdateTime(new Date());
                updateWarnRecord.setContinuedTime(continued(existingRecord.getCreateTime(), new Date()));
                updateWarnRecord.setStatus(ObjectUtil.isNull(insertWarnRecord.getGrade()) ? 1 : status);
                this.baseMapper.updateById(updateWarnRecord);
            }
            else if(ObjectUtil.isNotNull(insertWarnRecord.getGrade())){
                insertWarnRecord.setId(IdUtil.fastSimpleUUID());
                insertWarnRecord.setWarnType(warnDeploy.getWarnType());
                insertWarnRecord.setIp(monitorIp);
                insertWarnRecord.setExplain(dockerRecord.getDockerName()+"容器，"+WarnRecordEnum.getExplainByCode(dockerRecord.getType())); // 告警说明
                insertWarnRecord.setStatus(0);
                insertWarnRecord.setCreateTime(new Date());
                insertWarnRecord.setUpdateTime(new Date());
                this.baseMapper.insert(insertWarnRecord);
            }
        }
    }


    // docker 配置信息入库
    private Integer dockerDepInsert(int i, String monitorIp, String dockerCode, String contName){
        String cacheKey = contName+"_"+dockerCode;
        if(ObjectUtil.isNull(StationConst.dockerDepMap.get(cacheKey))){
            synchronized (dockerDeployLoadLock) {
                // 双重检查保证只有第一个线程真正执行查库和插库。
                SysDockerDeployEleDO cachedDeploy = StationConst.dockerDepMap.get(cacheKey);
                if (ObjectUtil.isNotNull(cachedDeploy)) {
                    return i;
                }
                List<SysDockerDeployEleDO> dockerDepList = dockerDeployMapper.selectList(new LambdaQueryWrapper<SysDockerDeployEleDO>()
                        .eq(SysDockerDeployEleDO::getIp, monitorIp).eq(SysDockerDeployEleDO::getDockerName,contName)
                        .eq(SysDockerDeployEleDO::getType,dockerCode).orderByDesc(SysDockerDeployEleDO::getSort));
                //todo 容器填写默认排序
                if(dockerDepList.isEmpty()){
                    SysDockerDeployEleDO dockerDeployEle = SysDockerDeployEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp)
                            .nodeName("气象中心").taskName(contName).dockerName(contName).isShow(0).type(dockerCode).sort(i).createTime(new Date()).build();
                    dockerDeployMapper.insert(dockerDeployEle);
                    StationConst.dockerDepMap.put(cacheKey,dockerDeployEle);
                }else {
                    i = dockerDepList.get(0).getSort() + 1;
                    StationConst.dockerDepMap.put(cacheKey,dockerDepList.get(0));
                }
                i++;
            }
        }
        return i;
    }

    // 定时读取Linux CPU 的任务
    @Override
    @Async("taskExecutor")
    public void warnLinuxCPUBySch(){
        log.info(" 此时开始CPU读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
        //新增监控数据的实体类
        String linuxCpuCode = WarnRecordEnum.linux_cpu.getCode();

        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        log.info(" 此时开始获取CPU的时间为  {}",formatNow());
        //todo 2.获取Linux系统-CPU的数据
        String procStatFile = Paths.get(Const.cpumoryIOUrl).resolve("stat").toString();
        if(!FileUtil.exist(procStatFile)){
            log.error("linux 监控 {} CPU配置文件为空或者不存在此文件",procStatFile);
            return;
        }
        String CPU = procStatCPU(procStatFile);
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(linuxCpuCode).dataRate(CPU).unit("%").createTime(new Date()).build());
        log.info(" 此时结束获取CPU的时间为  {}",formatNow());

        checklinuxDepRecord(monitorIp, linuxCpuCode,linuxRecordList);
        log.info(" 此时完成CPU读取一次的时间为  {}",formatNow());
    }

    // 定时读取Linux 内存 的任务
    @Override
    @Async("taskExecutor")
    public void warnLinuxMemoryBySch(){
        log.info(" 此时开始内存读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
        //新增监控数据的实体类
        String linuxMemoryCode = WarnRecordEnum.linux_memory.getCode();

//        log.info(" 此时开始获取网卡地址的时间为  {}",formatter.format(new Date()));
        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

//        log.info(" 此时开始获取内存的时间为  {}",formatter.format(new Date()));
        //todo 2.获取Linux系统-内存的数据
        String procMemoryFile = Paths.get(Const.cpumoryIOUrl).resolve("meminfo").toString();
        if(!FileUtil.exist(procMemoryFile)){
            log.error("linux 监控 {} 内存配置文件为空或者不存在此文件",procMemoryFile);
            return;
        }
        String memory = procMemory(procMemoryFile);
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(linuxMemoryCode).dataRate(memory).unit("%").createTime(new Date()).build());
//        log.info(" 此时结束获取内存的时间为  {}",formatter.format(new Date()));

        checklinuxDepRecord(monitorIp, linuxMemoryCode,linuxRecordList);
        log.info(" 此时完成内存读取一次的时间为  {}",formatNow());
    }

    // 定时读取Linux 磁盘 的任务
    @Override
    @Async("taskExecutor")
    public void warnLinuxDiskBySch(){
        log.info(" 此时开始磁盘读取一次的时间为  {}",formatNow());

        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }
//        log.info(" 此时开始获取磁盘的时间为  {}",formatter.format(new Date()));
        //todo 2.获取Linux系统-磁盘的数据
        List<String> diskPathList = new ArrayList<>();
        if(StringUtils.isNotBlank(Const.diskPaths)){
            diskPathList = Arrays.asList(Const.diskPaths.split(","));
            for(String diskPath : diskPathList){
                //新增告警日志的实体类
                List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
                String diskData = CommandUtil.diskCmd(new String[]{"sh", "-c","df -h | grep "+diskPath+" | awk -F '[ %]+' '{print $5}'"});
                if(StringUtils.isBlank(diskData)){
                    log.error(" 此时输入的地址 {} 不存在，请更改地址 ",diskPath);
                    continue;
                }
                String diskType = WarnRecordEnum.linux_disk.getCode()+"_"+diskPath;
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(diskType).dataRate(new BigDecimal(diskData)
                        .setScale(2,BigDecimal.ROUND_HALF_UP).toString()).unit("%").createTime(new Date()).build());
                // 监测linux监控信息，并入库
                checklinuxDepRecord(monitorIp, diskType,linuxRecordList);
            }
        }
        log.info(" 此时完成磁盘读取一次的时间为  {}",formatNow());
    }

    // 定时读取Linux IO 的任务
    @Override
    @Async("taskExecutor")
    public void warnLinuxIOBySch(){
        log.info(" 此时开始磁盘IO读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxIOReadList = new ArrayList<>();
        List<ZrLinuxRecordEleDO> linuxIOWriteList = new ArrayList<>();

//        log.info(" 此时开始获取网卡地址的时间为  {}",formatter.format(new Date()));
        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

//        log.info(" 此时开始获取内存的时间为  {}",formatter.format(new Date()));
        //todo 2.获取Linux系统-IO磁盘成功 读、写 的总速率
        String procDiskFile = Paths.get(Const.cpumoryIOUrl).resolve("diskstats").toString();
        if(!FileUtil.exist(procDiskFile)){
            log.error("linux 监控 {} IO配置文件为空或者不存在此文件",procDiskFile);
            return;
        }
        Map<String,String> IOMap = procDiskIO(procDiskFile);
        String[] IORead = IOMap.get("read").split("\\s+");
        String[] IOWrite = IOMap.get("write").split("\\s+");
        linuxIOReadList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(WarnRecordEnum.linux_IO_read.getCode()).dataRate(IORead[0]).unit(IORead[1]).createTime(new Date()).build());
        linuxIOWriteList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(WarnRecordEnum.linux_IO_write.getCode()).dataRate(IOWrite[0]).unit(IOWrite[1]).createTime(new Date()).build());
//        log.info(" 此时结束获取内存的时间为  {}",formatter.format(new Date()));

        checklinuxDepRecord(monitorIp, WarnRecordEnum.linux_IO_read.getCode(),linuxIOReadList);
        checklinuxDepRecord(monitorIp, WarnRecordEnum.linux_IO_write.getCode(),linuxIOWriteList);
        log.info(" 此时完成磁盘IO读取一次的时间为  {}",formatNow());
    }

    // 定时读取Linux 网卡 的任务
    @Override
    @Async("taskExecutor")
    public void warnLinuxNetWorkBySch(){
        log.info(" 此时开始网卡读取一次的时间为  {}",formatNow());

        String monitorIp = resolveLinuxMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }
        if(StringUtils.isBlank(Const.networkCardsUrl)){
            log.error(" 网卡路径配置不能为空 datacenter.monitor.network.cards ");
            return;
        }

        //新增告警监控日志的实体类
        List<ZrLinuxRecordEleDO> linuxNetworkUpList = new ArrayList<>();
        List<ZrLinuxRecordEleDO> linuxNetworkDownList = new ArrayList<>();
        String networkUpCode = WarnRecordEnum.linux_network_up.getCode();
        String networkDownCode = WarnRecordEnum.linux_network_down.getCode();

//        log.info(" 此时开始获取网卡的时间为  {}",formatter.format(new Date()));
        //todo 2.获取Linux系统-网卡出站、入站的数据
        /*
         *  /sys/class/net/ens7f0/statistics/rx_bytes：这个文件包含了网络接口接收的总字节数
         *  /sys/class/net/ens7f0/statistics/tx_bytes：这个文件包含了网络接口发送的总字节数
         */
        //todo 3.检查物理网卡列表是否为空，否则，获取网卡列表
        List<String> networkList = resolveLinuxNetworkList();
        if(networkList.isEmpty()){
            log.error(" 获取物理网卡列表失败 ");
            return;
        }

        //todo 4. 第一次采样，获取首次网络出入站速率
        Map<String, Long> rxBefore = new HashMap<>();    // 入站速率
        Map<String, Long> txBefore = new HashMap<>();    // 出站速率
        for (String iface : networkList) {
            rxBefore.put(iface, readNetwork(Paths.get(Const.networkCardsUrl).resolve(iface).resolve("statistics/rx_bytes").toString()));
            txBefore.put(iface, readNetwork(Paths.get(Const.networkCardsUrl).resolve(iface).resolve("statistics/tx_bytes").toString()));
        }

        try {
            Thread.sleep(1000); // 1 秒采样间隔
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Linux网络采样被中断: {}", monitorIp, e);
            return;
        }

        //todo 5. 第二次采样，计算网络出入站总速率
        long totalRx = 0;  // 总入站速率
        long totalTx = 0;  // 总出站速率

        for (String iface : networkList) {
            long rxAfter = readNetwork(Paths.get(Const.networkCardsUrl).resolve(iface).resolve("statistics/rx_bytes").toString());
            long txAfter = readNetwork(Paths.get(Const.networkCardsUrl).resolve(iface).resolve("statistics/tx_bytes").toString());

            long rxRate = rxAfter - rxBefore.getOrDefault(iface, 0L);
            long txRate = txAfter - txBefore.getOrDefault(iface, 0L);

            totalRx += rxRate;
            totalTx += txRate;
        }

        String[] up = formatRate(totalTx).split("\\s+");
        String[] down = formatRate(totalRx).split("\\s+");
        linuxNetworkUpList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(networkUpCode).dataRate(up[0]).unit(up[1]).createTime(new Date()).build());
        linuxNetworkDownList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(networkDownCode).dataRate(down[0]).unit(down[1]).createTime(new Date()).build());

        // 监测linux监控信息，并入库
        checklinuxDepRecord(monitorIp, networkUpCode,linuxNetworkUpList);
        checklinuxDepRecord(monitorIp, networkDownCode,linuxNetworkDownList);

        log.info(" 此时完成网卡读取一次的时间为  {}",formatNow());
    }

    
    // 读取CPU的配置信息
    private String procStatCPU(String procStatFile){
         try {
             String[] firstCpuData = readCpuSnapshot(procStatFile);
             if (firstCpuData.length < 5) {
                 return "0.00";
             }
             long prevTotal = Long.parseLong(firstCpuData[1]) + Long.parseLong(firstCpuData[2]) + Long.parseLong(firstCpuData[3]) + Long.parseLong(firstCpuData[4]);
             long prevIdle = Long.parseLong(firstCpuData[4]);

             // 等待一段时间
             Thread.sleep(1000);

             String[] secondCpuData = readCpuSnapshot(procStatFile);
             if (secondCpuData.length < 5) {
                 return "0.00";
             }
             long total = Long.parseLong(secondCpuData[1]) + Long.parseLong(secondCpuData[2]) + Long.parseLong(secondCpuData[3]) + Long.parseLong(secondCpuData[4]);
             long idle = Long.parseLong(secondCpuData[4]);

             // 计算差值
             long totalDiff = total - prevTotal;
             long idleDiff = idle - prevIdle;
             if (totalDiff <= 0) {
                 return "0.00";
             }

             // 计算使用率
             double usage = ((double) (totalDiff - idleDiff) / totalDiff) * 100;
             return new BigDecimal(usage).setScale(2,BigDecimal.ROUND_HALF_UP).toString();
         } catch (NumberFormatException | IOException e) {
             log.error("CPU 硬件监控文件读取失败: {}", procStatFile, e);
         } catch (InterruptedException e) {
             Thread.currentThread().interrupt(); // 恢复中断状态
             log.error("当前服务器运行报错: {}", procStatFile, e);
         }
         return "0.00";
     }

    // 读取内存的配置信息
    private String procMemory(String procMemoryFile){
        long totalMem = 0;
        long freeMem = 0;
        long buffersMem = 0;
        long cachedMem = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procMemoryFile), StationConst.SystemCode))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    totalMem = Long.parseLong(line.split("\\s+")[1]) * 1024; // 转换成字节
                } else if (line.startsWith("MemFree:")) {
                    freeMem = Long.parseLong(line.split("\\s+")[1]) * 1024; // 转换成字节
                } else if (line.startsWith("Buffers:")) {
                    buffersMem = Long.parseLong(line.split("\\s+")[1]) * 1024; // 转换成字节
                } else if (line.startsWith("Cached:")) {
                    cachedMem = Long.parseLong(line.split("\\s+")[1]) * 1024; // 转换成字节
                }
            }
        } catch (NumberFormatException | IOException e) {
            log.error("内存配置文件读取失败: {}", procMemoryFile, e);
            return "0.00";
        }
        if (totalMem <= 0) {
            log.warn("内存 配置文件======【 {} 】缺少 MemTotal，返回默认值========", procMemoryFile);
            return "0.00";
        }

        // 计算已用内存
        long usedMem = totalMem - freeMem - buffersMem - cachedMem;

        // 计算内存使用率
        double memUsage = (double) usedMem / totalMem * 100;
        return new BigDecimal(memUsage).setScale(2,BigDecimal.ROUND_HALF_UP).toString();
    }

    // 读取网卡配置的IP信息
    public String networkIP(String ifcfgFile){
        String ipAddress = "";
        if(StringUtils.isBlank(Const.networkipUrl)){
            log.error("linux 监控 datacenter.monitor.network.ip IP配置不能为空");
            return ipAddress;
        }
        // 直接返回IP
        if(ipPattern.matcher(Const.networkipUrl).matches()){
            ipAddress = Const.networkipUrl;
            return ipAddress;
        }
        if(!FileUtil.exist(Const.networkipUrl)){
            log.error("linux 监控 datacenter.monitor.network.ip IP路径配置错误");
            return ipAddress;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ifcfgFile), StationConst.SystemCode))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("IPADDR")) {
                    ipAddress = line.split("=")[1];
                    break;
                }
            }
        } catch (IOException e) {
            log.error("内网IP配置文件读取失败: {}", ifcfgFile, e);
        }
        return ipAddress;
    }

    /*
     * 读取指定磁盘的 IO读写速率
     * 读取 /proc/diskstats 文件中 指定盘符的 扇区数
     * 将扇区数转换为KB，假设每个扇区为512字节（0.5KB）
     *
     * /proc/diskstats文件的格式可能因内核版本而异，所以确保解析逻辑与你的系统上的文件格式匹配。
     * 此外，磁盘的行可能不仅包含"sd"开头的设备，还可能包含其他类型的存储设备，如NVMe设备等。
     * 根据你的需求，你可能需要调整代码以包括或排除某些类型的设备。
     * CentOS7.9 系统 第一行为磁盘总数据
     */
    private Map<String,String> procDiskIO(String procDiskFile) {
        Map<String,String> IOMap = createDefaultIoRateMap();
        try {
            long[] firstSnapshot = readDiskStatsSnapshot(procDiskFile);

            // 等待一段时间，1s
            Thread.sleep(1000);

            long[] secondSnapshot = readDiskStatsSnapshot(procDiskFile);
            String readRate = formatRate(calculateIORate(firstSnapshot[0], secondSnapshot[0]));
            String writeRate = formatRate(calculateIORate(firstSnapshot[1], secondSnapshot[1]));
            IOMap.put("read",readRate);
            IOMap.put("write",writeRate);
        } catch (IOException e) {
            log.error("磁盘IO硬件监控文件读取失败: {}", procDiskFile, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("当前服务器运行报错: {}", procDiskFile, e);
        }
        return IOMap;
    }

    private long[] readDiskStatsSnapshot(String procDiskFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procDiskFile), StationConst.SystemCode))) {
            String line = reader.readLine();
            if (StringUtils.isBlank(line)) {
                return new long[]{0L, 0L};
            }
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length <= 7) {
                return new long[]{0L, 0L};
            }
            return new long[]{Long.parseLong(tokens[3]), Long.parseLong(tokens[7])};
        }
    }

    private String[] readCpuSnapshot(String procStatFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(procStatFile), StationConst.SystemCode))) {
            String cpuLine = reader.readLine();
            if (StringUtils.isBlank(cpuLine)) {
                return new String[0];
            }
            return cpuLine.split("\\s+");
        }
    }

    private Map<String,String> createDefaultIoRateMap() {
        Map<String,String> ioMap = new HashMap<>();
        ioMap.put("read", formatRate(0));
        ioMap.put("write", formatRate(0));
        return ioMap;
    }

    // 读取网卡 出入站速率文件
    private static long readNetwork(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path),StationConst.SystemCode))) {
            return Long.parseLong(reader.readLine().trim());
        } catch (Exception e) {
            return 0L; // 出错返回 0
        }
    }

    // 核查linux配置信息，并入库
    private void checklinuxDepRecord(String monitorIp, String linuxCode,List<ZrLinuxRecordEleDO> linuxRecordList){
//        log.info(" 此时写入配置linux配置表的时间为  {}",formatter.format(new Date()));
        //todo 3.更新Linux配置表的数据
        insertlinuxDepMap(monitorIp, linuxCode);

        // 判断linux记录不为空
        if(!linuxRecordList.isEmpty()){
//            log.info(" 此时写入linux记录表的时间为  {}",formatter.format(new Date()));
            //todo 4.更新linux记录表信息
            linuxRecordSave(linuxRecordList);
//            log.info(" 此时写入warn告警监控的时间为  {}",formatter.format(new Date()));

            // 暂时不监控 磁盘IO、网卡流量; 因为它们的单位是活的(KB/s,MB/s)，不好操控，后期固定单位之后，再进行更改
//            String linuxIO = WarnRecordEnum.linux_IO_read.getCode().split("_")[0]+"_"+WarnRecordEnum.linux_IO_read.getCode().split("_")[1];
//            String linuxNetWorkIf =  WarnRecordEnum.linux_network_up.getCode().split("_")[0]+"_"+WarnRecordEnum.linux_network_up.getCode().split("_")[1];
//            String winIO = WarnRecordEnum.win_IO_read.getCode().split("_")[0]+"_"+WarnRecordEnum.win_IO_read.getCode().split("_")[1];
//            String winNetWorkIf =  WarnRecordEnum.win_network_up.getCode().split("_")[0]+"_"+WarnRecordEnum.win_network_up.getCode().split("_")[1];
//            if (!linuxCode.contains(linuxIO) && !linuxCode.contains(linuxNetWorkIf) && !linuxCode.contains(winIO) && !linuxCode.contains(winNetWorkIf)) {
                //todo 5.更新告警监控配置表信息
                insertWarnLinuxDepMap(monitorIp, linuxRecordList);
//            log.info(" 此时写入warn监控记录的时间为  {}",formatter.format(new Date()));
                //todo 6.更新告警记录表信息
                insertWarnLinuxRecord(monitorIp, linuxRecordList);
//            }
        }
    }


    // 新增linux 配置表信息
    private void insertlinuxDepMap(String monitorIp, String linuxCode){
        if(ObjectUtil.isNull(StationConst.linuxDepMap.get(linuxCode))){
            synchronized (linuxDeployLoadLock) {
                // 串行化首次装载，避免多个异步任务同时发现“没配置”后重复插库。
                if (ObjectUtil.isNotNull(StationConst.linuxDepMap.get(linuxCode))) {
                    return;
                }
                // 检查数据库是否存在此数据
                List<SysLinuxDeployDO> linuxDepList = linuxDeployMapper.selectList(new LambdaQueryWrapper<SysLinuxDeployDO>()
                        .eq(SysLinuxDeployDO::getIp, monitorIp).eq(SysLinuxDeployDO::getType,linuxCode));
                if(linuxDepList.isEmpty()){
            SysLinuxDeployDO sysLinuxDep = SysLinuxDeployDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(linuxCode)
                            .nodeName("气象中心").ipName(monitorIp).createTime(new Date()).sort(0).isShow(0).build();
                    StationConst.linuxDepMap.put(linuxCode,sysLinuxDep);
                    linuxDeployMapper.insert(sysLinuxDep);
                }else {
                    StationConst.linuxDepMap.put(linuxCode,linuxDepList.get(0));
                }
            }
        }
    }
    // 新增linux 记录表信息
    private void linuxRecordSave(List<ZrLinuxRecordEleDO> linuxRecordList){
        //工具方法，获得浮点数的字符串表示，整数，或保留两位小数
        zrLinuxRecordService.saveBatch(linuxRecordList);
    }

    // todo 告警监控，不监控 磁盘IO、网卡流量
    // 判断 告警监控表是否存在硬件监控
    private void insertWarnLinuxDepMap(String monitorIp, List<ZrLinuxRecordEleDO> linuxRecordList){
        for (ZrLinuxRecordEleDO linuxRecord : linuxRecordList){
            // 统一走懒加载收口逻辑，避免不同入口各自查库、各自插库。
            getOrCreateWarnDeploy(monitorIp, linuxRecord.getType(), linuxRecord.getType(), true);
        }
    }

    // 新增、检验 linux告警记录信息
    @Transactional
    public void insertWarnLinuxRecord(String monitorIp, List<ZrLinuxRecordEleDO> linuxRecordList){
        if (linuxRecordList == null || linuxRecordList.isEmpty()) {
            return;
        }

        // 【优化】一次性批量查询所有该IP下status=0的告警记录，按warnType分组，消除N+1查询
        List<ZrWarnRecordEleDO> allWarnRecords = this.baseMapper.selectList(
                new LambdaQueryWrapper<ZrWarnRecordEleDO>()
                        .eq(ZrWarnRecordEleDO::getIp, monitorIp)
                        .eq(ZrWarnRecordEleDO::getStatus, 0)
                        .orderByDesc(ZrWarnRecordEleDO::getCreateTime));

        // 按warnType分组，每个type只保留最新一条（orderByDesc保证第一条即最新）
        Map<String, ZrWarnRecordEleDO> warnRecordMap = new HashMap<>();
        for (ZrWarnRecordEleDO record : allWarnRecords) {
            warnRecordMap.putIfAbsent(record.getWarnType(), record);
        }

        for (ZrLinuxRecordEleDO linuxRecord : linuxRecordList) {
            ZrWarnRecordEleDO insertWarnRecord = new ZrWarnRecordEleDO();

            if(ObjectUtil.isNull(StationConst.warnDepMap.get(linuxRecord.getType()))){
                continue;
            }
            SysWarnDeployDO warnDeploy = StationConst.warnDepMap.get(linuxRecord.getType());
            // 如果 数据状态为 禁用，则不进行记录入库
            if(warnDeploy.getStatus() == 1){
                continue;
            }

            insertWarnRecord.setGrade(resolveWarnGrade(linuxRecord.getDataRate(), linuxRecord.getUnit(), warnDeploy));

            // todo  告警逻辑 ； 保留最高告警等级--只增加，不降低，以便查询当天告警最高的数据 ；
            //  添加告警 状态字段，显示此告警为 异常还是正常；只有最新一条数据为异常
            String explain = "";
            if(warnDeploy.getWarnType().contains(WarnRecordEnum.linux_disk.getCode())){
                explain = WarnRecordEnum.linux_disk.getExplain();
            }else {
                explain = WarnRecordEnum.getExplainByCode(warnDeploy.getWarnType());
            }
            // 【优化】从预加载的Map中取，不再每次查库
            ZrWarnRecordEleDO existingRecord = warnRecordMap.get(warnDeploy.getWarnType());

            if(existingRecord != null){
                Integer status = 0;
                if(ObjectUtil.isNotNull(insertWarnRecord.getGrade()) &&
                        !insertWarnRecord.getGrade().equals(existingRecord.getGrade())){
                    insertWarnRecord.setId(IdUtil.fastSimpleUUID());
                    insertWarnRecord.setWarnType(warnDeploy.getWarnType());
                    insertWarnRecord.setIp(monitorIp);
                    insertWarnRecord.setExplain(explain); // 告警说明
                    insertWarnRecord.setStatus(0);
                    insertWarnRecord.setCreateTime(new Date());
                    insertWarnRecord.setUpdateTime(new Date());
                    int i = this.baseMapper.insert(insertWarnRecord);
                    // 更新旧的告警信息为 已完成状态
                    status = 1;
                }
                ZrWarnRecordEleDO updateWarnRecord = new ZrWarnRecordEleDO();
                updateWarnRecord.setId(existingRecord.getId());
                updateWarnRecord.setUpdateTime(new Date());
                updateWarnRecord.setContinuedTime(continued(existingRecord.getCreateTime(), new Date()));
                updateWarnRecord.setStatus(ObjectUtil.isNull(insertWarnRecord.getGrade()) ? 1 : status);
                int i = this.baseMapper.updateById(updateWarnRecord);
            }
            else if(ObjectUtil.isNotNull(insertWarnRecord.getGrade())){
                insertWarnRecord.setId(IdUtil.fastSimpleUUID());
                insertWarnRecord.setWarnType(warnDeploy.getWarnType());
                insertWarnRecord.setIp(monitorIp);
                insertWarnRecord.setExplain(explain); // 告警说明
                insertWarnRecord.setStatus(0);
                insertWarnRecord.setCreateTime(new Date());
                insertWarnRecord.setUpdateTime(new Date());
                int i = this.baseMapper.insert(insertWarnRecord);
            }
        }

    }


    @Override
    @Async("taskExecutor")
    public void warnWinCPUBySch() {
        log.info(" 此时开始CPU读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
        //新增监控数据的实体类
        String winCpuCode = WarnRecordEnum.win_cpu.getCode();

        String monitorIp = resolveWindowsMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        //todo 2.首次获取 CPU 利用率
        CentralProcessor processor = hal.getProcessor();
        // 第一次采样：获取 tick 快照
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        // 等待一段时间（如 1 秒）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Linux网络采样被中断: {}", monitorIp, e);
            return;
        }

        //todo 3. 第二次调用，传入第一次的快照，再次获取 CPU 利用率
        String cpuLoad = new BigDecimal(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100).setScale(2,BigDecimal.ROUND_HALF_UP).toString();

        //todo 4.将CPU的信息记录入库
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(winCpuCode).dataRate(cpuLoad).unit("%").createTime(new Date()).build());
//        log.info(" 此时写入配置linux配置表的时间为  {}",formatter.format(new Date()));
        //todo 5.将CPU进行告警监控
        checklinuxDepRecord(monitorIp, winCpuCode,linuxRecordList);
        log.info(" 此时完成CPU读取一次的时间为  {}",formatNow());
    }

    // 2. 内存使用率
    @Override
    @Async("taskExecutor")
    public void warnWinMemoryBySch() {
        log.info(" 此时开始内存读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
        //新增监控数据的实体类
        String winMemoryCode = WarnRecordEnum.win_memory.getCode();

        String monitorIp = resolveWindowsMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }
        //todo 2.获取内存利用率
        GlobalMemory memory = hal.getMemory();
        String memData = new BigDecimal((memory.getTotal() - memory.getAvailable()) * 100.0 / memory.getTotal()).setScale(2,BigDecimal.ROUND_HALF_UP).toString();

        //todo 3.内存利用率入库
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(winMemoryCode).dataRate(memData).unit("%").createTime(new Date()).build());
//        log.info(" 此时结束获取内存的时间为  {}",formatter.format(new Date()));

        //todo 4.内存利用率 进行告警监控
        checklinuxDepRecord(monitorIp, winMemoryCode,linuxRecordList);
        log.info(" 此时完成内存读取一次的时间为  {}",formatNow());
    }

    // 3. 获取指定目录占用百分比
    @Override
    @Async("taskExecutor")
    public void warnWinDiskBySch() {
        log.info(" 此时开始磁盘读取一次的时间为  {}",formatNow());

        String monitorIp = resolveWindowsMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        //todo 2.获取所有的挂载磁盘
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        List<String> diskPathList = new ArrayList<>();

        if(StringUtils.isNotBlank(Const.diskPaths)) {
            diskPathList = Arrays.asList(Const.diskPaths.split(","));

            for (String pathStr : diskPathList) {
                //新增监控日志的实体类
                List<ZrLinuxRecordEleDO> linuxRecordList = new ArrayList<>();
                File file = new File(pathStr);

                if (!file.exists()) {
                    log.error(" 此时输入的地址 {} 不存在，请更改地址 ",pathStr);
                    continue;
                }

                //todo 3.找到该路径所在的挂载点磁盘
                OSFileStore matchedStore = null;
                for (OSFileStore store : fileStores) {
                    String mount = store.getMount();
                    if (file.getAbsolutePath().toLowerCase().startsWith(mount.toLowerCase())) {
                        if (matchedStore == null || mount.length() > matchedStore.getMount().length()) {
                            matchedStore = store;
                        }
                    }
                }

                if (matchedStore != null) {
                    long total = matchedStore.getTotalSpace();
                    long usable = matchedStore.getUsableSpace();
                    long used = total - usable;

                    String usedPercent = total > 0 ? new BigDecimal((used * 100.0) / total).setScale(2,BigDecimal.ROUND_HALF_UP).toString() : "0";
                    String diskType = WarnRecordEnum.win_disk.getCode()+"_"+pathStr;

                    //todo 4.磁盘路径监控信息入库
        linuxRecordList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(diskType).dataRate(usedPercent).unit("%").createTime(new Date()).build());

                    //todo 5.磁盘路径信息进行告警监控
                    checklinuxDepRecord(monitorIp, diskType,linuxRecordList);
                } else {
                    log.error(" 未找到 {} 所在的磁盘挂载点 " + pathStr);
                }
            }
        }
        log.info(" 此时完成磁盘读取一次的时间为  {}",formatNow());
    }

    // 电脑IO读写总占比
    @Override
    @Async("taskExecutor")
    public void warnWinIOBySch() {
        log.info(" 此时开始磁盘IO读取一次的时间为  {}",formatNow());
        //新增告警日志的实体类
        List<ZrLinuxRecordEleDO> linuxIOReadList = new ArrayList<>();
        List<ZrLinuxRecordEleDO> linuxIOWriteList = new ArrayList<>();

        String monitorIp = resolveWindowsMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        // 获取所有磁盘
        List<HWDiskStore> diskStores = hal.getDiskStores();

        // 初始化前一次读写字节数
        long totalReadBytesBefore = 0;
        long totalWriteBytesBefore = 0;

        //todo 2.初次记录 电脑IO 的总读写率
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            totalReadBytesBefore += disk.getReadBytes();
            totalWriteBytesBefore += disk.getWriteBytes();
        }

        // 等待 1 秒采样间隔
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Linux网络采样被中断: {}", monitorIp, e);
            return;
        }

        //todo 3.再次记录 电脑IO读写 的总读写率
        long totalReadBytesAfter = 0;
        long totalWriteBytesAfter = 0;

        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            totalReadBytesAfter += disk.getReadBytes();
            totalWriteBytesAfter += disk.getWriteBytes();
        }

        // 计算每秒读写速率（默认单位：KB/s）
        String[] IORead = formatRate(totalReadBytesAfter - totalReadBytesBefore).split("\\s+");
        String[] IOWrite = formatRate(totalWriteBytesAfter - totalWriteBytesBefore).split("\\s+");

        //todo 4.电脑IO 总读写率入库
        linuxIOReadList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(WarnRecordEnum.win_IO_read.getCode()).dataRate(IORead[0]).unit(IORead[1]).createTime(new Date()).build());
        linuxIOWriteList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(WarnRecordEnum.win_IO_write.getCode()).dataRate(IOWrite[0]).unit(IOWrite[1]).createTime(new Date()).build());
//        log.info(" 此时结束获取内存的时间为  {}",formatter.format(new Date()));

        //todo 5.电脑IO 总读写率进行告警监控
        checklinuxDepRecord(monitorIp, WarnRecordEnum.win_IO_read.getCode(),linuxIOReadList);
        checklinuxDepRecord(monitorIp, WarnRecordEnum.win_IO_write.getCode(),linuxIOWriteList);
        log.info(" 此时完成磁盘IO读取一次的时间为  {}",formatNow());

    }

    // 电脑 网络出入站总占比
    @Override
    @Async("taskExecutor")
    public void warnWinNetWorkBySch() {
        log.info(" 此时开始网卡读取一次的时间为  {}",formatNow());
        List<ZrLinuxRecordEleDO> linuxNetworkUpList = new ArrayList<>();
        List<ZrLinuxRecordEleDO> linuxNetworkDownList = new ArrayList<>();

        String networkUpCode = WarnRecordEnum.win_network_up.getCode();
        String networkDownCode = WarnRecordEnum.win_network_down.getCode();

        String monitorIp = resolveWindowsMonitorIp();
        if(StringUtils.isBlank(monitorIp)){
            return;
        }

        // 获取所有的网卡
        List<NetworkIF> networkIFs = hal.getNetworkIFs();

        //todo 2.首次记录 电脑网卡 的总出入站速率
        long totalRecvBefore = 0;
        long totalSentBefore = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // 必须先刷新
            totalRecvBefore += net.getBytesRecv(); // 入站速率
            totalSentBefore += net.getBytesSent(); // 出站速率
        }

        // 等待 1 秒采样间隔
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Linux网络采样被中断: {}", monitorIp, e);
            return;
        }

        //todo 3.再次记录 电脑网卡 的总出入站速率
        long totalRecvAfter = 0;
        long totalSentAfter = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            totalRecvAfter += net.getBytesRecv(); // 入站速率
            totalSentAfter += net.getBytesSent(); // 出站速率
        }

        // 计算差值（默认单位：KB/s）
        String[] up = formatRate(totalSentAfter - totalSentBefore).split("\\s+");
        String[] down = formatRate(totalRecvAfter - totalRecvBefore).split("\\s+");

        //todo 4.电脑网卡出入站速率入库
        linuxNetworkUpList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(networkUpCode).dataRate(up[0]).unit(up[1]).createTime(new Date()).build());
        linuxNetworkDownList.add(ZrLinuxRecordEleDO.builder().id(IdUtil.fastSimpleUUID()).ip(monitorIp).type(networkDownCode).dataRate(down[0]).unit(down[1]).createTime(new Date()).build());

        //todo 5.电脑网卡出入站速率 进行告警监控
        checklinuxDepRecord(monitorIp, networkUpCode,linuxNetworkUpList);
        checklinuxDepRecord(monitorIp, networkDownCode,linuxNetworkDownList);

        log.info(" 此时完成网卡读取一次的时间为  {}",formatNow());
    }

    // 读取windows系统配置的IP信息
    private String windowsIP(){
        String ipAddress = null;
        //todo 判断 Nacos 配置IP是否为 网卡IP ；直接返回IP
        if(ipPattern.matcher(Const.networkipUrl).matches()){
            ipAddress = Const.networkipUrl;
            return ipAddress;
        }

        // 获取所有网络接口
        List<NetworkIF> networkInterfaces = hal.getNetworkIFs();

        // 遍历网络接口，查找内网 IP 地址
        for (NetworkIF networkInterface : networkInterfaces) {
            // 获取 IPv4 地址列表
            String[] ipv4Addresses = networkInterface.getIPv4addr();

            // 遍历 IPv4 地址，找到内网 IP
            for (String ip : ipv4Addresses) {
                if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                    ipAddress = ip;
                }
            }
        }
        return ipAddress;
    }

    SysWarnDeployDO getOrCreateDockerWarnDeploy(ZrDockerRecordEleDO dockerRecord) {
        String monitorIp = resolveMonitorIp(dockerRecord.getIp());
        String warnDockerCode = buildDockerWarnType(dockerRecord.getDockerName(), dockerRecord.getType());
        return getOrCreateWarnDeploy(monitorIp, warnDockerCode, warnDockerCode, true);
    }

    private String buildDockerWarnType(String dockerName, String type) {
        return dockerName + "_" + type;
    }

    private String resolveLinuxMonitorIp() {
        return StationConst.getOrLoadLinuxIp(() -> networkIP(Const.networkipUrl));
    }

    private String resolveWindowsMonitorIp() {
        return StationConst.getOrLoadLinuxIp(this::windowsIP);
    }

    private List<String> resolveDockerContainerNames() {
        List<String> containerNames = dockerMetricCollectorService.listRunningContainerNames();
        if (containerNames.isEmpty()) {
            return Collections.emptyList();
        }
        String contNames = String.join(",", containerNames);
        StationConst.refreshContNames(contNames);
        return containerNames;
    }

    private String loadDockerContNames() {
        // 容器列表统一通过 docker-java 获取，避免继续依赖 shell 结果格式。
        String contNames = String.join(",", dockerMetricCollectorService.listRunningContainerNames());
        log.info("=========此时通过 docker-java 获取到的容器名称列表为 {} =====", contNames);
        return contNames;
    }

    private List<String> resolveLinuxNetworkList() {
        if (!StationConst.netWorkList.isEmpty()) {
            return StationConst.netWorkList;
        }
        synchronized (linuxNetworkLoadLock) {
            if (!StationConst.netWorkList.isEmpty()) {
                return StationConst.netWorkList;
            }
            String netWorkStr = CommandUtil.containerCmd(new String[]{"sh", "-c","ls -l /sys/class/net | grep '/pci' | awk '{print $9}'"});
            List<String> latestNetworkList = splitCommaText(netWorkStr);
            StationConst.replaceNetWorkList(latestNetworkList);
            log.info(" 此时获取的物理网卡列表为 {}",netWorkStr);
            return StationConst.netWorkList;
        }
    }
    private List<String> splitCommaText(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private String resolveMonitorIp(String ip) {
        return StringUtils.isNotBlank(ip) ? ip : StationConst.getOrLoadLinuxIp(() -> "");
    }

    private SysWarnDeployDO getOrCreateWarnDeploy(String ip, String warnType, String cacheKey, boolean cacheEnabled) {
        if (cacheEnabled) {
            SysWarnDeployDO cachedWarnDeploy = StationConst.warnDepMap.get(cacheKey);
            if (ObjectUtil.isNotNull(cachedWarnDeploy)) {
                return cachedWarnDeploy;
            }
        }
        synchronized (warnDeployLoadLock) {
            // 告警配置的首次创建在单机内串行化，避免相同 key 被并发插入多次。
            if (cacheEnabled) {
                SysWarnDeployDO cachedWarnDeploy = StationConst.warnDepMap.get(cacheKey);
                if (ObjectUtil.isNotNull(cachedWarnDeploy)) {
                    return cachedWarnDeploy;
                }
            }
            List<SysWarnDeployDO> warnDeployList = sysWarnDeployMapper.selectList(new LambdaQueryWrapper<SysWarnDeployDO>()
                    .eq(SysWarnDeployDO::getIp, ip)
                    .eq(SysWarnDeployDO::getWarnType, warnType));
            SysWarnDeployDO warnDeploy;
            if (warnDeployList.isEmpty()) {
                warnDeploy = SysWarnDeployDO.builder().warnId(IdUtil.fastSimpleUUID())
                        .warnType(warnType).thresholdSign(">").minThresholdNum(minThresholdNum)
                        .middleThresholdNum(middleThresholdNum).maxThresholdNum(maxThresholdNum)
                        .unit(MetricUnitUtil.defaultWarnUnit(warnType))
                        .ip(ip).status(0).createTime(new Date()).build();
                sysWarnDeployMapper.insert(warnDeploy);
            } else {
                warnDeploy = warnDeployList.get(0);
            }
            if (cacheEnabled) {
                StationConst.warnDepMap.put(cacheKey, warnDeploy);
            }
            return warnDeploy;
        }
    }

    private String formatNow() {
        // DateTimeFormatter 是线程安全的，适合替换单例 SimpleDateFormat。
        return LocalDateTime.now().format(LOG_TIME_FORMATTER);
    }

    private String formatDecimal(double value) {
        // Docker 采集结果统一保留两位小数，避免不同平台展示位数乱飘。
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return decimalFormat.format(value);
    }

    private Integer resolveWarnGrade(String dataRate, String recordUnit, SysWarnDeployDO warnDeploy) {
        if (warnDeploy == null || StringUtils.isBlank(dataRate)) {
            return null;
        }
        double recordValue;
        try {
            recordValue = Double.parseDouble(dataRate);
        } catch (NumberFormatException e) {
            // 脏数据或格式异常不应导致整个监控周期崩溃，记录日志后静默跳过
            log.warn("告警等级计算失败，dataRate 格式异常: {}", dataRate);
            return null;
        }
        double minThreshold = warnDeploy.getMinThresholdNum();
        double middleThreshold = warnDeploy.getMiddleThresholdNum();
        double maxThreshold = warnDeploy.getMaxThresholdNum();
        if (MetricUnitUtil.isRateUnit(recordUnit) || MetricUnitUtil.isRateUnit(warnDeploy.getUnit())) {
            recordValue = MetricUnitUtil.toKbPerSecond(recordValue, recordUnit);
            minThreshold = MetricUnitUtil.toKbPerSecond(minThreshold, warnDeploy.getUnit());
            middleThreshold = MetricUnitUtil.toKbPerSecond(middleThreshold, warnDeploy.getUnit());
            maxThreshold = MetricUnitUtil.toKbPerSecond(maxThreshold, warnDeploy.getUnit());
        }
        if (recordValue >= maxThreshold) {
            return 2;
        }
        if (recordValue >= middleThreshold) {
            return 1;
        }
        if (recordValue >= minThreshold) {
            return 0;
        }
        return null;
    }


    /**
     * 检查指定 Docker 监控项是否已关闭。
     * 缓存中不存在对应条目时视为未关闭（保守策略），且对 null 值安全。
     */
    private boolean isDockerMonitorDisabled(String cacheKey) {
        SysDockerDeployEleDO dep = StationConst.dockerDepMap.get(cacheKey);
        return dep != null && Integer.valueOf(1).equals(dep.getIsShow());
    }

    private static double calculateNetWorkRate(long first, long second) {
        // 网卡文件读取单位为 byte ，需要转化为 KB
        return (second - first) / 1024;
    }

    private static double calculateIORate(long first, long second) {
        // 每个扇区为0.5KB，两次读取间隔为1秒
        return (second - first) * 0.5;
    }

    public static String formatRate(double rate) {
        String[] units = {"KB/s", "MB/s", "GB/s", "TB/s", "PB/s", "EB/s", "ZB/s"};
        int unitIndex = 0;

        while (rate >= 1024 && unitIndex < units.length - 1) {
            rate /= 1024;
            unitIndex++;
        }

        // 优化：通过数学运算保留两位小数，避免 String.format 的正则解析开销
        // Math.round(rate * 100) / 100.0 会将 10.555 变成 10.56
        double roundedRate = Math.round(rate * 100) / 100.0;

        return roundedRate + " " + units[unitIndex];
    }
}
