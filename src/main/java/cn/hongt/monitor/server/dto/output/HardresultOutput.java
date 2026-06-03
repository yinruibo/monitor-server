package cn.hongt.monitor.server.dto.output;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HardresultOutput {

    /**
     * 时间戳
     */
    @ApiModelProperty(value="时间戳")
    private Date timestamp;

    /**
     * 最大值
     */
    @ApiModelProperty(value="最大值")
    private double Minimum;

    /**
     * 最小值
     */
    @ApiModelProperty(value="最小值")
    private double Maximum;

    /**
     * 平均值
     */
    @ApiModelProperty(value="平均值")
    private double Average;

    /**
     * 当前时间序列展示单位
     */
    @ApiModelProperty(value="当前时间序列展示单位")
    private String unit;
}
