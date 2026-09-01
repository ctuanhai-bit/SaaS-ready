<template>
  <div class="job-admin-page is-saas-view">
    <header class="job-admin-page__header">
      <div>
        <div class="job-admin-page__crumb">平台管理后台 / 全局能力</div>
        <h1>定时任务</h1>
      </div>
      <div class="job-admin-page__actions">
        <el-button @click="handleJobLog()" v-hasPermi="['infra:job:query']">
          <Icon icon="ep:document" class="mr-5px" />执行日志
        </el-button>
        <el-button :loading="exportLoading" @click="handleExport" v-hasPermi="['infra:job:export']">
          <Icon icon="ep:download" class="mr-5px" />导出
        </el-button>
        <el-button type="primary" @click="openForm('create')" v-hasPermi="['infra:job:create']">
          <Icon icon="ep:plus" class="mr-5px" />新增任务
        </el-button>
        <el-tooltip content="刷新任务" placement="bottom">
          <el-button :loading="loading" aria-label="刷新任务" @click="getList"
            ><Icon icon="ep:refresh"
          /></el-button>
        </el-tooltip>
      </div>
    </header>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="job-admin-page__filters">
      <el-form-item label="任务名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="搜索任务名称"
          clearable
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
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.INFRA_JOB_STATUS)"
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
    <section class="job-admin-page__table" aria-label="定时任务列表">
      <div class="job-admin-page__table-heading">
        <h2
          >任务列表 <span>{{ total }} 项</span></h2
        >
        <el-button
          v-if="checkedIds.length"
          type="danger"
          plain
          :disabled="mutating"
          @click="handleDeleteBatch"
          v-hasPermi="['infra:job:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" />删除所选（{{ checkedIds.length }}）
        </el-button>
      </div>
      <el-alert v-if="loadError" type="error" :title="loadError" :closable="false" show-icon />
      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        @selection-change="handleRowCheckboxChange"
      >
        <el-table-column v-if="checkPermi(['infra:job:delete'])" type="selection" width="46" />
        <el-table-column label="任务" min-width="280">
          <template #default="{ row }">
            <strong class="job-admin-page__name">{{ row.name }}</strong>
            <span class="job-admin-page__secondary">#{{ row.id }} · {{ row.handlerName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="任务状态" width="170">
          <template #default="{ row }">
            <div class="job-admin-page__status">
              <el-switch
                v-if="checkPermi(['infra:job:update']) && row.status !== InfraJobStatusEnum.INIT"
                :model-value="row.status === InfraJobStatusEnum.NORMAL"
                :disabled="mutating"
                :aria-label="row.name + '的任务开关'"
                :before-change="() => handleChangeStatus(row)"
              />
              <el-tag
                :type="row.status === InfraJobStatusEnum.NORMAL ? 'success' : 'info'"
                effect="light"
              >
                {{
                  row.status === InfraJobStatusEnum.NORMAL
                    ? '已开启'
                    : row.status === InfraJobStatusEnum.STOP
                      ? '已暂停'
                      : '未就绪'
                }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Cron 表达式" min-width="170">
          <template #default="{ row }"
            ><code>{{ row.cronExpression }}</code></template
          >
        </el-table-column>
        <el-table-column label="参数" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handlerParam || '-' }}</template>
        </el-table-column>
        <el-table-column label="重试次数" prop="retryCount" width="100" align="center" />
        <el-table-column
          label="操作"
          width="220"
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
            <el-button
              type="primary"
              link
              @click="handleJobLog(row.id)"
              v-hasPermi="['infra:job:query']"
              >日志</el-button
            >
            <el-dropdown
              @command="(command) => handleCommand(command, row)"
              v-hasPermi="['infra:job:update', 'infra:job:trigger', 'infra:job:delete']"
            >
              <el-button link aria-label="更多任务操作"><Icon icon="ep:more-filled" /></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="checkPermi(['infra:job:update'])"
                    command="edit"
                    :disabled="mutating"
                    >编辑任务</el-dropdown-item
                  >
                  <el-dropdown-item
                    v-if="checkPermi(['infra:job:trigger'])"
                    command="run"
                    :disabled="mutating"
                    >执行一次</el-dropdown-item
                  >
                  <el-dropdown-item
                    v-if="checkPermi(['infra:job:delete'])"
                    command="delete"
                    :disabled="mutating"
                    >删除任务</el-dropdown-item
                  >
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty
          ><el-empty
            :description="loadError ? '任务加载失败，请刷新重试' : '暂无匹配任务'"
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
    <JobForm ref="formRef" @success="getList" />
    <JobDetail ref="detailRef" />
  </div>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/store/modules/app'
