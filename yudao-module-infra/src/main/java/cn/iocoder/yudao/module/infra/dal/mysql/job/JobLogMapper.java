package cn.iocoder.yudao.module.infra.dal.mysql.job;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.log.JobLogPageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface JobLogMapper extends BaseMapperX<JobLogDO> {

    default PageResult<JobLogDO> selectPage(JobLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<JobLogDO>()
                .eqIfPresent(JobLogDO::getJobId, reqVO.getJobId())
                .likeIfPresent(JobLogDO::getHandlerName, reqVO.getHandlerName())
                .geIfPresent(JobLogDO::getBeginTime, reqVO.getBeginTime())
                .leIfPresent(JobLogDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(JobLogDO::getStatus, reqVO.getStatus())
                .orderByDesc(JobLogDO::getId) // ID 倒序
        );
    }

    @Select("SELECT MAX(id) FROM infra_job_log")
    Long selectMaxIdForCleanup();

    @Select("SELECT id FROM infra_job_log WHERE id > #{afterId} AND id <= #{maxId} "
            + "AND (status = #{successStatus} OR create_time < #{expireTime}) ORDER BY id LIMIT #{limit}")
    List<Long> selectCleanupIds(@Param("afterId") Long afterId, @Param("maxId") Long maxId,
                               @Param("expireTime") LocalDateTime expireTime,
                               @Param("successStatus") Integer successStatus, @Param("limit") Integer limit);

    @Delete({"<script>DELETE FROM infra_job_log WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND (status = #{successStatus} OR create_time &lt; #{expireTime})</script>"})
    Integer deleteCleanupIds(@Param("ids") List<Long> ids, @Param("expireTime") LocalDateTime expireTime,
                             @Param("successStatus") Integer successStatus);

}
