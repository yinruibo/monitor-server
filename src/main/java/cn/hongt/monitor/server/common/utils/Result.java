package cn.hongt.monitor.server.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author yrb
 * @date 2021/7/7
 * @Description: 用于ResultUtil
 */
@Data
@ApiModel(value = "通用返回模型")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

//    响应业务状态
    @ApiModelProperty(value = "响应业务状态")
    private String code;
//    响应消息
    @ApiModelProperty(value = "响应消息")
    private String msg;
//    响应中的数据
    @ApiModelProperty(value = "响应中的数据")
    private T data;
}
