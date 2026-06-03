package cn.hongt.monitor.server.dto.input;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
*
*/
@ApiModel(value="节点Docker监控接收数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeDockerInput {

    /**
    * 服务器IP
    */
    @ApiModelProperty("服务器IP")
    private String ip;

    /**
     * 服务器IP
     */
    @ApiModelProperty("节点名称")
    private String nodeName;
}
