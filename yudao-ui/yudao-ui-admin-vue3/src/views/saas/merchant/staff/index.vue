<template>
  <div class="saas-staff-page">
    <section class="saas-staff-page__hero">
      <div>
        <div class="saas-staff-page__crumb">Hotel PMS Community / 酒店管理 / 人员管理</div>
        <h1>人员管理</h1>
        <p>维护当前酒店的后台操作人员账号，包含新增、编辑、启停等操作。</p>
      </div>
      <div class="saas-staff-page__context">
        <span>当前酒店</span>
        <strong>{{ merchantContext.name || '未加载' }}</strong>
        <small>酒店编号 {{ merchantContext.id || '-' }}</small>
      </div>
    </section>

    <!-- 错误态 -->
    <el-alert
      v-if="pageError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="pageError"
    >
      <template #default>
        <el-button class="mt-8px" type="primary" @click="getList">重试</el-button>
      </template>
    </el-alert>

    <!-- 搜索区 -->
    <ContentWrap>
      <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="96px">
        <el-form-item label="用户账号" prop="username">
          <el-input
            v-model="queryParams.username"
            class="!w-220px"
            clearable
            placeholder="输入账号搜索"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="手机号码" prop="mobile">
          <el-input
            v-model="queryParams.mobile"
            class="!w-220px"
            clearable
            placeholder="输入手机号搜索"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="全部">
            <el-option label="启用" :value="0" />
            <el-option label="停用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery">
            <Icon class="mr-5px" icon="ep:search" />
            查询
          </el-button>
          <el-button @click="resetQuery">
            <Icon class="mr-5px" icon="ep:refresh" />
            重置
          </el-button>
          <el-button v-hasPermi="['merchant:staff:create']" plain type="primary" @click="openForm('create')">
            <Icon class="mr-5px" icon="ep:plus" />
            新增人员
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表区 -->
    <ContentWrap>
      <!-- 空态 -->
      <el-empty v-if="!loading && list.length === 0 && !pageError" description="暂无人员数据" />

      <el-table
        v-else
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
        :stripe="true"
      >
        <el-table-column align="center" label="人员编号" prop="id" width="100" />
        <el-table-column align="center" label="用户账号" prop="username" min-width="140" />
        <el-table-column align="center" label="昵称" prop="nickname" min-width="120" />
        <el-table-column align="center" label="手机号码" prop="mobile" width="140">
          <template #default="scope">
            {{ scope.row.mobile || '-' }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="状态" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'info'">
              {{ scope.row.status === 0 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" label="创建时间" prop="createTime" width="180" />
        <el-table-column align="center" fixed="right" label="操作" width="180">
          <template #default="scope">
            <el-button
              v-hasPermi="['merchant:staff:update']"
              link
              type="primary"
              @click="openForm('update', scope.row.id)"
            >
              编辑
            </el-button>
            <el-button
              v-hasPermi="['merchant:staff:update']"
              link
              :type="scope.row.status === 0 ? 'warning' : 'success'"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 0 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNo"
        :total="total"
        @pagination="getList"
      />
    </ContentWrap>

    <!-- 新增/编辑弹窗 -->
    <Dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="用户账号" prop="username">
          <el-input
            v-model="formData.username"
            maxlength="30"
            placeholder="4-30位字母或数字"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="formData.nickname"
            maxlength="30"
            placeholder="请输入昵称"
          />
        </el-form-item>
        <el-form-item label="手机号码" prop="mobile">
          <el-input
            v-model="formData.mobile"
            maxlength="11"
            placeholder="请输入手机号码"
          />
        </el-form-item>
        <el-form-item v-if="formType === 'create'" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            maxlength="16"
            placeholder="4-16位密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { ContentWrap } from '@/components/ContentWrap'
import { Dialog } from '@/components/Dialog'
import { Icon } from '@/components/Icon'
import Pagination from '@/components/Pagination/index.vue'
import { MerchantStaffApi } from '@/api/saas/merchant/staff'
import { SaasMerchantApi } from '@/api/saas/merchant'
import type { SaasMerchantVO } from '@/api/saas/merchant'
import type {
  MerchantStaffRespVO,
  MerchantStaffPageReqVO,
  MerchantStaffSaveReqVO
} from '@/api/saas/merchant/staff'

// ---- 商户上下文 ----
const merchantContext = ref<Partial<SaasMerchantVO>>({})
const pageError = ref('')

const getMerchantContext = async () => {
  try {
    merchantContext.value = await SaasMerchantApi.getMerchantContext()
  } catch {
    // 商户上下文接口可能未就绪，非阻塞
  }
}

// ---- 查询 ----
const queryFormRef = ref()
const loading = ref(false)
const list = ref<MerchantStaffRespVO[]>([])
const total = ref(0)

const queryParams = reactive<MerchantStaffPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  username: '',
  mobile: '',
  status: undefined
})

const getList = async () => {
  loading.value = true
  pageError.value = ''
  try {
    const res = await MerchantStaffApi.getStaffPage({
      ...queryParams,
      pageNo: queryParams.pageNo ?? 1,
      pageSize: queryParams.pageSize ?? 10
    })
    list.value = (res as any).list ?? []
    total.value = (res as any).total ?? 0
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '加载人员列表失败'
    pageError.value = typeof msg === 'string' ? msg : '加载人员列表失败，请稍后重试'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.username = ''
  queryParams.mobile = ''
  queryParams.status = undefined
  handleQuery()
}

// ---- 表单 ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formType = ref<'create' | 'update'>('create')
const formLoading = ref(false)
const formRef = ref()

const formData = reactive<MerchantStaffSaveReqVO>({
  id: undefined,
  username: '',
  nickname: '',
  mobile: '',
  password: ''
})

const formRules = {
  username: [
    { required: true, message: '用户账号不能为空', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9]{4,30}$/, message: '用户账号由4-30位字母或数字组成', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '昵称不能为空', trigger: 'blur' },
    { max: 30, message: '昵称长度不能超过30个字符', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 4, max: 16, message: '密码长度为4-16位', trigger: 'blur' }
  ]
}

const resetForm = () => {
  formData.id = undefined
  formData.username = ''
  formData.nickname = ''
  formData.mobile = ''
  formData.password = ''
}

const openForm = (type: 'create' | 'update', id?: number) => {
  formType.value = type
  resetForm()
  if (type === 'update' && id) {
    dialogTitle.value = '编辑人员'
    formData.id = id
    const row = list.value.find((r) => r.id === id)
    if (row) {
      formData.username = row.username
      formData.nickname = row.nickname
      formData.mobile = row.mobile || ''
    }
  } else {
    dialogTitle.value = '新增人员'
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  formLoading.value = true
  try {
    const payload: MerchantStaffSaveReqVO = {
      username: formData.username,
      nickname: formData.nickname,
      mobile: formData.mobile || undefined
    }
    if (formType.value === 'create') {
      payload.password = formData.password
      await MerchantStaffApi.createStaff(payload)
      ElMessage.success('新增人员成功')
    } else {
      payload.id = formData.id
      await MerchantStaffApi.updateStaff(payload)
      ElMessage.success('编辑人员成功')
    }
    dialogVisible.value = false
    await getList()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '操作失败'
    ElMessage.error(typeof msg === 'string' ? msg : '操作失败，请稍后重试')
  } finally {
    formLoading.value = false
  }
}

// ---- 启停 ----
const toggleStatus = async (row: MerchantStaffRespVO) => {
  const action = row.status === 0 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}人员「${row.username}」？`, `确认${action}`, {
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  const newStatus = row.status === 0 ? 1 : 0
  try {
    await MerchantStaffApi.updateStaffStatus({ id: row.id, status: newStatus })
    ElMessage.success(`${action}成功`)
    await getList()
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || `${action}失败`
    ElMessage.error(typeof msg === 'string' ? msg : `${action}失败，请稍后重试`)
  }
}

// ---- 初始化 ----
onMounted(async () => {
  await getMerchantContext()
  await getList()
})

// suppress unused-ref warnings for template-only refs
void queryFormRef
</script>

<style scoped lang="scss">
.saas-staff-page__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 32px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: #fff;

  h1 {
    margin: 8px 0 4px;
    font-size: 24px;
    font-weight: 600;
  }

  p {
    margin: 0;
    opacity: 0.85;
    font-size: 14px;
  }

  .saas-staff-page__crumb {
    font-size: 12px;
    opacity: 0.7;
  }

  .saas-staff-page__context {
    text-align: right;
    font-size: 13px;

    span {
      display: block;
      opacity: 0.7;
      font-size: 12px;
    }

    strong {
      display: block;
      margin-top: 4px;
      font-size: 16px;
      font-weight: 600;
    }

    small {
      display: block;
      margin-top: 2px;
      opacity: 0.65;
    }
  }
}
</style>
