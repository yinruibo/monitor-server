package cn.hongt.monitor.server.dto.input;

import cn.hongt.monitor.server.common.page.AbstractPageQuery;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
*
*/
@ApiModel(value="Linux监控配置接收数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinuxDeployInput extends AbstractPageQuery {

    /**
    * 服务器IP
    */
    @ApiModelProperty("服务器IP")
    private String ip;

    @ApiModelProperty("服务器名称")
    @TableField(value = "ip_name")
    private String ipName;

    @ApiModelProperty("数据类型 ：linux_cpu：CPU，linux_memory：内存，linux_disk：磁盘目录1")
    private String type;

    @ApiModelProperty("节点名称")
    private String nodeName;

}
