package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.dto.output.DockerImagesOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.service.ZrDockerDeployService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZrDockerDeployServiceImpl extends ServiceImpl<ZrDockerDeployMapper, SysDockerDeployEleDO> implements ZrDockerDeployService {

    private static final int UPDATE_BATCH_SIZE = 200;

    @Override
    public List<String> queryNodeList() {
        List<SysDockerDeployEleDO> dockerDepList = this.baseMapper.selectList(new QueryWrapper<SysDockerDeployEleDO>().lambda()
                .eq(SysDockerDeployEleDO::getIsShow, 0)
                .isNotNull(SysDockerDeployEleDO::getNodeName));
        return dockerDepList.stream().map(SysDockerDeployEleDO::getNodeName).distinct().collect(Collectors.toList());
    }

    @Override
    public List<SysDockerDeployEleDO> queryNodeAndServers(String nodeName) {
        return this.baseMapper.selectList(new QueryWrapper<SysDockerDeployEleDO>().lambda()
                .select(SysDockerDeployEleDO::getNodeName, SysDockerDeployEleDO::getIp)
                .eq(SysDockerDeployEleDO::getNodeName, nodeName)
                .eq(SysDockerDeployEleDO::getIsShow, 0)
                .isNotNull(SysDockerDeployEleDO::getIp)
                .groupBy(SysDockerDeployEleDO::getNodeName, SysDockerDeployEleDO::getIp));
    }

    @Override
    public List<SysDockerDeployEleDO> queryServerList() {
        return this.baseMapper.selectList(new QueryWrapper<SysDockerDeployEleDO>().lambda()
                .select(SysDockerDeployEleDO::getIp)
                .eq(SysDockerDeployEleDO::getIsShow, 0)
                .isNotNull(SysDockerDeployEleDO::getIp)
                .groupBy(SysDockerDeployEleDO::getIp));
    }

    @Override
    public List<DockerImagesOutput> queryDockerNameList(String nodeName, String ip) {
        List<DockerImagesOutput> outputList = new ArrayList<>();
        LambdaQueryWrapper<SysDockerDeployEleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(ip), SysDockerDeployEleDO::getIp, ip);
        queryWrapper.eq(StringUtils.isNotBlank(nodeName), SysDockerDeployEleDO::getNodeName, nodeName);
        queryWrapper.eq(SysDockerDeployEleDO::getType, WarnRecordEnum.docker_cpu.getCode());
        queryWrapper.eq(SysDockerDeployEleDO::getIsShow, 0);
        List<SysDockerDeployEleDO> dockerDeployS = this.baseMapper.selectList(queryWrapper);
        if (!dockerDeployS.isEmpty()) {
            for (SysDockerDeployEleDO dockerDep : dockerDeployS) {
                DockerImagesOutput output = new DockerImagesOutput();
                BeanUtils.copyProperties(dockerDep, output);
                outputList.add(output);
            }
        }
        return outputList;
    }

    @Override
    public Map<String, Object> queryDeployList(DockerDeployInput input) {
        Map<String, Object> resultMap = new HashMap<>();
        LambdaQueryWrapper<SysDockerDeployEleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(input.getIp()), SysDockerDeployEleDO::getIp, input.getIp());
        queryWrapper.eq(StringUtils.isNotBlank(input.getTaskName()), SysDockerDeployEleDO::getTaskName, input.getTaskName());
        queryWrapper.eq(StringUtils.isNotBlank(input.getDockerName()), SysDockerDeployEleDO::getDockerName, input.getDockerName());
        queryWrapper.eq(StringUtils.isNotBlank(input.getType()), SysDockerDeployEleDO::getType, input.getType());
        queryWrapper.eq(StringUtils.isNotBlank(input.getNodeName()), SysDockerDeployEleDO::getNodeName, input.getNodeName());
        queryWrapper.eq(SysDockerDeployEleDO::getIsShow, 0);
        queryWrapper.orderByAsc(SysDockerDeployEleDO::getSort, SysDockerDeployEleDO::getId);
        Page<SysDockerDeployEleDO> page = this.baseMapper.selectPage(new Page<>(input.getPageNo(), input.getPageSize()), queryWrapper);
        if (page.getRecords().isEmpty()) {
            return resultMap;
        }
        resultMap.put("data", page.getRecords());
        resultMap.put("total", page.getTotal());
        return resultMap;
    }

    @Override
    public void updateDeployList(List<SysDockerDeployEleDO> inputList) {
        List<SysDockerDeployEleDO> updateList = inputList.stream()
                .map(this::buildBatchUpdateEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (updateList.isEmpty()) {
            return;
        }
        this.updateBatchById(updateList, UPDATE_BATCH_SIZE);
    }

    @Override
    public void deleteDeployList(List<String> idList) {
        this.baseMapper.deleteBatchIds(idList);
    }

    private SysDockerDeployEleDO buildBatchUpdateEntity(SysDockerDeployEleDO input) {
        if (input == null || StringUtils.isBlank(input.getId())) {
            return null;
        }
        SysDockerDeployEleDO updateEntity = new SysDockerDeployEleDO();
        updateEntity.setId(input.getId());
        boolean changed = false;
        if (StringUtils.isNotBlank(input.getIp())) {
            updateEntity.setIp(input.getIp());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getDockerName())) {
            updateEntity.setDockerName(input.getDockerName());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getNodeName())) {
            updateEntity.setNodeName(input.getNodeName());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getTaskName())) {
            updateEntity.setTaskName(input.getTaskName());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getType())) {
            updateEntity.setType(input.getType());
            changed = true;
        }
        if (input.getIsShow() != null) {
            updateEntity.setIsShow(input.getIsShow());
            changed = true;
        }
        if (input.getSort() != null) {
            updateEntity.setSort(input.getSort());
            changed = true;
        }
        return changed ? updateEntity : null;
    }
}
