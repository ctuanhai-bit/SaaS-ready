package cn.iocoder.yudao.framework.quartz.core.service;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Job 日志 Framework Service 接口
 *
 * @author 芋道源码
 */
public interface JobLogFrameworkService {

    /**
     * 异步记录失败的 Job 执行；成功执行不写入运行日志。
     *
     * @param jobId           任务编号
     * @param beginTime       开始时间
     * @param jobHandlerName  Job 处理器的名字
     * @param jobHandlerParam Job 处理器的参数
     * @param executeIndex    第几次执行
     * @param endTime        结束时间
     * @param duration       运行时长，单位：毫秒
     * @param result         失败原因
     */
    void recordJobFailureAsync(@NotNull(message = "任务编号不能为空") Long jobId,
                      @NotNull(message = "开始时间") LocalDateTime beginTime,
                      @NotEmpty(message = "Job 处理器的名字不能为空") String jobHandlerName,
                      String jobHandlerParam,
                      @NotNull(message = "第几次执行不能为空") Integer executeIndex,
                      @NotNull(message = "结束时间不能为空") LocalDateTime endTime,
                      @NotNull(message = "运行时长不能为空") Integer duration, String result);
}
