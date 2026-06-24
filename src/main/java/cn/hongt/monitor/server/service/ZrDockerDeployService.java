package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.dto.input.NodeDataInput;
import cn.hongt.monitor.server.dto.output.DockerImagesOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ZrDockerDeployService extends IService<SysDockerDeployEleDO> {

    List<String> queryNodeList();

    List<SysDockerDeployEleDO> queryNodeAndServers(String nodeName);

    List<SysDockerDeployEleDO> queryServerList();

    List<DockerImagesOutput> queryDockerNameList(NodeDataInput input);

    Map<String,Object> queryDeployList(DockerDeployInput input);

    void updateDeployList(List<SysDockerDeployEleDO> inputList);

    void deleteDeployList(IdListInput input);
}
