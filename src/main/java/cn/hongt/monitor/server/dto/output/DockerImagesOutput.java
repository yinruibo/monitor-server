package cn.hongt.monitor.server.dto.output;

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
@ApiModel(value="Docker监控配置镜像信息列表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DockerImagesOutput{

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
    private String dockerName;

    /**
     * 节点名称
     */
    @ApiModelProperty("节点名称")
    private String nodeName;

    /**
     * 任务名称
     */
    @ApiModelProperty("镜像中文名称")
    private String taskName;

    /**
     * 利用率
     */
    @ApiModelProperty("数据类型 ：docker_cpu：CPU，docker_memory：内存")
    private String type;

}
