package cn.hongt.monitor.server.dto.output;

import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 告警管理输出
 */
@ApiModel(value="告警管理返回数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysWarnDeployOutput {

    // 查询告警管理详情返回
    private SysWarnDeployDO sysWarnDeployDO;
    // 查询告警管理列表返回
    private List<SysWarnDeployDO> sysWarnDeployList;
    // 返回列表条数
    private Integer total;

}
