package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.service.DockerMetricCollectorService;
import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DockerCornJobTest {

    @Mock
    private ZrWarnRecordEleService zrWarnRecordEleService;
    @Mock
    private DockerMetricCollectorService dockerMetricCollectorService;

    private DockerCornJob dockerCornJob;
    private String originalOsName;

    @BeforeEach
    void setUp() {
        dockerCornJob = new DockerCornJob();
        ReflectionTestUtils.setField(dockerCornJob, "zrWarnRecordEleService", zrWarnRecordEleService);
        ReflectionTestUtils.setField(dockerCornJob, "dockerMetricCollectorService", dockerMetricCollectorService);
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    void tearDown() {
        if (originalOsName == null) {
            System.clearProperty("os.name");
        } else {
            System.setProperty("os.name", originalOsName);
        }
    }

    @Test
    void warnDockerBySchCpuMemory_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = dockerCornJob.warnDockerBySchCpuMemory();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnDockerBySchCpuMemory();
    }

    @Test
    void warnDockerBySchCpuMemory_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = dockerCornJob.warnDockerBySchCpuMemory();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnDockerBySchCpuMemory();
    }

    @Test
    void warnDockerBySchDiskIO_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = dockerCornJob.warnDockerBySchDiskIO();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnDockerIOBySch();
    }

    @Test
    void warnDockerBySchDiskIO_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = dockerCornJob.warnDockerBySchDiskIO();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnDockerIOBySch();
    }

    @Test
    void warnDockerBySchNetwork_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = dockerCornJob.warnDockerBySchNetwork();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnDockerNetWorkBySch();
    }

    @Test
    void warnDockerBySchNetwork_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = dockerCornJob.warnDockerBySchNetwork();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnDockerNetWorkBySch();
    }

    @Test
    void refreshDockerSubscriptions_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = dockerCornJob.refreshDockerSubscriptions();

        assertNotNull(result);
        verify(dockerMetricCollectorService, never()).refreshContainerSubscriptions();
    }

    @Test
    void refreshDockerSubscriptions_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = dockerCornJob.refreshDockerSubscriptions();

        assertNotNull(result);
        verify(dockerMetricCollectorService).refreshContainerSubscriptions();
    }
}
