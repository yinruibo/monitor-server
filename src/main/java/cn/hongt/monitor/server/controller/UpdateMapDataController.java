package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.common.consts.Const;
import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.CommandUtil;
import cn.hongt.monitor.server.entity.*;
import cn.hongt.monitor.server.mapper.*;
import cn.hongt.monitor.server.service.impl.ZrWarnRecordEleServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yrb
 * @date 2024/2/26
 * @Description: 手动更新服务缓存接口
 */
@Api(tags = "手动更新缓存MapData数据服务")
@Slf4j
@RestController
@RequestMapping("/update")
public class UpdateMapDataController {

    @Autowired
    private ZrDockerDeployMapper dockerDeployMapper;
    @Autowired
    private ZrLinuxDeployMapper linuxDeployMapper;
    @Autowired
    private SysWarnDeployMapper sysWarnDeployMapper;
    @Autowired
    private ZrWarnRecordEleServiceImpl warnRecordEleServiceImpl;


    @GetMapping("/updateIP")
    @ApiOperation(value = "更新-服务器内网IP", httpMethod = "GET")
    public void updateIP() {
        String monitorIp = StationConst.refreshLinuxIp(warnRecordEleServiceImpl.networkIP(Const.networkipUrl));
        log.info("=========此时执行命令更新的内网IP为 {} =====", monitorIp);
    }

    @GetMapping("/updateDockerContNames")
    @ApiOperation(value = "更新-Docker监控-容器名称列表", httpMethod = "GET")
    public void updateDockerContNames() {
        String contNames = StationConst.refreshContNames(CommandUtil.containerCmd(new String[]{"sh", "-c","docker ps --format '{{.Names}}' | sort | uniq"}));
        log.info("=========此时执行命令打印出的容器名称列表为 {} =====", contNames);
    }

    @GetMapping("/updateDockerDepMap")
    @ApiOperation(value = "更新-Docker监控-历史监控-配置表", httpMethod = "GET")
    public void updateDockerDepMap() {
        // 先构建完整快照，再一次性替换缓存，避免并发读取拿到半成品数据。
        Map<String, SysDockerDeployEleDO> latestDockerDepMap = new HashMap<>();
        List<SysDockerDeployEleDO> dockerDepList = dockerDeployMapper.selectList(new LambdaQueryWrapper<SysDockerDeployEleDO>()
                .eq(SysDockerDeployEleDO::getIp,StationConst.linuxIP).orderByDesc(SysDockerDeployEleDO::getSort));
        if(!dockerDepList.isEmpty()){
            for(SysDockerDeployEleDO deployEle : dockerDepList){
                latestDockerDepMap.put(deployEle.getDockerName()+"_"+deployEle.getType(),deployEle);
            }
            StationConst.replaceDockerDepMap(latestDockerDepMap);
            log.info("此时更新后的 dockerDepMap 内的数据为 {}",StationConst.dockerDepMap.toString());
        }else {
            StationConst.replaceDockerDepMap(latestDockerDepMap);
            log.info(" 此时 sys_docker_deploy_ele 表无数据 ");
        }
    }

    @GetMapping("/updateLinuxNetWorkList")
    @ApiOperation(value = "更新--服务器网卡监控--缓存信息", httpMethod = "GET")
    public void updateLinuxNetWorkList() {
        String netWorkStr = CommandUtil.containerCmd(new String[]{"sh", "-c","ls -l /sys/class/net | grep '/pci' | awk '{print $9}'"});
        List<String> latestNetworkList = new ArrayList<>();
        if(netWorkStr != null && !"".equals(netWorkStr.trim())){
            latestNetworkList = Arrays.asList(netWorkStr.split(","));
        }
        // 发布不可变快照，避免监控线程遍历时列表被修改。
        StationConst.replaceNetWorkList(latestNetworkList);
        log.info(" 此时获取的物理网卡列表为 {}",netWorkStr);
    }

    @GetMapping("/updateLinuxDepMap")
    @ApiOperation(value = "更新-Linux监控-历史监控-配置表", httpMethod = "GET")
    public void updateLinuxDepMap() {
        // Linux 配置刷新和异步采集并发发生时，快照替换比 clear + put 更稳。
        Map<String, SysLinuxDeployDO> latestLinuxDepMap = new HashMap<>();
        List<SysLinuxDeployDO> linuxDepList = linuxDeployMapper.selectList(new LambdaQueryWrapper<SysLinuxDeployDO>()
                .eq(SysLinuxDeployDO::getIp,StationConst.linuxIP).orderByDesc(SysLinuxDeployDO::getSort));
        if(!linuxDepList.isEmpty()){
            for(SysLinuxDeployDO deployEle : linuxDepList){
                latestLinuxDepMap.put(deployEle.getType(),deployEle);
            }
            StationConst.replaceLinuxDepMap(latestLinuxDepMap);
            log.info("此时更新后的 linuxDepMap 内的数据为 {}",StationConst.linuxDepMap.toString());
        }else {
            StationConst.replaceLinuxDepMap(latestLinuxDepMap);
            log.info(" 此时 sys_linux_deploy_ele 表无数据 ");
        }
    }


    @GetMapping("/updateWarnDepMap")
    @ApiOperation(value = "更新--告警监控-配置表", httpMethod = "GET")
    public void updateWarnDepMap() {
        // 告警线程频繁读取该缓存，这里不能暴露清空后的瞬时空状态。
        Map<String, SysWarnDeployDO> latestWarnDepMap = new HashMap<>();
        List<SysWarnDeployDO> warnDeployList = sysWarnDeployMapper.selectList(new LambdaQueryWrapper<SysWarnDeployDO>()
                .eq(SysWarnDeployDO::getIp,StationConst.linuxIP));
        if(!warnDeployList.isEmpty()){
            for(SysWarnDeployDO warnDeploy : warnDeployList){
                latestWarnDepMap.put(warnDeploy.getWarnType(),warnDeploy);
            }
            StationConst.replaceWarnDepMap(latestWarnDepMap);
            log.info("此时更新后的 warnDepMap 内的数据为 {}",StationConst.warnDepMap.toString());
        }else {
            StationConst.replaceWarnDepMap(latestWarnDepMap);
            log.info(" 此时 sys_warn_deploy 表无数据 ");
        }
    }

}
