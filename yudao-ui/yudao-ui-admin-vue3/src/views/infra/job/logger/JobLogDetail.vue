<template>
  <Dialog
    v-model="dialogVisible"
    title="执行日志详情"
    aria-label="执行日志详情"
    width="700px"
    class="job-admin-dialog"
  >
    <el-descriptions v-loading="detailLoading" :column="1" border>
      <el-descriptions-item label="日志编号" min-width="60">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item label="任务编号">
        {{ detailData.jobId }}
      </el-descriptions-item>
      <el-descriptions-item label="处理器">
        {{ detailData.handlerName }}
      </el-descriptions-item>
      <el-descriptions-item label="任务参数">
        {{ detailData.handlerParam || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="第几次执行">
        {{ detailData.executeIndex }}
      </el-descriptions-item>
      <el-descriptions-item label="执行时间">
        {{ formatDate(detailData.beginTime) + ' ~ ' + formatDate(detailData.endTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="执行时长">
        {{ detailData.duration + ' 毫秒' }}
      </el-descriptions-item>
      <el-descriptions-item label="任务状态">
        <dict-tag :type="DICT_TYPE.INFRA_JOB_LOG_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="执行结果">
        {{ detailData.result }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as JobLogApi from '@/api/infra/jobLog'

defineOptions({ name: 'JobLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as JobLogApi.JobLogVO) // 详情数据

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  detailData.value = {} as JobLogApi.JobLogVO
  // 查看，设置数据
  if (id) {
    detailLoading.value = true
    try {
      detailData.value = await JobLogApi.getJobLog(id)
    } finally {
      detailLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
