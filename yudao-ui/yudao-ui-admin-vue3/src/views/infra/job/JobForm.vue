<template>
  <Dialog
    :title="dialogTitle"
    :aria-label="dialogTitle"
    v-model="dialogVisible"
    class="job-admin-dialog"
    width="640px"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="formLoading"
    >
      <el-form-item label="任务名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入任务名称" />
      </el-form-item>
      <el-form-item label="处理器" prop="handlerName">
        <el-input
          :readonly="formData.id !== undefined"
          v-model="formData.handlerName"
          placeholder="请输入处理器名称"
        />
      </el-form-item>
      <el-form-item label="任务参数" prop="handlerParam">
        <el-input v-model="formData.handlerParam" placeholder="选填" />
      </el-form-item>
      <el-form-item label="CRON 表达式" prop="cronExpression">
        <crontab v-model="formData.cronExpression" />
      </el-form-item>
      <el-form-item label="重试次数" prop="retryCount">
        <el-input-number v-model="formData.retryCount" :min="0" :precision="0" />
      </el-form-item>
      <el-form-item label="重试间隔 (毫秒)" prop="retryInterval">
        <el-input-number v-model="formData.retryInterval" :min="0" :precision="0" />
      </el-form-item>
      <el-form-item label="超时时间 (毫秒)" prop="monitorTimeout">
        <el-input-number v-model="formData.monitorTimeout" :min="0" :precision="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm" :loading="formLoading">保存</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as JobApi from '@/api/infra/job'

defineOptions({ name: 'JobForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<Partial<JobApi.JobVO>>({
  id: undefined,
  name: '',
  handlerName: '',
  handlerParam: '',
  cronExpression: '',
  retryCount: undefined,
  retryInterval: undefined,
  monitorTimeout: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
  handlerName: [{ required: true, message: '处理器不能为空', trigger: 'blur' }],
  cronExpression: [{ required: true, message: 'CRON 表达式不能为空', trigger: 'blur' }],
  retryCount: [{ required: true, message: '重试次数不能为空', trigger: 'blur' }],
  retryInterval: [{ required: true, message: '重试间隔不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增任务' : '编辑任务'
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await JobApi.getJob(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交按钮 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as JobApi.JobVO
    if (formType.value === 'create') {
      await JobApi.createJob(data)
      message.success(t('common.createSuccess'))
    } else {
      await JobApi.updateJob(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: '',
    handlerName: '',
    handlerParam: '',
    cronExpression: '',
    retryCount: undefined,
    retryInterval: undefined,
    monitorTimeout: undefined
  }
  formRef.value?.resetFields()
}
</script>
