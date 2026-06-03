package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.DateUtils;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.ZrLinuxDeployMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxRecordEleMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZrLinuxRecordServiceImplTest {

    @Mock
    private ZrLinuxDeployMapper linuxDeployMapper;
    @Mock
    private ZrLinuxRecordEleMapper linuxRecordEleMapper;

    private ZrLinuxRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ZrLinuxRecordServiceImpl();
        initTableInfo(ZrLinuxRecordEleDO.class);
        initTableInfo(SysLinuxDeployDO.class);
        ReflectionTestUtils.setField(service, "linuxDeployMapper", linuxDeployMapper);
        ReflectionTestUtils.setField(service, "baseMapper", linuxRecordEleMapper);
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName());
        builderAssistant.setCurrentNamespace(entityClass.getName());
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    @Test
    void queryServerRecord_shouldReturnDynamicDisplayUnitForRateSeries() {
        HardWareMonitorInput input = HardWareMonitorInput.builder()
            .ip("10.0.0.1")
            .type(WarnRecordEnum.linux_IO_read.getCode())
            .startTime("2026-04-18 00:00:06")
            .endTime("2026-04-18 00:00:20")
            .build();
        Date firstTime = DateUtils.stringToDate("2026-04-18 00:00:10", DateUtils.dateType5);
        Date secondTime = DateUtils.stringToDate("2026-04-18 00:00:15", DateUtils.dateType5);

        when(linuxRecordEleMapper.selectList(any())).thenReturn(
            Collections.singletonList(ZrLinuxRecordEleDO.builder().createTime(secondTime).build()),
            Arrays.asList(
                ZrLinuxRecordEleDO.builder().createTime(firstTime).dataRate("1024").unit("KB/s").build(),
                ZrLinuxRecordEleDO.builder().createTime(secondTime).dataRate("2048").unit("KB/s").build()
            )
        );
        when(linuxDeployMapper.selectList(any())).thenReturn(
            Collections.singletonList(SysLinuxDeployDO.builder().ip("10.0.0.1").isShow(0).build())
        );

        List<HardresultOutput> result = service.queryServerRecord(input);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MB/s", result.get(0).getUnit());
        assertEquals(1D, result.get(0).getMinimum());
        assertEquals(2D, result.get(0).getMaximum());
        assertEquals(1.5D, result.get(0).getAverage());
    }
}
