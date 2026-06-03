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
* @TableName sys_docker_deploy_ele
*/
@ApiModel(value="Docker信息配置表")
@Data
@Builder
@AllArgsConstructor
@TableName(value = "sys_docker_deploy_ele")
public class SysDockerDeployEleDO implements Serializable {
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
     * docker中容器名称
     */
    @ApiModelProperty("docker中容器名称")
    @TableField(value = "docker_name")
    private String dockerName;

    /**
     * 节点名称
     */
    @ApiModelProperty("节点名称")
    @TableField(value = "node_name")
    private String nodeName;

    /**
     * 任务名称
     */
    @ApiModelProperty("任务名称（容器显示名称）")
    @TableField(value = "task_name")
    private String taskName;

    /**
     * 利用率
     */
    @ApiModelProperty("数据类型 ：docker_cpu：CPU，docker_memory：内存")
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
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField(value = "update_time")
    private Date updateTime;

    public SysDockerDeployEleDO(){
        this.isShow = 0;
    }
}
