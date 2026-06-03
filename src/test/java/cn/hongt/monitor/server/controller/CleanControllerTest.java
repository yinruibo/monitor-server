package cn.hongt.monitor.server.controller;

import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO;
import cn.hongt.monitor.server.entity.ZrWarnRecordEleDO;
import cn.hongt.monitor.server.mapper.ZrDockerRecordEleMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxRecordEleMapper;
import cn.hongt.monitor.server.mapper.ZrWarnRecordEleMapper;
import cn.hongt.monitor.server.schedule.CleanSchJob;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanControllerTest {

    @Mock
    private ZrWarnRecordEleMapper warnRecordEleMapper;
    @Mock
    private ZrLinuxRecordEleMapper linuxRecordEleMapper;
    @Mock
    private ZrDockerRecordEleMapper dockerRecordEleMapper;

    private CleanController controller;

    @BeforeEach
    void setUp() {
        initTableInfo(ZrWarnRecordEleDO.class);
        initTableInfo(ZrLinuxRecordEleDO.class);
        initTableInfo(ZrDockerRecordEleDO.class);
        controller = new CleanController();
        CleanSchJob cleanSchJob = new CleanSchJob();
        ReflectionTestUtils.setField(cleanSchJob, "warnRecordEleMapper", warnRecordEleMapper);
        ReflectionTestUtils.setField(cleanSchJob, "linuxRecordEleMapper", linuxRecordEleMapper);
        ReflectionTestUtils.setField(cleanSchJob, "dockerRecordEleMapper", dockerRecordEleMapper);
        ReflectionTestUtils.setField(controller, "cleanSchJob", cleanSchJob);
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
    void cleanWarnRecord_shouldUseLatestSingleRecordAsCutoffBase() {
        when(warnRecordEleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ZrWarnRecordEleDO.builder().id("warn-1").createTime(new Date()).build());

        controller.cleanWarnRecord();

        verify(warnRecordEleMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(warnRecordEleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        ArgumentCaptor<LambdaQueryWrapper<ZrWarnRecordEleDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(warnRecordEleMapper).delete(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void cleanLinuxRecord_shouldUseLatestSingleRecordAsCutoffBase() {
        when(linuxRecordEleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ZrLinuxRecordEleDO.builder().id("linux-1").createTime(new Date()).build());

        controller.cleanLinuxRecord();

        verify(linuxRecordEleMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(linuxRecordEleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        ArgumentCaptor<LambdaQueryWrapper<ZrLinuxRecordEleDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(linuxRecordEleMapper).delete(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void cleanDockerRecord_shouldUseLatestSingleRecordAsCutoffBase() {
        when(dockerRecordEleMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(ZrDockerRecordEleDO.builder().id("docker-1").createTime(new Date()).build());

        controller.cleanDockerRecord();

        verify(dockerRecordEleMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(dockerRecordEleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        ArgumentCaptor<LambdaQueryWrapper<ZrDockerRecordEleDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(dockerRecordEleMapper).delete(captor.capture());
        assertNotNull(captor.getValue());
    }
}
