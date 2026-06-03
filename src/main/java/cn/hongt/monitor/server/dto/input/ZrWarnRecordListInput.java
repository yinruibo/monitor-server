package cn.hongt.monitor.server.dto.input;

import cn.hongt.monitor.server.common.page.AbstractPageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * @author yrb
 * @date 2021/8/18
 * @description ：雷达数据列表查询-Input
 */
@Data
@Builder
@ApiModel(value = "告警日志列表查询-Input")
@NoArgsConstructor
@AllArgsConstructor
public class ZrWarnRecordListInput extends AbstractPageQuery {

    @ApiModelProperty(value = "服务器IP")
    @NotBlank(message = "IP不得为空")
    private String ip;

    @ApiModelProperty(value = "开始时间-yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "开始时间不得为空")
    private String startTime;

    @ApiModelProperty(value = "结束时间-yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "结束时间不得为空")
    private String endTime;

    /**
     * 告警类型
     */
    @ApiModelProperty(value = "告警类型：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录")
    private String warnType;

    /**
     * 告警级别：0：一般，1：严重，2：非常严重
     */
    @ApiModelProperty(value = "告警级别：0：一般，1：严重，2：非常严重")
    private Integer grade;

}
