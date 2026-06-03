package cn.hongt.monitor.server.mapper;

import cn.hongt.monitor.server.dto.output.WarnFaultOutput;
import cn.hongt.monitor.server.entity.ZrWarnRecordEleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ZrWarnRecordEleMapper extends BaseMapper<ZrWarnRecordEleDO> {

    List<WarnFaultOutput> queryFaultList(@Param("start") Date start, @Param("end")Date end, @Param("ip")String ip);

}