import JobForm from './JobForm.vue'
import JobDetail from './JobDetail.vue'
import download from '@/utils/download'
import * as JobApi from '@/api/infra/job'
import { InfraJobStatusEnum } from '@/utils/constants'
import './job-admin.scss'

defineOptions({ name: 'InfraJob' })

const message = useMessage()
const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const total = ref(0)
const list = ref<JobApi.JobVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  status: undefined,
  handlerName: undefined
})
const queryFormRef = ref()
const exportLoading = ref(false)
const mutating = ref(false)
const checkedIds = ref<number[]>([])
const formRef = ref()
const detailRef = ref()

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await JobApi.getJobPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch {
    list.value = []
    total.value = 0
    loadError.value = '任务加载失败，请稍后重试'
  } finally {
    checkedIds.value = []
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const openDetail = (id: number) => detailRef.value.open(id)
const handleRowCheckboxChange = (rows: JobApi.JobVO[]) => {
  checkedIds.value = rows.map((row) => row.id)
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    download.excel(await JobApi.exportJob(queryParams), '定时任务.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const handleChangeStatus = async (row: JobApi.JobVO) => {
  if (mutating.value) return false
  mutating.value = true
  const enabling = row.status === InfraJobStatusEnum.STOP
  const text = enabling ? '开启' : '暂停'
  try {
    await ElMessageBox.confirm('确认' + text + '“' + row.name + '”？', '任务状态确认', {
      customClass: 'job-admin-dialog',
      confirmButtonText: '确认' + text,
      cancelButtonText: '取消',
      type: 'warning'
    })
    await JobApi.updateJobStatus(
      row.id,
      enabling ? InfraJobStatusEnum.NORMAL : InfraJobStatusEnum.STOP
    )
    message.success(text + '成功')
    await getList()
  } catch {
  } finally {
    mutating.value = false
  }
  // The server response, not an optimistic switch, owns the displayed state.
  return false
}

const handleDeleteBatch = async () => handleDelete(checkedIds.value)
const handleDelete = async (ids: number[]) => {
  if (mutating.value || !ids.length) return
  mutating.value = true
  try {
    await message.delConfirm()
    if (ids.length === 1) await JobApi.deleteJob(ids[0])
    else await JobApi.deleteJobList(ids)
    message.success('删除成功')
    await getList()
  } catch {
  } finally {
    mutating.value = false
  }
}

const handleRun = async (row: JobApi.JobVO) => {
  if (mutating.value) return
  mutating.value = true
  try {
    await ElMessageBox.confirm('确认立即执行“' + row.name + '”？', '执行确认', {
      customClass: 'job-admin-dialog',
      confirmButtonText: '确认执行',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await JobApi.runJob(row.id)
    message.success('已提交执行，请在执行日志中查看结果')
  } catch {
  } finally {
    mutating.value = false
  }
}

const handleCommand = (command: string, row: JobApi.JobVO) => {
  if (command === 'edit') openForm('update', row.id)
  else if (command === 'run') handleRun(row)
  else if (command === 'delete') handleDelete([row.id])
}

const handleJobLog = (id?: number) =>
  router.push({
    path: route.path.startsWith('/saas-platform') ? '/saas-platform/jobs/logs' : '/job/job-log',
    query: id ? { id } : {}
  })

onMounted(getList)
</script>
