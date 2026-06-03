package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.common.utils.Result;
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
class LinuxCornJobTest {

    @Mock
    private ZrWarnRecordEleService zrWarnRecordEleService;

    private LinuxCornJob linuxCornJob;
    private String originalOsName;

    @BeforeEach
    void setUp() {
        linuxCornJob = new LinuxCornJob();
        ReflectionTestUtils.setField(linuxCornJob, "zrWarnRecordEleService", zrWarnRecordEleService);
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
    void warnLinuxCPUByScheduled_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = linuxCornJob.warnLinuxCPUByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnLinuxCPUBySch();
    }

    @Test
    void warnLinuxCPUByScheduled_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = linuxCornJob.warnLinuxCPUByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnLinuxCPUBySch();
    }

    @Test
    void warnLinuxMemoryByScheduled_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = linuxCornJob.warnLinuxMemoryByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnLinuxMemoryBySch();
    }

    @Test
    void warnLinuxMemoryByScheduled_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = linuxCornJob.warnLinuxMemoryByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnLinuxMemoryBySch();
    }

    @Test
    void warnLinuxDiskByScheduled_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = linuxCornJob.warnLinuxDiskByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnLinuxDiskBySch();
    }

    @Test
    void warnLinuxDiskByScheduled_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = linuxCornJob.warnLinuxDiskByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnLinuxDiskBySch();
    }

    @Test
    void warnLinuxIOByScheduled_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = linuxCornJob.warnLinuxIOByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnLinuxIOBySch();
    }

    @Test
    void warnLinuxIOByScheduled_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = linuxCornJob.warnLinuxIOByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnLinuxIOBySch();
    }

    @Test
    void warnLinuxNetWorkByScheduled_shouldSkipOnWindows() {
        System.setProperty("os.name", "Windows 10");

        Result result = linuxCornJob.warnLinuxNetWorkByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService, never()).warnLinuxNetWorkBySch();
    }

    @Test
    void warnLinuxNetWorkByScheduled_shouldCallServiceOnLinux() {
        System.setProperty("os.name", "Linux");

        Result result = linuxCornJob.warnLinuxNetWorkByScheduled();

        assertNotNull(result);
        verify(zrWarnRecordEleService).warnLinuxNetWorkBySch();
    }
}
