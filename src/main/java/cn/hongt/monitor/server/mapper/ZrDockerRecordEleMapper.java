package cn.hongt.monitor.server.mapper;

import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZrDockerRecordEleMapper extends BaseMapper<ZrDockerRecordEleDO> {
    // 未启用
    void batchInsert(@Param("list") List<ZrDockerRecordEleDO> list);
}
