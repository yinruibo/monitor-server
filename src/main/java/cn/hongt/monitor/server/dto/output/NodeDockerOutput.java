package cn.hongt.monitor.server.dto.output;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
*
*/
@ApiModel(value="节点Docker监控返回数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeDockerOutput {

    /**
    * 服务器IP
    */
    @ApiModelProperty("服务器IP")
    private String ip;
    /**
     * 容器名称
     */
    @ApiModelProperty("容器名称")
    private String dockerName;
    /**
     * 节点名称
     */
    @ApiModelProperty("节点名称")
    private String nodeName;
    /**
     * 任务名称
     */
    @ApiModelProperty("任务名称")
    private String taskName;
    /**
    * CPU利用率
    */
    @ApiModelProperty("CPU利用率")
    private String cpu;
    /**
     * CPU颜色
     */
//    @ApiModelProperty("CPU颜色")
//    private String cpuColor;

    /**
    * 内存利用率
    */
    @ApiModelProperty("内存利用率")
    private String memory;
    /**
     * IO读取速率
     */
    @ApiModelProperty("IO读取速率")
    private String IORead;

    /**
     * IO写入速率
     */
    @ApiModelProperty("IO写入速率")
    private String IOWrite;
    /**
     * 网卡上传速率
     */
    @ApiModelProperty("网卡上传速率")
    private String networkUp;

    /**
     * 网卡下载速率
     */
    @ApiModelProperty("网卡下载速率")
    private String networkDown;

    /**
     * 时间戳
     */
    @ApiModelProperty(value="时间戳")
    private Date timestamp;
}
