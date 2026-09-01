package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobLogDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobLogMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobLogStatusEnum;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Job 日志 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class JobLogServiceImpl implements JobLogService {

    @Resource
    private JobLogMapper jobLogMapper;

    @Override
    @Async
    public void recordJobFailureAsync(Long jobId, LocalDateTime beginTime, String jobHandlerName,
                                      String jobHandlerParam, Integer executeIndex, LocalDateTime endTime,
                                      Integer duration, String result) {
        try {
            JobLogDO logEntry = JobLogDO.builder().jobId(jobId).handlerName(jobHandlerName)
                    .handlerParam(jobHandlerParam).executeIndex(executeIndex).beginTime(beginTime)
                    .endTime(endTime).duration(duration).status(JobLogStatusEnum.FAILURE.getStatus())
                    .result(result != null && result.length() > 4000 ? result.substring(0, 4000) : result).build();
            jobLogMapper.insert(logEntry);
        } catch (Exception ex) {
            log.error("[recordJobFailureAsync][jobId({}) handler({})]", jobId, jobHandlerName, ex);
        }
    }

    @Override
    public Integer cleanJobLog(Integer exceedDay, Integer deleteLimit) {
        if (exceedDay == null || exceedDay < 1 || deleteLimit == null || deleteLimit < 1 || deleteLimit > 1000) {
            throw new IllegalArgumentException("日志保留天数必须大于 0，每批清理条数必须在 1 到 1000 之间");
        }
        int count = 0;
        LocalDateTime expireDate = LocalDateTime.now().minusDays(exceedDay);
        Long maxId = jobLogMapper.selectMaxIdForCleanup();
        if (maxId == null) {
            return 0;
        }
        // 固定本轮上界并按主键推进，避免反复扫描保留日志或追逐新增记录。
        long afterId = 0;
        while (afterId < maxId && !Thread.currentThread().isInterrupted()) {
            List<Long> ids = jobLogMapper.selectCleanupIds(afterId, maxId, expireDate,
                    JobLogStatusEnum.SUCCESS.getStatus(), deleteLimit);
            if (ids.isEmpty()) {
                break;
            }
            count += jobLogMapper.deleteCleanupIds(ids, expireDate, JobLogStatusEnum.SUCCESS.getStatus());
            afterId = ids.get(ids.size() - 1);
        }
        return count;
    }

    @Override
    public JobLogDO getJobLog(Long id) {
        return jobLogMapper.selectById(id);
    }

    @Override
    public PageResult<JobLogDO> getJobLogPage(JobLogPageReqVO pageReqVO) {
        return jobLogMapper.selectPage(pageReqVO);
    }

}
