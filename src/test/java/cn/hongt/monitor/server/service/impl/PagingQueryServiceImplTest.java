package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.dto.input.LinuxDeployInput;
import cn.hongt.monitor.server.dto.input.SysWarnDeployInput;
import cn.hongt.monitor.server.dto.output.SysWarnDeployOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import cn.hongt.monitor.server.mapper.SysWarnDeployMapper;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.mapper.ZrLinuxDeployMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagingQueryServiceImplTest {

    @Mock
    private SysWarnDeployMapper sysWarnDeployMapper;
    @Mock
    private ZrDockerDeployMapper zrDockerDeployMapper;
    @Mock
    private ZrLinuxDeployMapper zrLinuxDeployMapper;

    private SysWarnDeployServiceImpl sysWarnDeployService;
    private ZrDockerDeployServiceImpl zrDockerDeployService;
    private ZrLinuxDeployServiceImpl zrLinuxDeployService;

    @BeforeEach
    void setUp() {
        initTableInfo(SysWarnDeployDO.class);
        initTableInfo(SysDockerDeployEleDO.class);
        initTableInfo(SysLinuxDeployDO.class);

        sysWarnDeployService = new SysWarnDeployServiceImpl();
        ReflectionTestUtils.setField(sysWarnDeployService, "baseMapper", sysWarnDeployMapper);

        zrDockerDeployService = new ZrDockerDeployServiceImpl();
        ReflectionTestUtils.setField(zrDockerDeployService, "baseMapper", zrDockerDeployMapper);

        zrLinuxDeployService = new ZrLinuxDeployServiceImpl();
        ReflectionTestUtils.setField(zrLinuxDeployService, "baseMapper", zrLinuxDeployMapper);
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
    void queryWarnDepList_shouldUseDatabasePaging() {
        Page<SysWarnDeployDO> page = new Page<>(2, 3);
        page.setRecords(Collections.singletonList(SysWarnDeployDO.builder().warnId("warn-1").ip("10.0.0.1").build()));
        page.setTotal(11L);
        when(sysWarnDeployMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        SysWarnDeployInput input = SysWarnDeployInput.builder().ip("10.0.0.1").build();
        input.setPageNo(2);
        input.setPageSize(3);

        SysWarnDeployOutput output = sysWarnDeployService.queryWarnDepList(input);

        assertNotNull(output);
        assertEquals(1, output.getSysWarnDeployList().size());
        assertEquals(11, output.getTotal());
        verify(sysWarnDeployMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(sysWarnDeployMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void queryDockerDeployList_shouldUseDatabasePaging() {
        Page<SysDockerDeployEleDO> page = new Page<>(1, 2);
        page.setRecords(Collections.singletonList(SysDockerDeployEleDO.builder().id("docker-1").dockerName("app").sort(1).build()));
        page.setTotal(5L);
        when(zrDockerDeployMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        DockerDeployInput input = DockerDeployInput.builder().ip("10.0.0.1").build();
        input.setPageNo(1);
        input.setPageSize(2);

        Map<String, Object> result = zrDockerDeployService.queryDeployList(input);

        assertNotNull(result);
        assertEquals(5L, ((Number) result.get("total")).longValue());
        assertEquals(1, ((List<?>) result.get("data")).size());
        verify(zrDockerDeployMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(zrDockerDeployMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void queryLinuxDeployList_shouldUseDatabasePaging() {
        Page<SysLinuxDeployDO> page = new Page<>(3, 4);
        page.setRecords(Collections.singletonList(SysLinuxDeployDO.builder().id("linux-1").ip("10.0.0.2").sort(2).build()));
        page.setTotal(9L);
        when(zrLinuxDeployMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        LinuxDeployInput input = LinuxDeployInput.builder().ip("10.0.0.2").build();
        input.setPageNo(3);
        input.setPageSize(4);

        Map<String, Object> result = zrLinuxDeployService.queryDeployList(input);

        assertNotNull(result);
        assertEquals(9L, ((Number) result.get("total")).longValue());
        assertEquals(1, ((List<?>) result.get("data")).size());
        verify(zrLinuxDeployMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(zrLinuxDeployMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }
}
