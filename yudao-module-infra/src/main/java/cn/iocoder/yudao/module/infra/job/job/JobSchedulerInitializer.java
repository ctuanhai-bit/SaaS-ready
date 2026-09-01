package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.module.infra.service.job.JobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/** Synchronizes enabled database jobs into the in-memory Quartz scheduler. */
@Component
@Slf4j
public class JobSchedulerInitializer {

    @Resource
    private ObjectProvider<Scheduler> schedulerProvider;
    @Resource
    private JobService jobService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        if (schedulerProvider.getIfAvailable() == null) {
            return;
        }
        try {
            jobService.syncJob();
        } catch (Exception ex) {
            log.error("[initialize][database job synchronization failed]", ex);
        }
    }
}
