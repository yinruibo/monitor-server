package cn.hongt.monitor.server.mapper;

import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ZrDockerDeployMapper extends BaseMapper<SysDockerDeployEleDO> {
}
