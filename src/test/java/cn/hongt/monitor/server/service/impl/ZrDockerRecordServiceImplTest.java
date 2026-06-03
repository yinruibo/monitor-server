package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.common.utils.DateUtils;
import cn.hongt.monitor.server.dto.input.HardWareMonitorInput;
import cn.hongt.monitor.server.dto.output.HardresultOutput;
import cn.hongt.monitor.server.dto.output.NodeDockerOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.mapper.ZrDockerRecordEleMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZrDockerRecordServiceImplTest {

    @Mock
    private ZrDockerDeployMapper dockerDeployMapper;
    @Mock
    private ZrDockerRecordEleMapper dockerRecordEleMapper;

    private ZrDockerRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ZrDockerRecordServiceImpl();
        initTableInfo(ZrDockerRecordEleDO.class);
        initTableInfo(SysDockerDeployEleDO.class);
        ReflectionTestUtils.setField(service, "dockerDeployMapper", dockerDeployMapper);
        ReflectionTestUtils.setField(service, "baseMapper", dockerRecordEleMapper);
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
    void queryNodeDocker_shouldReturnEmptyResultWhenNoRecordTimestampExists() {
        SysDockerDeployEleDO deploy = SysDockerDeployEleDO.builder()
            .ip("10.0.0.1")
            .dockerName("app")
            .taskName("task")
            .type(WarnRecordEnum.docker_cpu.getCode())
            .isShow(0)
            .build();
        when(dockerDeployMapper.selectList(any())).thenReturn(Collections.singletonList(deploy));
        when(dockerRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrDockerRecordEleDO>emptyList());

        Result<List<NodeDockerOutput>> result = service.queryNodeDocker("node-a", "10.0.0.1");

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertNotNull(result.getData());
        assertEquals(0, result.getData().size());
    }

    @Test
    void queryDockerRecord_shouldReturnDynamicDisplayUnitForRateSeries() {
        HardWareMonitorInput input = HardWareMonitorInput.builder()
            .ip("10.0.0.1")
            .dockerName("app")
            .type(WarnRecordEnum.docker_network_up.getCode())
            .startTime("2026-04-18 00:00:06")
            .endTime("2026-04-18 00:00:20")
            .build();
        Date firstTime = DateUtils.stringToDate("2026-04-18 00:00:10", DateUtils.dateType5);
        Date secondTime = DateUtils.stringToDate("2026-04-18 00:00:15", DateUtils.dateType5);

        when(dockerRecordEleMapper.selectList(any())).thenReturn(
            Collections.singletonList(ZrDockerRecordEleDO.builder().createTime(secondTime).build()),
            java.util.Arrays.asList(
                ZrDockerRecordEleDO.builder().createTime(firstTime).dataRate("1024").unit("KB/s").build(),
                ZrDockerRecordEleDO.builder().createTime(secondTime).dataRate("2048").unit("KB/s").build()
            )
        );
        when(dockerDeployMapper.selectList(any())).thenReturn(
            Collections.singletonList(SysDockerDeployEleDO.builder().ip("10.0.0.1").isShow(0).build())
        );

        Result<List<HardresultOutput>> result = service.queryDockerRecord(input);

        assertNotNull(result);
        assertEquals("200", result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("MB/s", result.getData().get(0).getUnit());
        assertEquals(1D, result.getData().get(0).getMinimum());
        assertEquals(2D, result.getData().get(0).getMaximum());
        assertEquals(1.5D, result.getData().get(0).getAverage());
    }
}
