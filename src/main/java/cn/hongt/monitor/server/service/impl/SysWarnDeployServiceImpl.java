package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import cn.hongt.monitor.server.controller.UpdateMapDataController;
import cn.hongt.monitor.server.dto.input.InsertSysWarnDeployInput;
import cn.hongt.monitor.server.dto.input.SysWarnDeployInput;
import cn.hongt.monitor.server.dto.output.SysWarnDeployOutput;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import cn.hongt.monitor.server.mapper.SysWarnDeployMapper;
import cn.hongt.monitor.server.service.SysWarnDeployService;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class SysWarnDeployServiceImpl extends ServiceImpl<SysWarnDeployMapper, SysWarnDeployDO> implements SysWarnDeployService {

    @Autowired
    private UpdateMapDataController updateController;

    private final ConcurrentMap<String, Object> warnDepInsertLocks = new ConcurrentHashMap<>();

    @Override
    public Result<String> insertWarnDep(InsertSysWarnDeployInput insertSysWarnDeploy) {
        String lockKey = buildWarnDepInsertLockKey(insertSysWarnDeploy);
        Object lock = warnDepInsertLocks.computeIfAbsent(lockKey, key -> new Object());
        try {
            synchronized (lock) {
                SysWarnDeployDO sysWarnDeploy = queryActiveWarnDeploy(insertSysWarnDeploy.getIp(), insertSysWarnDeploy.getWarnType());
                if (sysWarnDeploy != null) {
                    return ResultUtil.errorMsg("告警类型不可重复新增");
                }
                SysWarnDeployDO sysWarnDeployDO = new SysWarnDeployDO();
                BeanUtils.copyProperties(insertSysWarnDeploy, sysWarnDeployDO);
        // 告警配置主键统一改成 Hutool UUID，避免项目里继续混用两套 ID 生成规则。
        sysWarnDeployDO.setWarnId(IdUtil.fastSimpleUUID());
                sysWarnDeployDO.setStatus(0);
                sysWarnDeployDO.setCreateTime(new Date());
                int i = this.baseMapper.insert(sysWarnDeployDO);
                if (0 == i) {
                    return ResultUtil.errorMsg("新增告警信息失败");
                }
                return ResultUtil.success("新增告警信息成功");
            }
        } finally {
            warnDepInsertLocks.remove(lockKey, lock);
        }
    }

    @Override
    public SysWarnDeployOutput queryWarnDepList(SysWarnDeployInput sysWarnDepInput) {
        SysWarnDeployOutput sysWarnDeployOutput = new SysWarnDeployOutput();
        try {
            LambdaQueryWrapper<SysWarnDeployDO> queryWrapper = new LambdaQueryWrapper<SysWarnDeployDO>()
                    .eq(SysWarnDeployDO::getIp, sysWarnDepInput.getIp())
                    .eq(SysWarnDeployDO::getStatus, 0)
                    .orderByAsc(SysWarnDeployDO::getWarnType);
            Page<SysWarnDeployDO> page = this.baseMapper.selectPage(new Page<>(sysWarnDepInput.getPageNo(), sysWarnDepInput.getPageSize()), queryWrapper);
            sysWarnDeployOutput.setSysWarnDeployList(page.getRecords());
            sysWarnDeployOutput.setTotal((int) page.getTotal());
        } catch (Exception e) {
            log.error("查询告警信息列表失败: {}", sysWarnDepInput.getIp(), e);
            throw new RuntimeException("查询告警信息列表失败");
        }
        return sysWarnDeployOutput;
    }

    @Override
    public SysWarnDeployOutput queryWarnDep(SysWarnDeployInput sysWarnDepInput) {
        SysWarnDeployOutput sysWarnDeployOutput = new SysWarnDeployOutput();
        List<SysWarnDeployDO> sysWarnDeployList = this.baseMapper.selectList(new LambdaQueryWrapper<SysWarnDeployDO>()
                .eq(SysWarnDeployDO::getIp, sysWarnDepInput.getIp())
                .eq(SysWarnDeployDO::getStatus, 0)
                .eq(SysWarnDeployDO::getWarnId, sysWarnDepInput.getWarnId()));
        if (!sysWarnDeployList.isEmpty()) {
            sysWarnDeployOutput.setSysWarnDeployDO(sysWarnDeployList.get(0));
        }
        return sysWarnDeployOutput;
    }

    @Override
    public Result<String> updateWarnDep(SysWarnDeployInput sysWarnDepInput) {
        SysWarnDeployDO sysWarnDeployDO = new SysWarnDeployDO();
        // 使用 Hutool 的 copyProperties 并忽略 null 值，避免将未传字段覆盖为 null 导致数据库数据丢失
        cn.hutool.core.bean.BeanUtil.copyProperties(sysWarnDepInput, sysWarnDeployDO,
                cn.hutool.core.bean.copier.CopyOptions.create().ignoreNullValue());
        sysWarnDeployDO.setUpdateTime(new Date());
        int i = this.baseMapper.updateById(sysWarnDeployDO);
        if (0 == i) {
            return ResultUtil.errorMsg("告警信息更新失败");
        }
        return ResultUtil.success("告警信息更新成功");
    }

    @Override
    public Result<String> deleteWarnDep(SysWarnDeployInput sysWarnDepInput) {
        int i = this.baseMapper.deleteById(sysWarnDepInput.getWarnId());
        if (0 == i) {
            return ResultUtil.errorMsg("告警信息删除失败");
        }
        return ResultUtil.success("告警信息删除成功");
    }

    @Override
    public Result<String> warnDepStart(String id, Integer code) {
        LambdaUpdateWrapper<SysWarnDeployDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(SysWarnDeployDO::getStatus, code);
        updateWrapper.eq(SysWarnDeployDO::getWarnId, id);
        int i = this.baseMapper.update(null, updateWrapper);
        updateController.updateWarnDepMap();
        if (0 == i) {
            return ResultUtil.errorMsg("告警启动失败");
        }
        return ResultUtil.success("告警启动成功");
    }

    private SysWarnDeployDO queryActiveWarnDeploy(String ip, String warnType) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<SysWarnDeployDO>()
                .eq(SysWarnDeployDO::getIp, ip)
                .eq(SysWarnDeployDO::getWarnType, warnType)
                .eq(SysWarnDeployDO::getStatus, 0));
    }

    private String buildWarnDepInsertLockKey(InsertSysWarnDeployInput input) {
        // 显式处理 null，避免 String.valueOf(null) 生成字面量 "null"，
        // 导致多个不同的空 IP 请求意外共享同一把锁
        String ip = input.getIp() != null ? input.getIp() : "";
        String warnType = input.getWarnType() != null ? input.getWarnType() : "";
        return ip + '#' + warnType + "#0";
    }
}


