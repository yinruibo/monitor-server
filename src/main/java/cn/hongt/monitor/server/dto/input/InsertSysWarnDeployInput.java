package cn.hongt.monitor.server.dto.input;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 告警管理配置表
 */
@ApiModel(value="告警管理新增数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InsertSysWarnDeployInput {

    /**
     * 告警类型：0：CPU告警，1：文件异常，2：内存告警，3：磁盘告警
     */
    @NotNull(message = "告警类型不能为空")
    @ApiModelProperty(value="告警类型：linux_cpu：CPU告警，linux_memory：内存告警，linux_disk：磁盘告警，3：资料引接告警，4：融合分析告警，5：服务化产品告警")
    private String warnType;

    /**
     * 服务器IP
     */
    @ApiModelProperty("服务器IP")
    @TableField(value = "ip")
    private String ip;

    /**
     * 阈值符号：<：小于，>：大于
     */
    @NotBlank(message = "阈值符号不能为空")
    @ApiModelProperty(value="阈值符号：<：小于，>：大于")
    private String thresholdSign;

    /**
     * 最小阈值数值
     */
    @NotNull(message = "阈值数值不能为空")
    @ApiModelProperty(value="最小阈值数值")
    private Integer minThresholdNum;

    /**
     * 中位阈值数值
     */
    @NotNull(message = "阈值数值不能为空")
    @ApiModelProperty(value="中位阈值数值")
    private Integer middleThresholdNum;

    /**
     * 最大阈值数值
     */
    @NotNull(message = "阈值数值不能为空")
    @ApiModelProperty(value="最大阈值数值")
    private Integer maxThresholdNum;

    /**
     * 告警描述
     */
    @ApiModelProperty(value="告警描述")
    private String remarks;

    /**
     * 阈值消息：0：发送，1：不发送
     */
    @NotNull(message = "阈值消息不能为空")
    @ApiModelProperty(value="阈值消息")
    private Integer news;

    /**
     * 邮箱-阈值发送邮件
     */
    @ApiModelProperty(value="邮箱-阈值发送邮件")
    private String email;

    /**
     * 手机号-阈值发送短信
     */
    @ApiModelProperty(value="手机号-阈值发送短信")
    private String phone;

}
