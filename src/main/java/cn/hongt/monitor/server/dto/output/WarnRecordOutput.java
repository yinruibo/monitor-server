package cn.hongt.monitor.server.dto.output;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@ApiModel(value="告警监控返回信息表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarnRecordOutput {

    /**
     * 主键Id
     */
    @ApiModelProperty(value="告警记录Id")
    private String id;

    /**
     * 多服务IP
     */
    private String ip;

    /**
     * 告警标志： CPU告警，内存告警，磁盘告警  地面气象观测资料，实况观测资料，空间天气观测资料，水文观测资料，卫星雷达观测资料，数值预报资料等
     */
    private String warnSign;

    /**
     * 告警类型:0：CPU告警，1：内存告警，2：磁盘告警
     */
    private Integer warnType;

    /**
     * 告警级别：0：一般，1：严重，2：非常严重
     */
    private Integer grade;

    /**
     * 告警状态：0：运行中，1：已完成
     */
    private Integer status;

    /**
     * 告警说明
     */
    private String explain;

    /**
     * 告警进程：0：汇集，1：解析，2：入库
     */
    private Integer process;

    /**
     * 持续时间
     */
    private String continuedTime;

    /**
     * 首次告警时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
