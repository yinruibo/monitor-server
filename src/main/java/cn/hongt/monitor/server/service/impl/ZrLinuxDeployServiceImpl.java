package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.dto.input.IdListInput;
import cn.hongt.monitor.server.dto.input.LinuxDeployInput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.mapper.ZrLinuxDeployMapper;
import cn.hongt.monitor.server.service.ZrLinuxDeployService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZrLinuxDeployServiceImpl extends ServiceImpl<ZrLinuxDeployMapper, SysLinuxDeployDO> implements ZrLinuxDeployService {

    private static final int UPDATE_BATCH_SIZE = 200;

    @Override
    public List<String> queryNodeList() {
        List<SysLinuxDeployDO> linuxDeployDOList = this.baseMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
                .select(SysLinuxDeployDO::getNodeName)
                .eq(SysLinuxDeployDO::getIsShow, 0)
                .isNotNull(SysLinuxDeployDO::getNodeName)
                .groupBy(SysLinuxDeployDO::getNodeName));
        return linuxDeployDOList.stream().map(SysLinuxDeployDO::getNodeName).collect(Collectors.toList());
    }

    @Override
    public List<SysLinuxDeployDO> queryNodeAndServers(String nodeName) {
        return this.baseMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
                .select(SysLinuxDeployDO::getNodeName, SysLinuxDeployDO::getIp, SysLinuxDeployDO::getIpName)
                .eq(StringUtils.isNotBlank(nodeName),SysLinuxDeployDO::getNodeName, nodeName)
                .eq(SysLinuxDeployDO::getIsShow, 0)
                .isNotNull(SysLinuxDeployDO::getIp)
                .groupBy(SysLinuxDeployDO::getNodeName, SysLinuxDeployDO::getIp, SysLinuxDeployDO::getIpName));
    }

    @Override
    public List<SysLinuxDeployDO> queryServerList() {
        return this.baseMapper.selectList(new QueryWrapper<SysLinuxDeployDO>().lambda()
                .select(SysLinuxDeployDO::getIp, SysLinuxDeployDO::getIpName)
                .eq(SysLinuxDeployDO::getIsShow, 0)
                .isNotNull(SysLinuxDeployDO::getIp)
                .groupBy(SysLinuxDeployDO::getIp, SysLinuxDeployDO::getIpName));
    }

    @Override
    public Map<String, Object> queryDeployList(LinuxDeployInput input) {
        Map<String, Object> resultMap = new HashMap<>();
        LambdaQueryWrapper<SysLinuxDeployDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(input.getIp()), SysLinuxDeployDO::getIp, input.getIp());
        queryWrapper.eq(StringUtils.isNotBlank(input.getIpName()), SysLinuxDeployDO::getIpName, input.getIpName());
        queryWrapper.eq(StringUtils.isNotBlank(input.getType()), SysLinuxDeployDO::getType, input.getType());
        queryWrapper.eq(StringUtils.isNotBlank(input.getNodeName()), SysLinuxDeployDO::getNodeName, input.getNodeName());
        queryWrapper.eq(SysLinuxDeployDO::getIsShow, 0);
        queryWrapper.orderByAsc(SysLinuxDeployDO::getSort, SysLinuxDeployDO::getId);
        Page<SysLinuxDeployDO> page = this.baseMapper.selectPage(new Page<>(input.getPageNo(), input.getPageSize()), queryWrapper);
        if (page.getRecords().isEmpty()) {
            return resultMap;
        }
        resultMap.put("data", page.getRecords());
        resultMap.put("total", page.getTotal());
        return resultMap;
    }

    @Override
    public void updateDeployList(List<SysLinuxDeployDO> inputList) {
        List<SysLinuxDeployDO> updateList = inputList.stream()
                .map(this::buildBatchUpdateEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (updateList.isEmpty()) {
            return;
        }
        this.updateBatchById(updateList, UPDATE_BATCH_SIZE);
    }

    @Override
    public void deleteDeployList(IdListInput input) {
        this.baseMapper.deleteBatchIds(input.getIdList());
    }

    private SysLinuxDeployDO buildBatchUpdateEntity(SysLinuxDeployDO input) {
        if (input == null || StringUtils.isBlank(input.getId())) {
            return null;
        }
        SysLinuxDeployDO updateEntity = new SysLinuxDeployDO();
        updateEntity.setId(input.getId());
        boolean changed = false;
        if (StringUtils.isNotBlank(input.getIp())) {
            updateEntity.setIp(input.getIp());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getIpName())) {
            updateEntity.setIpName(input.getIpName());
            changed = true;
        }
        if (StringUtils.isNotBlank(input.getNodeName())) {
            updateEntity.setNodeName(input.getNodeName());
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
