package cn.hongt.monitor.server.schedule;

import cn.hongt.monitor.server.service.ZrWarnRecordEleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Win10CronJobTest {

    @Mock
    private ZrWarnRecordEleService zrWarnRecordEleService;

    private Win10CronJob cronJob;
    private String originalOsName;

    @BeforeEach
    void setUp() {
        cronJob = new Win10CronJob();
        ReflectionTestUtils.setField(cronJob, "zrWarnRecordEleService", zrWarnRecordEleService);
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
    void warnWinCPUByScheduled_shouldSkipWhenOperatingSystemIsNotWindows() {
        System.setProperty("os.name", "Mac OS X");

        cronJob.warnWinCPUByScheduled();

        verify(zrWarnRecordEleService, never()).warnWinCPUBySch();
    }
}
