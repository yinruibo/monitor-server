package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.InsertSysWarnDeployInput;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import cn.hongt.monitor.server.mapper.SysWarnDeployMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysWarnDeployServiceImplTest {

    @Mock
    private SysWarnDeployMapper sysWarnDeployMapper;

    private SysWarnDeployServiceImpl service;

    @BeforeEach
    void setUp() {
        initTableInfo(SysWarnDeployDO.class);
        service = new SysWarnDeployServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", sysWarnDeployMapper);
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
    void insertWarnDep_shouldInsertOnlyOnceWhenInvokedConcurrently() throws Exception {
        AtomicInteger activeSelects = new AtomicInteger();
        AtomicBoolean overlapDetected = new AtomicBoolean(false);
        AtomicBoolean inserted = new AtomicBoolean(false);
        when(sysWarnDeployMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            int active = activeSelects.incrementAndGet();
            if (active > 1) {
                overlapDetected.set(true);
            }
            try {
                Thread.sleep(50L);
                if (overlapDetected.get()) {
                    return null;
                }
                if (inserted.get()) {
                    return SysWarnDeployDO.builder()
                            .warnId("existing")
                            .ip("10.0.0.1")
                            .warnType("linux_cpu")
                            .status(0)
                            .build();
                }
                return null;
            } finally {
                activeSelects.decrementAndGet();
            }
        });
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenAnswer(invocation -> {
            inserted.set(true);
            return 1;
        });

        InsertSysWarnDeployInput input = InsertSysWarnDeployInput.builder()
                .ip("10.0.0.1")
                .warnType("linux_cpu")
                .thresholdSign(">")
                .minThresholdNum(50)
                .middleThresholdNum(70)
                .maxThresholdNum(80)
                .news(0)
                .build();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Result<String>> first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return service.insertWarnDep(input);
            });
            Future<Result<String>> second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return service.insertWarnDep(input);
            });

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            Result<String> firstResult = first.get(3, TimeUnit.SECONDS);
            Result<String> secondResult = second.get(3, TimeUnit.SECONDS);
            assertNotNull(firstResult);
            assertNotNull(secondResult);
        } finally {
            executor.shutdownNow();
        }

        verify(sysWarnDeployMapper, times(1)).insert(any(SysWarnDeployDO.class));
    }
}
