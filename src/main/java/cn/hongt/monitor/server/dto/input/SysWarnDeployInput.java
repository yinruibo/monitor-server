package cn.hongt.monitor.server.dto.input;

import cn.hongt.monitor.server.common.page.AbstractPageQuery;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警管理配置表
 */
@ApiModel(value="告警管理查询")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SysWarnDeployInput extends AbstractPageQuery {

    /**
     * 告警配置Id
     */
    @ApiModelProperty(value="告警配置Id")
    private String warnId;

    /**
     * 服务器IP
     */
    @ApiModelProperty("服务器IP")
    @TableField(value = "ip")
    private String ip;

    /**
     * 告警类型
     */
    @ApiModelProperty(value="告警类型：linux_cpu：CPU告警，linux_memory：内存告警，linux_disk：磁盘告警")
    private String warnType;

    /**
     * 阈值符号：<：小于，>：大于
     */
    @ApiModelProperty(value="阈值符号：>：大于")
    private String thresholdSign;

    /**
     * 最小阈值数值
     */
    @ApiModelProperty(value="最小阈值数值")
    private Integer minThresholdNum;

    /**
     * 中位阈值数值
     */
    @ApiModelProperty(value="中位阈值数值")
    private Integer middleThresholdNum;

    /**
     * 最大阈值数值
     */
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
    @ApiModelProperty(value="阈值消息：0：发送，1：不发送")
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
