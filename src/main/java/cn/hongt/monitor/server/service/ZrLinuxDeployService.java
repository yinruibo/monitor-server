package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.LinuxDeployInput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ZrLinuxDeployService extends IService<SysLinuxDeployDO> {

    List<String> queryNodeList();

    List<SysLinuxDeployDO>queryNodeAndServers(String nodeName);

    List<SysLinuxDeployDO> queryServerList();

    Map<String,Object> queryDeployList(LinuxDeployInput input);

    void updateDeployList(List<SysLinuxDeployDO> inputList);

    void deleteDeployList(IdListInput input);

}
