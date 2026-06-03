package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.dto.input.ZrWarnRecordListInput;
import cn.hongt.monitor.server.dto.output.WarnFaultOutput;
import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * @author yrb
 * @date 2021/8/18
 * @Description: 告警监控服务
 */
@Api(tags = "告警监控服务")
@RestController
@RequestMapping("/warn")
public class ZrWarnRecordEleController {

    @Autowired
    private ZrWarnRecordEleService zrWarnRecordEleService;

    // 查询告警数据 ，即数据告警时间 查询时间范围内 所有告警数据即可
    @PostMapping("/queryWarnRecordList")
    @ApiOperation(value = "查询告警日志列表", httpMethod = "POST")
    public Result queryWarnDepList(@Validated @RequestBody ZrWarnRecordListInput zrWarnRecordListInput) {
        return ResultUtil.success(zrWarnRecordEleService.queryWarnDepList(zrWarnRecordListInput));
    }

    @GetMapping("/deleteWarnRecord")  // 清除单个/所有
    @ApiOperation(value = "删除告警日志", httpMethod = "GET")
    public Result deleteWarnRecord(@RequestParam("ids") List<String> ids) {
        zrWarnRecordEleService.deleteWarnRecord(ids);
        return ResultUtil.success();
    }

    @PostMapping("/queryWarnSignList")
    @ApiOperation(value = "查询告警日志分布饼状图", httpMethod = "POST")
    public Result queryWarnSignList(@Validated @RequestBody ZrWarnRecordListInput input) {
        return ResultUtil.success(zrWarnRecordEleService.queryWarnSignList(input));
    }

    //todo 可以通过 查询指定时间段的 告警数据列表，展示折线图，体现告警变化情况

    // 查询每天的告警次数；
    @PostMapping("/queryFaultList")
    @ApiOperation(value = "查询当前故障趋势", httpMethod = "POST")
    public Result<List<WarnFaultOutput>> queryFaultList(@Validated @RequestBody ZrWarnRecordListInput input) {
        return zrWarnRecordEleService.queryFaultList(input);
    }

    @GetMapping("/queryExport")
    @ApiOperation(value = "导出Excel表格", httpMethod = "GET")
    public Result<String> queryExport(@RequestParam("idList") List<String> idList) {
        return zrWarnRecordEleService.queryExport(idList);
    }

    @GetMapping("/queryTimingUpdate")
    @ApiOperation(value = "更新告警日志列表的持续时间-不对前端开放", httpMethod = "GET")
    public Result<String> queryTimingUpdate() {
        return zrWarnRecordEleService.queryTimingUpdate();
    }

    @GetMapping("/queryWarnNumber")
    @ApiOperation(value = "查询当日告警数量", httpMethod = "GET")
    public Result<Integer> queryWarnNumber(@RequestParam("ip") String ip) {
        return zrWarnRecordEleService.queryWarnNumber(ip);
    }

}
