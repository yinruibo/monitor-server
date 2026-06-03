package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.dto.input.DockerDeployInput;
import cn.hongt.monitor.server.dto.output.DockerImagesOutput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZrDockerDeployServiceImplTest {

    @Mock
    private ZrDockerDeployMapper dockerDeployMapper;

    private ZrDockerDeployServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(SysDockerDeployEleDO.class);
        service = new ZrDockerDeployServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", dockerDeployMapper);
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
    void queryNodeList_shouldReturnDistinctNodeNames() {
        List<SysDockerDeployEleDO> mockList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").nodeName("node1").build(),
                SysDockerDeployEleDO.builder().id("2").nodeName("node1").build(),
                SysDockerDeployEleDO.builder().id("3").nodeName("node2").build()
        );
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<String> result = service.queryNodeList();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("node1"));
        assertTrue(result.contains("node2"));
    }

    @Test
    void queryNodeList_shouldHandleEmptyList() {
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<String> result = service.queryNodeList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void queryNodeAndServers_shouldReturnServersForNode() {
        String nodeName = "node1";
        List<SysDockerDeployEleDO> mockList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").nodeName("node1").ip("192.168.1.1").build(),
                SysDockerDeployEleDO.builder().id("2").nodeName("node1").ip("192.168.1.2").build()
        );
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<SysDockerDeployEleDO> result = service.queryNodeAndServers(nodeName);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(dockerDeployMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void queryServerList_shouldReturnDistinctServers() {
        List<SysDockerDeployEleDO> mockList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").ip("192.168.1.1").build(),
                SysDockerDeployEleDO.builder().id("2").ip("192.168.1.2").build()
        );
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<SysDockerDeployEleDO> result = service.queryServerList();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void queryDockerNameList_shouldReturnDockerImages() {
        String nodeName = "node1";
        String ip = "192.168.1.1";
        List<SysDockerDeployEleDO> mockList = Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("2").dockerName("app2").build()
        );
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockList);

        List<DockerImagesOutput> result = service.queryDockerNameList(nodeName, ip);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void queryDockerNameList_shouldHandleEmptyList() {
        String nodeName = "node1";
        String ip = "192.168.1.1";
        when(dockerDeployMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<DockerImagesOutput> result = service.queryDockerNameList(nodeName, ip);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void queryDeployList_shouldReturnPagedData() {
        DockerDeployInput input = new DockerDeployInput();
        input.setPageNo(1);
        input.setPageSize(10);

        Page<SysDockerDeployEleDO> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(
                SysDockerDeployEleDO.builder().id("1").dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("2").dockerName("app2").build()
        ));
        page.setTotal(2L);
        when(dockerDeployMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Map<String, Object> result = service.queryDeployList(input);

        assertNotNull(result);
        assertEquals(2L, result.get("total"));
        assertNotNull(result.get("data"));
    }

    @Test
    void queryDeployList_shouldHandleEmptyPage() {
        DockerDeployInput input = new DockerDeployInput();
        input.setPageNo(1);
        input.setPageSize(10);

        Page<SysDockerDeployEleDO> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);
        when(dockerDeployMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Map<String, Object> result = service.queryDeployList(input);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void updateDeployList_shouldHandleEmptyList() {
        List<SysDockerDeployEleDO> inputList = Collections.emptyList();

        // 空列表不应抛出异常
        service.updateDeployList(inputList);
    }

    @Test
    void updateDeployList_shouldSkipNullId() {
        List<SysDockerDeployEleDO> inputList = Arrays.asList(
                SysDockerDeployEleDO.builder().id(null).dockerName("app1").build(),
                SysDockerDeployEleDO.builder().id("").dockerName("app2").build()
        );

        // 空ID应被过滤，不应抛出异常
        service.updateDeployList(inputList);
    }

    @Test
    void deleteDeployList_shouldCallBatchDelete() {
        List<String> idList = Arrays.asList("1", "2", "3");

        service.deleteDeployList(idList);

        verify(dockerDeployMapper).deleteBatchIds(idList);
    }

    @Test
    void deleteDeployList_shouldHandleEmptyList() {
        List<String> idList = Collections.emptyList();

        service.deleteDeployList(idList);

        verify(dockerDeployMapper).deleteBatchIds(idList);
    }
}
