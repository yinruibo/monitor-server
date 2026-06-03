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
* 
* @TableName zr_docker_record_ele
*/
@ApiModel(value="linux系统中docker监控信息记录表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "zr_docker_record_ele") // 此表无主键
public class ZrDockerRecordEleDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 服务器IP
     */
    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
    * 服务器IP
    */
    @ApiModelProperty("服务器IP")
    @TableField(value = "ip")
    private String ip;

    /**
     * docker中容器名称
     */
    @ApiModelProperty("docker中容器名称")
    @TableField(value = "docker_name")
    private String dockerName;

    /**
     * 利用率
     */
    @ApiModelProperty("利用率")
    @TableField(value = "data_rate")
    private String dataRate;

    /**
     * 单位
     */
    @ApiModelProperty("单位")
    @TableField(value = "unit")
    private String unit;

    /**
     * 数据类型 ：0：CPU，1：内存，2：磁盘目录1
     */
    @ApiModelProperty("数据类型 ：0：CPU，1：内存，2：磁盘目录1")
    private String type;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time")
    private Date createTime;

}
