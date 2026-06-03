package cn.hongt.monitor.server.common.page;

import cn.hongt.monitor.server.common.utils.ParamUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: 分页查询
 */
@Data
@ApiModel(description = "分页查询")
public abstract class AbstractPageQuery {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 1024;

    @ApiModelProperty(value = "请求页码-默认1，从1开始",required = true)
    private int pageNo = 1;

    @ApiModelProperty( value = "每页条数-默认20，最大1024",required = true)
    private int pageSize = 20;

    public AbstractPageQuery() {
    }

    public AbstractPageQuery(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public void checkInput() {
        ParamUtils.expectTrue(this.pageNo >= 1, String.format("请求页码不能小于%d", 1));
        ParamUtils.expectInRange(this.pageSize, 1, 1024, String.format("每页条数需在[%d, %d]范围内", 1, 1024));
    }
}
