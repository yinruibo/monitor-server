package cn.hongt.monitor.server.dto.input;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 */
@ApiModel(value="Id列表数据")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdListInput {

    /**
     * Id列表
     */
    @ApiModelProperty("Id列表")
    private List<String> idList;

}
