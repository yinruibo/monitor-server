package cn.hongt.monitor.server.dto.input;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@ApiModel(value = "告警监控记录表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ZrWarnRecordInput{


    /**
     * 告警标志
     */
    @ApiModelProperty(value = "告警标志，中文字符串")
    private String warnSign;

    /**
     * 告警类型
     */
    @ApiModelProperty(value = "告警类型：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录")
    private Integer warnType;

    /**
     * 告警级别：0：一般，1：严重，2：非常严重
     */
    @ApiModelProperty(value = "告警级别：0：一般，1：严重，2：非常严重")
    private Integer grade;

    /**
     * 状态：0：正常，1：异常
     */
    @ApiModelProperty(value = "状态：0：正常，1：异常")
    private Integer status;

    /**
     * 告警说明
     */
    @ApiModelProperty(value = "告警说明")
    private String explain;

    /**
     * 首次告警时间
     */
    @ApiModelProperty(value = "首次告警时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 持续时间
     */
    @ApiModelProperty(value = "持续时间")
    private String continuedTime;


}
