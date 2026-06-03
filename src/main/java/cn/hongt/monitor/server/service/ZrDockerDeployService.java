package cn.hongt.monitor.server.service;

import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.dto.output.DockerImagesOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

public interface ZrDockerDeployService extends IService<SysDockerDeployEleDO> {

    List<String> queryNodeList();

    List<SysDockerDeployEleDO> queryNodeAndServers(String nodeName);

    List<SysDockerDeployEleDO> queryServerList();

    List<DockerImagesOutput> queryDockerNameList(String nodeName, String ip);

    Map<String,Object> queryDeployList(DockerDeployInput input);

    void updateDeployList(List<SysDockerDeployEleDO> inputList);

    void deleteDeployList(List<String> idList);
}
