package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker;
import cn.iocoder.yudao.framework.quartz.core.service.JobLogFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJobAspect;
import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobHandlerInvokerTest {
    @Mock private ApplicationContext applicationContext;
    @Mock private JobLogFrameworkService jobLogFrameworkService;
    @Mock private JobHandler handler;
    @Mock private JobExecutionContext context;
    @InjectMocks private TestInvoker invoker;
    private JobDataMap data;

    @BeforeEach
    void setup() {
        data = new JobDataMap();
        data.put(JobDataKeyEnum.JOB_ID.name(), 5L);
        data.put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "payNotifyJob");
        data.put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), "param");
        data.put(JobDataKeyEnum.JOB_RETRY_COUNT.name(), 1);
        data.put(JobDataKeyEnum.JOB_RETRY_INTERVAL.name(), 0);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(applicationContext.getBean("payNotifyJob", JobHandler.class)).thenReturn(handler);
    }

    @Test
    void successDoesNotWriteAnyLog() throws Exception {
        when(handler.execute("param")).thenReturn("no pending notifications");
        invoker.run(context);
        verify(handler).execute("param");
        verifyNoInteractions(jobLogFrameworkService);
    }

    @Test
    void failureIsLoggedAndRetried() throws Exception {
        IllegalStateException failure = new IllegalStateException("upstream unavailable");
        when(handler.execute("param")).thenThrow(failure);
        JobExecutionException thrown = assertThrows(JobExecutionException.class, () -> invoker.run(context));
        assertTrue(thrown.refireImmediately());
        assertSame(failure, thrown.getCause());
        verify(jobLogFrameworkService).recordJobFailureAsync(eq(5L), any(), eq("payNotifyJob"), eq("param"),
                eq(1), any(), intThat(duration -> duration >= 0), contains("upstream unavailable"));
    }

    @Test
    void retryLimitPreservesFailureAndExecutionIndex() throws Exception {
        when(context.getRefireCount()).thenReturn(1);
        when(handler.execute("param")).thenThrow(new IllegalStateException("still unavailable"));
        JobExecutionException thrown = assertThrows(JobExecutionException.class, () -> invoker.run(context));
        assertFalse(thrown.refireImmediately());
        verify(jobLogFrameworkService).recordJobFailureAsync(eq(5L), any(), eq("payNotifyJob"), eq("param"),
                eq(2), any(), anyInt(), contains("still unavailable"));
    }

    @Test
    void loggingFailureDoesNotReplaceTheBusinessFailure() throws Exception {
        IllegalStateException failure = new IllegalStateException("handler failed");
        when(handler.execute("param")).thenThrow(failure);
        doThrow(new IllegalStateException("log queue full")).when(jobLogFrameworkService)
                .recordJobFailureAsync(anyLong(), any(), anyString(), any(), anyInt(), any(), anyInt(), any());
        JobExecutionException thrown = assertThrows(JobExecutionException.class, () -> invoker.run(context));
        assertSame(failure, thrown.getCause());
        assertTrue(thrown.refireImmediately());
    }

    @Test
    void successfulRetryDoesNotEraseEarlierFailure() throws Exception {
        when(handler.execute("param")).thenThrow(new IllegalStateException("temporary failure")).thenReturn("ok");
        assertThrows(JobExecutionException.class, () -> invoker.run(context));
        when(context.getRefireCount()).thenReturn(1);
        invoker.run(context);
        verify(jobLogFrameworkService, times(1)).recordJobFailureAsync(anyLong(), any(), anyString(), any(),
                eq(1), any(), anyInt(), contains("temporary failure"));
        verifyNoMoreInteractions(jobLogFrameworkService);
    }

    @Test
    void tenantFailureIsLoggedWithoutReplayingSuccessfulTenants() throws Throwable {
        TenantFrameworkService tenants = mock(TenantFrameworkService.class);
        when(tenants.getTenantIds()).thenReturn(Arrays.asList(1L, 9018L));
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        Set<Long> executed = ConcurrentHashMap.newKeySet();
        when(invocation.proceed()).thenAnswer(call -> {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            executed.add(tenantId);
            if (tenantId == 9018L) throw new IllegalStateException("upstream timeout");
            return "ok";
        });
        TenantJobAspect aspect = new TenantJobAspect(tenants);
        when(handler.execute("param")).thenAnswer(call -> aspect.around(invocation, mock(TenantJob.class)));
        JobExecutionException failure = assertThrows(JobExecutionException.class, () -> invoker.run(context));
        assertFalse(failure.refireImmediately());
        assertEquals(2, executed.size());
        verify(invocation, times(2)).proceed();
        verify(jobLogFrameworkService).recordJobFailureAsync(eq(5L), any(), eq("payNotifyJob"), eq("param"),
                eq(1), any(), anyInt(), argThat(reason -> reason.contains("9018") && reason.contains("upstream timeout")));
    }

    @Test
    void successfulTenantBatchDoesNotWriteLogs() throws Throwable {
        TenantFrameworkService tenants = mock(TenantFrameworkService.class);
        when(tenants.getTenantIds()).thenReturn(Arrays.asList(1L, 9018L));
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        when(invocation.proceed()).thenReturn("ok");
        TenantJobAspect aspect = new TenantJobAspect(tenants);
        when(handler.execute("param")).thenAnswer(call -> aspect.around(invocation, mock(TenantJob.class)));
        invoker.run(context);
        verify(invocation, times(2)).proceed();
        verifyNoInteractions(jobLogFrameworkService);
    }

    static class TestInvoker extends JobHandlerInvoker {
        void run(JobExecutionContext context) throws JobExecutionException {
            executeInternal(context);
        }
    }
}
