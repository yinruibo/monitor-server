package cn.hongt.monitor.server.dto.input;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@ApiModel(value = "查询硬件监控")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HardWareMonitorInput {

    /**
     * 硬件监控开始时间
     */
    @NotBlank(message = "开始时间不能为空")
    @ApiModelProperty(value = "硬件监控开始时间：yyyy-MM-dd HH:mm:ss")
    private String startTime;

    /**
     * 硬件监控结束时间
     */
    @NotBlank(message = "结束时间不能为空")
    @ApiModelProperty(value = "硬件监控结束时间：yyyy-MM-dd HH:mm:ss")
    private String endTime;

    /**
     * 节点编号
     */
    @ApiModelProperty(value = "节点编号：192.168.1.124")
    private String ip;

    /**
     * 节点编号
     */
    @ApiModelProperty(value = "数据类型 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录1")
    private String type;

    /**
     * docker中容器名称
     */
    @ApiModelProperty("docker中容器名称")
    private String dockerName;
}
