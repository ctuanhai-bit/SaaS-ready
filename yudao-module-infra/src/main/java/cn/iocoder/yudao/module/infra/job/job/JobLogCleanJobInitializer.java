package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/** Registers only the log retention task; other job states are never synchronized here. */
@Component
@Slf4j
public class JobLogCleanJobInitializer {

    static final String HANDLER = "jobLogCleanJob";
    static final String DEFAULT_CRON = "0 0/10 * * * ?";

    @Resource
    private ObjectProvider<Scheduler> schedulerProvider;
    @Resource
    private SchedulerManager schedulerManager;
    @Resource
    private JobMapper jobMapper;
    @Resource
    private JobService jobService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Scheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            return;
        }
        try {
            JobDO job = jobMapper.selectByHandlerName(HANDLER);
            if (job == null) {
                jobService.createJob(new JobSaveReqVO().setName("任务日志清理").setHandlerName(HANDLER)
                        .setCronExpression(DEFAULT_CRON).setRetryCount(0).setRetryInterval(0).setMonitorTimeout(0));
                return;
            }
            if (!Objects.equals(job.getStatus(), JobStatusEnum.NORMAL.getStatus())) {
                return;
            }
            if (!scheduler.checkExists(new JobKey(HANDLER)) || !scheduler.checkExists(new TriggerKey(HANDLER))) {
                schedulerManager.deleteJob(HANDLER);
                schedulerManager.addJob(job.getId(), HANDLER, job.getHandlerParam(), job.getCronExpression(),
                        job.getRetryCount(), job.getRetryInterval());
            } else {
                schedulerManager.updateJob(HANDLER, job.getHandlerParam(), job.getCronExpression(),
                        job.getRetryCount(), job.getRetryInterval());
            }
            schedulerManager.resumeJob(HANDLER);
        } catch (Exception ex) {
            log.error("[initialize][任务日志清理调度注册失败]", ex);
        }
    }
}
