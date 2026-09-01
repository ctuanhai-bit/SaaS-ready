package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.module.infra.service.job.JobService;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.Mockito.*;

class JobSchedulerInitializerTest {

    @Test
    void synchronizesJobsWhenQuartzIsEnabled() throws Exception {
        ObjectProvider<Scheduler> provider = schedulerProvider(mock(Scheduler.class));
        JobService jobService = mock(JobService.class);
        JobSchedulerInitializer initializer = initializer(provider, jobService);

        initializer.initialize();

        verify(jobService).syncJob();
    }

    @Test
    void doesNothingWhenQuartzIsDisabled() throws Exception {
        ObjectProvider<Scheduler> provider = schedulerProvider(null);
        JobService jobService = mock(JobService.class);
        JobSchedulerInitializer initializer = initializer(provider, jobService);

        initializer.initialize();

        verifyNoInteractions(jobService);
    }

    private JobSchedulerInitializer initializer(ObjectProvider<Scheduler> provider, JobService jobService) {
        JobSchedulerInitializer initializer = new JobSchedulerInitializer();
        ReflectionTestUtils.setField(initializer, "schedulerProvider", provider);
        ReflectionTestUtils.setField(initializer, "jobService", jobService);
        return initializer;
    }

    private ObjectProvider<Scheduler> schedulerProvider(Scheduler scheduler) {
        StaticListableBeanFactory beanFactory = scheduler == null
                ? new StaticListableBeanFactory()
                : new StaticListableBeanFactory(Collections.singletonMap("scheduler", scheduler));
        return beanFactory.getBeanProvider(Scheduler.class);
    }
}
