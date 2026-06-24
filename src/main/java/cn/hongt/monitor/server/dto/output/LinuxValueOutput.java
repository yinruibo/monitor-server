package cn.hongt.monitor.server.dto.output;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@ApiModel(value = "查询服务器资源使用情况")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinuxValueOutput {

    // 时间
    @ApiModelProperty(value="时间戳")
    private Date time;
    // 值
    @ApiModelProperty(value="值")
    private double values;
    // 单位
    @ApiModelProperty(value="单位")
    private String unit;
}
