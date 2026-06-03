package cn.hongt.monitor.server.service;


import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.InsertSysWarnDeployInput;
import cn.hongt.monitor.server.dto.input.SysWarnDeployInput;
import cn.hongt.monitor.server.dto.output.SysWarnDeployOutput;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysWarnDeployService extends IService<SysWarnDeployDO> {

    Result<String> insertWarnDep(InsertSysWarnDeployInput insertSysWarnDeploy);

    SysWarnDeployOutput queryWarnDepList(SysWarnDeployInput sysWarnDepInput);

    SysWarnDeployOutput queryWarnDep(SysWarnDeployInput sysWarnDepInput);

    Result<String> updateWarnDep(SysWarnDeployInput sysWarnDepInput);

    Result<String> deleteWarnDep(SysWarnDeployInput sysWarnDepInput);

    Result<String> warnDepStart(String Id,Integer code);

}
