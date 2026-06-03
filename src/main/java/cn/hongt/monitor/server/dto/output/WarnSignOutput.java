package cn.hongt.monitor.server.dto.output;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警监控记录表
 */
@ApiModel(value="告警监控记录表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarnSignOutput {

    /**
     * 告警标志
     */
    @ApiModelProperty(value="告警标志")
    private String warnsign;

    /**
     * 文件个数
     */
    @ApiModelProperty(value="文件个数")
    private Integer num;

}
