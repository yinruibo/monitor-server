package cn.hongt.monitor.server.dto.input;

import cn.hongt.monitor.server.common.page.AbstractPageQuery;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
*
*/
@ApiModel(value="Docker监控配置接收数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false) // 声明 不希望调用父类的 equals 和 hashCode 方法
public class DockerDeployInput extends AbstractPageQuery {

    /**
     * 服务器IP
     */
    @ApiModelProperty("服务器IP")
    @TableId(value = "ip", type = IdType.INPUT)
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

}
