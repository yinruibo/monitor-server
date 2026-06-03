package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.WarnRecMessage;
import cn.hongt.monitor.server.entity.ZrWarnRecordEleDO;
import cn.hongt.monitor.server.dto.input.ZrWarnRecordListInput;
import cn.hongt.monitor.server.dto.output.WarnFaultOutput;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ZrWarnRecordEleService extends IService<ZrWarnRecordEleDO> {

    Map<String,Object> queryWarnDepList(ZrWarnRecordListInput zrWarnRecordListInput);

    void deleteWarnRecord(List<String> ids);

    Map<String,Object> queryWarnSignList(ZrWarnRecordListInput input);

    Result<List<WarnFaultOutput>> queryFaultList(ZrWarnRecordListInput input);

    Result<String> queryExport(List<String> idList);

    Result<String> queryTimingUpdate();

    Result<Integer> queryWarnNumber(String ip);

    // 服务器docker CPU、内存监控
    void warnDockerBySchCpuMemory();

    void warnDockerIOBySch();

    void warnDockerNetWorkBySch();

    // 定时任务，读取CPU 数值
    void warnLinuxCPUBySch();

    // 定时任务，读取内存 数值
    void warnLinuxMemoryBySch();

    // 定时任务，读取磁盘 数值
    void warnLinuxDiskBySch();

    // 定时任务，读取IO读写 数值
    void warnLinuxIOBySch();

    // 定时任务，读取网卡监控 数值
    void warnLinuxNetWorkBySch();

    // 定时任务，读取CPU 数值
    void warnWinCPUBySch();

    // 定时任务，读取内存 数值
    void warnWinMemoryBySch();

    // 定时任务，读取磁盘 数值
    void warnWinDiskBySch();

    // 定时任务，读取IO读写 数值
    void warnWinIOBySch();

    // 定时任务，读取网卡监控 数值
    void warnWinNetWorkBySch();



}
