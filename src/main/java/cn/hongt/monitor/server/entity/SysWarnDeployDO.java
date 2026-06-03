package cn.hongt.monitor.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
    * 告警管理配置表
    */
@ApiModel(value="告警管理配置表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "sys_warn_deploy")
public class SysWarnDeployDO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
    * 告警配置Id
    */
    @TableId(value = "warn_id", type = IdType.INPUT)
    @ApiModelProperty(value="告警配置Id")
    private String warnId;

    /**
     * 服务器IP
     */
    @ApiModelProperty("服务器IP")
    @TableField(value = "ip")
    private String ip;

    /**
    * 告警类型:linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录
    */
    @TableField(value = "warn_type")
    @ApiModelProperty(value="告警类型:linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录")
    private String warnType;

    /**
    * 阈值符号：<：小于，>：大于
    */
    @TableField(value = "threshold_sign")
    @ApiModelProperty(value="阈值符号: >：大于")
    private String thresholdSign;

    /**
     * 最小阈值数值
     */
    @TableField(value = "min_threshold_num")
    @ApiModelProperty(value="最小阈值数值")
    private Integer minThresholdNum;

    /**
     * 中位阈值数值
     */
    @TableField(value = "middle_threshold_num")
    @ApiModelProperty(value="中位阈值数值")
    private Integer middleThresholdNum;

    /**
     * 最大阈值数值
     */
    @TableField(value = "max_threshold_num")
    @ApiModelProperty(value="最大阈值数值")
    private Integer maxThresholdNum;

    /**
     * 利用率-单位
     */
    @TableField(value = "unit")
    @ApiModelProperty("利用率-单位")
    private String unit;

    /**
    * 告警描述
    */
    @ApiModelProperty(value="告警描述")
    private String remarks;

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

    /**
    * 告警配置状态：0：启用，1：禁用
    */
    @ApiModelProperty(value="告警配置状态：0：启用，1：禁用")
    private Integer status;

    /**
    * 创建时间
    */
    @TableField(value = "create_time")
    @ApiModelProperty(value="创建时间")
    private Date createTime;

    /**
    * 更新时间
    */
    @TableField(value = "update_time")
    @ApiModelProperty(value="更新时间")
    private Date updateTime;


}