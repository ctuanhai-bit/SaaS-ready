package cn.iocoder.yudao.module.infra.service.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobLogDO;
import cn.iocoder.yudao.module.infra.dal.mysql.job.JobLogMapper;
import cn.iocoder.yudao.module.infra.enums.job.JobLogStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.addTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(JobLogServiceImpl.class)
public class JobLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private JobLogServiceImpl jobLogService;
    @Resource
    private JobLogMapper jobLogMapper;
    @Resource
    private DataSource dataSource;

    @Test
    public void testRecordJobFailureAsync() {
        JobLogDO reqVO = randomPojo(JobLogDO.class, o -> o.setExecuteIndex(1));
        jobLogService.recordJobFailureAsync(reqVO.getJobId(), reqVO.getBeginTime(), reqVO.getHandlerName(),
                reqVO.getHandlerParam(), reqVO.getExecuteIndex(), reqVO.getEndTime(), reqVO.getDuration(), reqVO.getResult());
        List<JobLogDO> logs = jobLogMapper.selectList();
        assertEquals(1, logs.size());
        JobLogDO job = logs.get(0);
        assertEquals(JobLogStatusEnum.FAILURE.getStatus(), job.getStatus());
        assertEquals(reqVO.getJobId(), job.getJobId());
        assertEquals(reqVO.getHandlerName(), job.getHandlerName());
        assertEquals(reqVO.getHandlerParam(), job.getHandlerParam());
        assertEquals(reqVO.getExecuteIndex(), job.getExecuteIndex());
        assertEquals(reqVO.getBeginTime(), job.getBeginTime());
        assertEquals(reqVO.getEndTime(), job.getEndTime());
        assertEquals(reqVO.getDuration(), job.getDuration());
        assertEquals(reqVO.getResult(), job.getResult());
    }

    @Test
    public void testRecordJobFailureAsync_truncatesOversizeReason() {
        String result = new String(new char[5000]).replace('\0', 'x');
        jobLogService.recordJobFailureAsync(27L, LocalDateTime.now(), "jobLogCleanJob", null,
                2, LocalDateTime.now(), 10, result);
        JobLogDO job = jobLogMapper.selectList().get(0);
        assertEquals(4000, job.getResult().length());
        assertEquals(JobLogStatusEnum.FAILURE.getStatus(), job.getStatus());
    }

    @Test
    public void testCleanJobLog_retainsRecentFailuresAndRunningEntries() {
        insertLog(8, JobLogStatusEnum.FAILURE);
        insertLog(1, JobLogStatusEnum.SUCCESS);
        JobLogDO recentFailure = insertLog(6, JobLogStatusEnum.FAILURE);
        JobLogDO running = insertLog(1, JobLogStatusEnum.RUNNING);
        insertLog(9, JobLogStatusEnum.RUNNING);
        JobLogDO deleted = insertLog(8, JobLogStatusEnum.FAILURE);
        jobLogMapper.deleteById(deleted.getId());
        assertEquals(4, jobLogService.cleanJobLog(7, 1));
        assertEquals(2, new JdbcTemplate(dataSource).queryForObject("SELECT COUNT(*) FROM infra_job_log", Integer.class));
        assertEquals(2, jobLogMapper.selectList().size());
        assertEquals(recentFailure.getId(), jobLogMapper.selectById(recentFailure.getId()).getId());
        assertEquals(running.getId(), jobLogMapper.selectById(running.getId()).getId());
        assertEquals(0, jobLogService.cleanJobLog(7, 1));
    }

    @Test
    public void testCleanupBoundaryAndSnapshot() {
        LocalDateTime cutoff = buildTime(2026, 8, 24);
        JobLogDO before = insertLog(1, JobLogStatusEnum.FAILURE);
        JobLogDO exact = insertLog(1, JobLogStatusEnum.FAILURE);
        JobLogDO success = insertLog(1, JobLogStatusEnum.SUCCESS);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE infra_job_log SET create_time=? WHERE id=?", cutoff.minusSeconds(1), before.getId());
        jdbc.update("UPDATE infra_job_log SET create_time=? WHERE id=?", cutoff, exact.getId());
        Long snapshot = jobLogMapper.selectMaxIdForCleanup();
        JobLogDO addedLater = insertLog(1, JobLogStatusEnum.SUCCESS);
        List<Long> ids = jobLogMapper.selectCleanupIds(0L, snapshot, cutoff, 1, 1000);
        assertEquals(java.util.Arrays.asList(before.getId(), success.getId()), ids);
        assertEquals(2, jobLogMapper.deleteCleanupIds(ids, cutoff, 1));
        assertTrue(jobLogMapper.selectCleanupIds(snapshot, addedLater.getId(), cutoff, 1, 1000).contains(addedLater.getId()));
        assertEquals(exact.getId(), jobLogMapper.selectById(exact.getId()).getId());
    }

    @Test
    public void testCleanJobLog_validatesLimits() {
        assertThrows(IllegalArgumentException.class, () -> jobLogService.cleanJobLog(0, 100));
        assertThrows(IllegalArgumentException.class, () -> jobLogService.cleanJobLog(7, 0));
        assertThrows(IllegalArgumentException.class, () -> jobLogService.cleanJobLog(7, 1001));
        assertEquals(0, jobLogService.cleanJobLog(7, 100));
    }

    private JobLogDO insertLog(int ageDays, JobLogStatusEnum status) {
        JobLogDO log = randomPojo(JobLogDO.class, o -> {
            o.setExecuteIndex(1);
            o.setStatus(status.getStatus());
            o.setCreateTime(LocalDateTime.now().minusDays(ageDays));
        });
        jobLogMapper.insert(log);
        return log;
    }

    @Test
    public void testCleanJobLog() {
        // mock 数据
        JobLogDO log01 = randomPojo(JobLogDO.class, o -> {
            o.setCreateTime(addTime(Duration.ofDays(-3)));
            o.setStatus(JobLogStatusEnum.FAILURE.getStatus());
        })
                .setExecuteIndex(1);
        jobLogMapper.insert(log01);
        JobLogDO log02 = randomPojo(JobLogDO.class, o -> {
            o.setCreateTime(addTime(Duration.ofDays(-1)));
            o.setStatus(JobLogStatusEnum.FAILURE.getStatus());
        })
                .setExecuteIndex(1);
        jobLogMapper.insert(log02);
        // 准备参数
        Integer exceedDay = 2;
        Integer deleteLimit = 1;

        // 调用
        Integer count = jobLogService.cleanJobLog(exceedDay, deleteLimit);
        // 断言
        assertEquals(1, count);
        List<JobLogDO> logs = jobLogMapper.selectList();
        assertEquals(1, logs.size());
        // TODO @芋艿：createTime updateTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(log02, logs.get(0), "createTime", "updateTime");
    }

    @Test
    public void testGetJobLog() {
        // mock 数据
        JobLogDO dbJobLog = randomPojo(JobLogDO.class, o -> o.setExecuteIndex(1));
        jobLogMapper.insert(dbJobLog);
        // 准备参数
        Long id = dbJobLog.getId();

        // 调用
        JobLogDO jobLog = jobLogService.getJobLog(id);
        // 断言
        assertPojoEquals(dbJobLog, jobLog);
    }

    @Test
    public void testGetJobPage() {
        // mock 数据
        JobLogDO dbJobLog = randomPojo(JobLogDO.class, o -> {
            o.setExecuteIndex(1);
            o.setHandlerName("handlerName 单元测试");
            o.setStatus(JobLogStatusEnum.SUCCESS.getStatus());
            o.setBeginTime(buildTime(2021, 1, 8));
            o.setEndTime(buildTime(2021, 1, 8));
        });
        jobLogMapper.insert(dbJobLog);
        // 测试 jobId 不匹配
        jobLogMapper.insert(cloneIgnoreId(dbJobLog, o -> o.setJobId(randomLongId())));
        // 测试 handlerName 不匹配
        jobLogMapper.insert(cloneIgnoreId(dbJobLog, o -> o.setHandlerName(randomString())));
        // 测试 beginTime 不匹配
        jobLogMapper.insert(cloneIgnoreId(dbJobLog, o -> o.setBeginTime(buildTime(2021, 1, 7))));
        // 测试 endTime 不匹配
        jobLogMapper.insert(cloneIgnoreId(dbJobLog, o -> o.setEndTime(buildTime(2021, 1, 9))));
        // 测试 status 不匹配
        jobLogMapper.insert(cloneIgnoreId(dbJobLog, o -> o.setStatus(JobLogStatusEnum.FAILURE.getStatus())));
        // 准备参数
        JobLogPageReqVO reqVo = new JobLogPageReqVO();
        reqVo.setJobId(dbJobLog.getJobId());
        reqVo.setHandlerName("单元");
        reqVo.setBeginTime(dbJobLog.getBeginTime());
        reqVo.setEndTime(dbJobLog.getEndTime());
        reqVo.setStatus(JobLogStatusEnum.SUCCESS.getStatus());

        // 调用
        PageResult<JobLogDO> pageResult = jobLogService.getJobLogPage(reqVo);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbJobLog, pageResult.getList().get(0));
    }

}
