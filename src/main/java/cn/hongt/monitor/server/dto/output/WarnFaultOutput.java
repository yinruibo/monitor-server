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
public class WarnFaultOutput {

    /**
     * 告警监控时间点
     */
    @ApiModelProperty(value="告警监控时间点")
    private String obstime;

    /**
     * 告警监控时间点对应个数
     */
    @ApiModelProperty(value="告警监控时间点对应个数")
    private Integer timeNumber;

}
