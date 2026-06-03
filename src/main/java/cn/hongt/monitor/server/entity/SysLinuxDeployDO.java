package cn.hongt.monitor.server.entity;

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
* @TableName sys_linux_deploy_ele
*/
@ApiModel(value="linux系统信息配置表")
@Data
@Builder
@AllArgsConstructor
@TableName(value = "sys_linux_deploy_ele")
public class SysLinuxDeployDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ApiModelProperty("主键")
    @TableId(value = "id")
    private String id;

    /**
    * 服务器IP
    */
    @ApiModelProperty("服务器IP")
    @TableField(value = "ip")
    private String ip;

    /**
     * 服务器名称
     */
    @ApiModelProperty("服务器名称")
    @TableField(value = "ip_name")
    private String ipName;

    /**
     * 节点名称
     */
    @ApiModelProperty("节点名称")
    @TableField(value = "node_name")
    private String nodeName;

    /**
     * 数据类型 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录
     */
    @ApiModelProperty("数据类型种类 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录")
    private String type;

    /**
     * 是否展示；当配置为不展示时，Linux IP查询最新数据、根据CPU查询不同IP都无法查询到结果；默认为 均展示
     */
    @ApiModelProperty("是否展示：0：展示，1：不展示")
    @TableField(value = "is_show")
    private Integer isShow;

    @ApiModelProperty("排序")
    @TableField(value = "sort")
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField(value = "update_time")
    private Date updateTime;

    public SysLinuxDeployDO(){
        this.isShow = 0;
    }
}
