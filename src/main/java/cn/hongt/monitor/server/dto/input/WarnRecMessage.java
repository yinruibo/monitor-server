package cn.hongt.monitor.server.dto.input;

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

@ApiModel(value = "告警监控记录表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarnRecMessage{

    /**
     * 主键Id
     */
    @ApiModelProperty(value="告警记录Id")
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 多服务IP
     */
    private String ip;

    /**
     * 文件告警类型
     */
    @TableField(value = "warn_file_type")
    private String warnFileType;

    /**
     * 告警类型: linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录
     */
    @TableField(value = "warn_type")
    private String warnType;

    /**
     * 告警级别：0：一般，1：严重，2：非常严重
     */
    private Integer grade;

    /**
     * 告警状态：0：运行中，1：已完成
     */
    private Integer status;

    /**
     * 告警说明
     */
    private String explain;

    /**
     * 告警说明
     */
    private String filePath;

    /**
     * 持续时间
     */
    @TableField(value = "continued_time")
    private String continuedTime;

    /**
     * 首次告警时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
}
