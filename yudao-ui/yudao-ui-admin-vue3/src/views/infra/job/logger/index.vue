<template>
  <div class="job-admin-page is-saas-view">
    <header class="job-admin-page__header">
      <div>
        <div class="job-admin-page__crumb">平台管理后台 / 全局能力 / 定时任务</div>
        <h1>执行日志</h1>
      </div>
      <div class="job-admin-page__actions">
        <el-button @click="backToJobs"><Icon icon="ep:back" class="mr-5px" />定时任务</el-button>
        <el-button :loading="exportLoading" @click="handleExport" v-hasPermi="['infra:job:export']">
          <Icon icon="ep:download" class="mr-5px" />导出日志
        </el-button>
        <el-tooltip content="刷新日志" placement="bottom">
          <el-button :loading="loading" aria-label="刷新日志" @click="getList"
            ><Icon icon="ep:refresh"
          /></el-button>
        </el-tooltip>
      </div>
    </header>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="job-admin-page__filters">
      <el-form-item label="任务编号" prop="jobId">
        <el-input-number
          v-model="queryParams.jobId"
          :min="1"
          :precision="0"
          :controls="false"
          placeholder="全部任务"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="处理器" prop="handlerName">
        <el-input
          v-model="queryParams.handlerName"
          placeholder="搜索处理器名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="beginTime">
        <el-date-picker
          v-model="queryParams.beginTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="开始执行时间"
          clearable
        />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker
          v-model="queryParams.endTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="结束执行时间"
          clearable
        />
      </el-form-item>
      <el-form-item label="执行结果" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部结果" clearable>
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.INFRA_JOB_LOG_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"
          ><Icon icon="ep:search" class="mr-5px" />查询</el-button
        >
        <el-button @click="resetQuery"
          ><Icon icon="ep:refresh-left" class="mr-5px" />重置</el-button
        >
      </el-form-item>
    </el-form>
    <section class="job-admin-page__table" aria-label="任务执行日志">
      <div class="job-admin-page__table-heading"
        ><h2
          >执行记录 <span>{{ total }} 条</span></h2
        ></div
      >
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="日志编号" prop="id" width="100" />
        <el-table-column label="任务" min-width="240">
          <template #default="{ row }">
            <strong class="job-admin-page__name">#{{ row.jobId }}</strong>
            <span class="job-admin-page__secondary">{{ row.handlerName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ formatDate(row.beginTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ row.endTime ? formatDate(row.endTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="120">
          <template #default="{ row }">{{
            row.duration == null ? '-' : row.duration + ' 毫秒'
          }}</template>
        </el-table-column>
        <el-table-column label="执行次数" prop="executeIndex" width="100" align="center" />
        <el-table-column label="执行结果" width="110">
          <template #default="{ row }"
            ><dict-tag :type="DICT_TYPE.INFRA_JOB_LOG_STATUS" :value="row.status"
          /></template>
        </el-table-column>
        <el-table-column label="结果摘要" prop="result" min-width="180" show-overflow-tooltip />
        <el-table-column
          label="操作"
          width="80"
          :fixed="appStore.getMobile ? false : 'right'"
          align="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="openDetail(row.id)"
              v-hasPermi="['infra:job:query']"
              >详情</el-button
            >
          </template>
        </el-table-column>
        <template #empty
          ><el-empty
            :description="loadError ? '日志加载失败，请刷新重试' : '暂无执行记录'"
            :image-size="64"
        /></template>
      </el-table>
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </section>
    <JobLogDetail ref="detailRef" />
  </div>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import { useAppStore } from '@/store/modules/app'
import download from '@/utils/download'
import JobLogDetail from './JobLogDetail.vue'
import * as JobLogApi from '@/api/infra/jobLog'
import '../job-admin.scss'

defineOptions({ name: 'InfraJobLog' })

const message = useMessage()
const appStore = useAppStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const total = ref(0)
const list = ref<JobLogApi.JobLogVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  jobId: Number(route.query.id) || undefined,
  handlerName: undefined,
  beginTime: undefined,
  endTime: undefined,
  status: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)
const detailRef = ref()

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await JobLogApi.getJobLogPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch {
    list.value = []
    total.value = 0
    loadError.value = '日志加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  queryParams.jobId = Number(route.query.id) || undefined
  handleQuery()
}
const openDetail = (id: number) => detailRef.value.open(id)
const backToJobs = () =>
  router.push(route.path.startsWith('/saas-platform') ? '/saas-platform/jobs' : '/infra/job')
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    download.excel(await JobLogApi.exportJobLog(queryParams), '定时任务执行日志.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}
watch(
  () => route.query.id,
  (id) => {
    queryParams.jobId = Number(id) || undefined
    handleQuery()
  }
)
onMounted(getList)
</script>
