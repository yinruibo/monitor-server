package cn.hongt.monitor.server.dto.output;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeMonitorOutput {

    /**
     * 节点名称
     */
    @ApiModelProperty("节点名称")
    private String nodeName;

    /**
     * 服务器IP
     */
    @ApiModelProperty("服务器IP")
    private String ip;

    /**
     * docker中容器名称
     */
    @ApiModelProperty("服务器名称")
    private String ipName;

    /**
     * CPU利用率
     */
    @ApiModelProperty(value="CPU利用率")
    private String cpu;

    /**
     * 内存利用率
     */
    @ApiModelProperty(value="内存利用率")
    private String memory;

    /**
     * 磁盘利用率
     */
    @ApiModelProperty(value="磁盘列表")
    private List<DiskOutput> diskList;

    /**
     * 磁盘IO读取速率
     */
    @ApiModelProperty(value="磁盘IO读取速率")
    private String IORead;

    /**
     * 磁盘IO写入速率
     */
    @ApiModelProperty(value="磁盘IO写入速率")
    private String IOWrite;

    /**
     * 网卡入站速率
     */
    @ApiModelProperty(value="网卡入站速率")
    private String netWorkUp;

    @ApiModelProperty(value="网卡出站出率")
    private String netWorkDown;


    /**
     * 时间戳
     */
    @ApiModelProperty(value="时间戳")
    private Date timestamp;

    @Data
    @Builder
//    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiskOutput {

        @ApiModelProperty(value="磁盘类型")
        private String type;

        @ApiModelProperty(value="磁盘利用率")
        private String disk;

        private DiskOutput(String type,String disk){
            this.type = type;
            this.disk = disk;
        }

    }

}

