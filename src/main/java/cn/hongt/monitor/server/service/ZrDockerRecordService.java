package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeDockerOutput;
import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.common.utils.Result;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ZrDockerRecordService extends IService<ZrDockerRecordEleDO> {

    Result<List<NodeDockerOutput>> queryNodeDocker(String nodeName,String ip);

    Result<List<HardresultOutput>> queryDockerRecord(HardWareMonitorInput input);
}
