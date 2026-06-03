package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeMonitorOutput;
import cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ZrLinuxRecordService extends IService<ZrLinuxRecordEleDO> {

    List<HardresultOutput> queryServerRecord(HardWareMonitorInput input);

    NodeMonitorOutput queryNodeMonitor(String nodeName,String ip);
}
