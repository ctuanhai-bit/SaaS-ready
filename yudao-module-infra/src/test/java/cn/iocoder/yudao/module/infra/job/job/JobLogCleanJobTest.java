package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.module.infra.service.job.JobLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobLogCleanJobTest {
    @Mock private JobLogService jobLogService;
    @InjectMocks private JobLogCleanJob job;

    @Test
    void usesSevenDaysAndBoundedBatches() {
        when(jobLogService.cleanJobLog(7, 1000)).thenReturn(1234);
        assertTrue(job.execute(null).contains("1234"));
        verify(jobLogService).cleanJobLog(7, 1000);
    }

    @Test
    void cleanupFailureRemainsAnExecutionFailure() {
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(jobLogService.cleanJobLog(7, 1000)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> job.execute(null)));
    }
}
