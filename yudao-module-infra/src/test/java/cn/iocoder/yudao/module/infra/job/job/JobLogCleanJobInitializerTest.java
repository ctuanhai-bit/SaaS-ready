package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class JobLogCleanJobInitializerTest {
    private Scheduler scheduler;
    private JobMapper jobMapper;
    private JobService jobService;
    private JobLogCleanJobInitializer initializer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws SchedulerException {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "log-retention-test-" + System.nanoTime());
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        scheduler = new StdSchedulerFactory(properties).getScheduler();
        ObjectProvider<Scheduler> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(scheduler);
        jobMapper = mock(JobMapper.class);
        jobService = mock(JobService.class);
        initializer = new JobLogCleanJobInitializer();
        ReflectionTestUtils.setField(initializer, "schedulerProvider", provider);
        ReflectionTestUtils.setField(initializer, "schedulerManager", new SchedulerManager(scheduler));
        ReflectionTestUtils.setField(initializer, "jobMapper", jobMapper);
        ReflectionTestUtils.setField(initializer, "jobService", jobService);
    }

    @AfterEach
    void close() throws SchedulerException {
        scheduler.shutdown(true);
    }

    @Test
    void registersOnlyCleanupAndIsRepeatable() throws SchedulerException {
        JobDO job = cleanupJob(1);
        when(jobMapper.selectByHandlerName(JobLogCleanJobInitializer.HANDLER)).thenReturn(job);
        initializer.initialize();
        initializer.initialize();
        JobKey key = new JobKey(JobLogCleanJobInitializer.HANDLER);
        TriggerKey trigger = new TriggerKey(JobLogCleanJobInitializer.HANDLER);
        assertTrue(scheduler.checkExists(key));
        assertEquals(27L, scheduler.getJobDetail(key).getJobDataMap().getLong("JOB_ID"));
        assertEquals(Trigger.TriggerState.NORMAL, scheduler.getTriggerState(trigger));
        assertEquals(JobLogCleanJobInitializer.DEFAULT_CRON, ((CronTrigger) scheduler.getTrigger(trigger)).getCronExpression());
        verifyNoInteractions(jobService);
    }

    @Test
    void respectsAdministratorPauseOnRestart() throws SchedulerException {
        when(jobMapper.selectByHandlerName(JobLogCleanJobInitializer.HANDLER)).thenReturn(cleanupJob(2));
        initializer.initialize();
        assertFalse(scheduler.checkExists(new JobKey(JobLogCleanJobInitializer.HANDLER)));
        verifyNoInteractions(jobService);
    }

    @Test
    void createsDefaultTaskOnNewInstallation() throws SchedulerException {
        initializer.initialize();
        verify(jobService).createJob(argThat(req -> JobLogCleanJobInitializer.HANDLER.equals(req.getHandlerName())
                && JobLogCleanJobInitializer.DEFAULT_CRON.equals(req.getCronExpression()) && req.getRetryCount() == 0));
    }

    @Test
    void repairsMissingTriggerForCleanupOnly() throws SchedulerException {
        when(jobMapper.selectByHandlerName(JobLogCleanJobInitializer.HANDLER)).thenReturn(cleanupJob(1));
        scheduler.addJob(JobBuilder.newJob(NoOpJob.class).withIdentity(JobLogCleanJobInitializer.HANDLER).storeDurably().build(), false);
        initializer.initialize();
        assertTrue(scheduler.checkExists(new TriggerKey(JobLogCleanJobInitializer.HANDLER)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void doesNothingWhenQuartzIsDisabled() {
        ReflectionTestUtils.setField(initializer, "schedulerProvider", mock(ObjectProvider.class));
        initializer.initialize();
        verifyNoInteractions(jobMapper, jobService);
    }

    private JobDO cleanupJob(int status) {
        return JobDO.builder().id(27L).handlerName(JobLogCleanJobInitializer.HANDLER).name("log cleanup")
                .status(status).cronExpression(JobLogCleanJobInitializer.DEFAULT_CRON).retryCount(0).retryInterval(0).build();
    }

    public static class NoOpJob implements Job {
        @Override public void execute(JobExecutionContext context) { }
    }
}
