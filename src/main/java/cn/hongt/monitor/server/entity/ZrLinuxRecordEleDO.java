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
* @TableName zr_linux_record_ele
*/
@ApiModel(value="linux系统信息记录表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "zr_linux_record_ele")
public class ZrLinuxRecordEleDO implements Serializable {
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
    private String ip;

    /**
     * 利用率-数值
     */
    @ApiModelProperty("利用率-数值")
    @TableField(value = "data_rate")
    private String dataRate;

    /**
     * 利用率-单位
     */
    @ApiModelProperty("利用率-单位")
    @TableField(value = "unit")
    private String unit;

    /**
     * 数据类型 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录
     */
    @ApiModelProperty("数据类型 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录")
    private String type;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

}
