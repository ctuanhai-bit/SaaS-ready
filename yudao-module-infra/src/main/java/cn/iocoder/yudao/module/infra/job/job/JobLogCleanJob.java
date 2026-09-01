package cn.iocoder.yudao.module.infra.job.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.infra.service.job.JobLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * 物理删除 N 天前的任务日志的 Job
 *
 * @author j-sentinel
 */
@Slf4j
@Component
public class JobLogCleanJob implements JobHandler {

    @Resource
    private JobLogService jobLogService;

    /**
     * 失败日志保留 7 天；历史成功日志一并清理。
     */
    private static final Integer JOB_CLEAN_RETAIN_DAY = 7;

    /**
     * 每次删除间隔的条数，如果值太高可能会造成数据库的压力过大
     */
    private static final Integer DELETE_LIMIT = 1000;

    @Override
    @TenantIgnore
    public String execute(String param) {
        Integer count = jobLogService.cleanJobLog(JOB_CLEAN_RETAIN_DAY, DELETE_LIMIT);
        log.info("[execute][定时执行清理定时任务日志数量 ({}) 个]", count);
        return String.format("定时执行清理定时任务日志数量 %s 个", count);
    }

}
