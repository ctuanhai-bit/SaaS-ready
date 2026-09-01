<template>
  <el-form
    v-show="getShow"
    ref="formLogin"
    :model="loginData.loginForm"
    :rules="LoginRules"
    class="login-form"
    label-position="top"
    label-width="120px"
    size="large"
  >
    <el-row class="mx-[-10px]">
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <LoginFormTitle class="w-full" />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item v-if="loginData.tenantEnable === 'true'">
          <el-select
            v-model="selectedTenantId"
            class="tenant-select"
            filterable
            :loading="tenantLoading"
            placeholder="请选择租户"
            @change="selectTenant"
          >
            <el-option
              v-for="tenant in tenantOptions"
              :key="tenant.id"
              :label="tenant.label"
              :value="tenant.id"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item prop="username">
          <el-input
            v-model="loginData.loginForm.username"
            :placeholder="t('login.usernamePlaceholder')"
            :prefix-icon="iconAvatar"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item prop="password">
          <el-input
            v-model="loginData.loginForm.password"
            :placeholder="t('login.passwordPlaceholder')"
            :prefix-icon="iconLock"
            show-password
            type="password"
            @keyup.enter="getCode()"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <XButton
            :loading="loginLoading"
            :title="t('login.login')"
            class="w-full"
            type="primary"
            @click="getCode()"
          />
        </el-form-item>
      </el-col>
      <Verify
        v-if="loginData.captchaEnable === 'true'"
        ref="verify"
        :captchaType="captchaType"
        :imgSize="{ width: '400px', height: '200px' }"
        mode="pop"
        @success="handleLogin"
      />
    </el-row>
  </el-form>
</template>
<script lang="ts" setup>
import { ElLoading } from 'element-plus'
import LoginFormTitle from './LoginFormTitle.vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

import { useIcon } from '@/hooks/web/useIcon'

import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import * as LoginApi from '@/api/login'
import { LoginStateEnum, useFormValid, useLoginState } from './useLogin'

defineOptions({ name: 'LoginForm' })

const { t } = useI18n()
const message = useMessage()
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })
const formLogin = ref()
const { validForm } = useFormValid(formLogin)
const { getLoginState } = useLoginState()
const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const redirect = ref<string>('')
const loginLoading = ref(false)
const verify = ref()
const captchaType = ref('blockPuzzle') // blockPuzzle 滑块 clickWord 点击文字 pictureWord 文字验证码

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

const LoginRules = {
  username: [required],
  password: [required]
}
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE,
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  loginForm: {
    tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    rememberMe: false
  }
})

interface LoginTenantOption extends LoginApi.LoginTenantVO {
  label: string
}

const getDefaultTenantId = () => {
  const tenantId = Number(import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT_ID || 0)
  return Number.isFinite(tenantId) && tenantId > 0 ? tenantId : 0
}

const defaultTenantId = getDefaultTenantId()
const defaultTenantName = String(import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '').trim()
const selectedTenantId = ref<number | undefined>(defaultTenantId || undefined)
const tenantOptions = ref<LoginTenantOption[]>(
  defaultTenantId && defaultTenantName
    ? [{ id: defaultTenantId, name: defaultTenantName, label: defaultTenantName }]
    : []
)
const tenantLoading = ref(false)

const selectTenant = (tenantId?: number) => {
  if (!tenantId) return
  const tenant = tenantOptions.value.find((item) => item.id === tenantId)
  loginData.loginForm.tenantName = tenant?.name || defaultTenantName
  authUtil.setTenantId(tenantId)
}

const loadTenantOptions = async () => {
  if (loginData.tenantEnable !== 'true') return
  tenantLoading.value = true
  try {
    const tenants = await LoginApi.getTenantList()
    const options = (Array.isArray(tenants) ? tenants : []).map((tenant) => ({
      ...tenant,
      label: tenant.id === defaultTenantId && defaultTenantName ? defaultTenantName : tenant.name
    }))
    if (options.length) {
      tenantOptions.value = options
    }
    if (
      !selectedTenantId.value ||
      !tenantOptions.value.some((item) => item.id === selectedTenantId.value)
    ) {
      selectedTenantId.value = tenantOptions.value[0]?.id
    }
    selectTenant(selectedTenantId.value)
  } catch {
    if (defaultTenantId) {
      tenantOptions.value = [
        {
          id: defaultTenantId,
          name: defaultTenantName,
          label: defaultTenantName
        }
      ]
      selectedTenantId.value = defaultTenantId
      selectTenant(defaultTenantId)
      return
    }
    message.error('租户列表加载失败，请稍后再试')
  } finally {
    tenantLoading.value = false
  }
}

// 获取验证码
const getCode = async () => {
  const data = await validForm()
  if (!data) return
  if (loginData.tenantEnable === 'true' && !selectedTenantId.value) {
    message.warning('请选择租户')
    return
  }
  // 情况一，未开启：则直接登录
  if (loginData.captchaEnable === 'false') {
    await handleLogin({})
  } else {
    // 情况二，已开启：则展示验证码；只有完成验证码的情况，才进行登录
    // 弹出验证码
    verify.value.show()
  }
}
const loading = ref() // ElLoading.service 返回的实例
// 登录
const handleLogin = async (params: any) => {
  loginLoading.value = true
  try {
    const data = await validForm()
    if (!data) {
      return
    }
    selectTenant(selectedTenantId.value)
    const loginDataLoginForm = { ...loginData.loginForm }
    loginDataLoginForm.captchaVerification = params.captchaVerification
    const res = await LoginApi.login(loginDataLoginForm)
    if (!res) {
      return
    }
    loading.value = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    if (loginDataLoginForm.rememberMe) {
      authUtil.setLoginForm(loginDataLoginForm)
    } else {
      authUtil.removeLoginForm()
    }
    if (res.tenantId) {
      authUtil.setTenantId(res.tenantId)
    }
    authUtil.setToken(res)
    if (!redirect.value) {
      redirect.value = '/'
    }
    // 判断是否为SSO登录
    if (redirect.value.indexOf('sso') !== -1) {
      window.location.href = window.location.href.replace('/login?redirect=', '')
    } else {
      await push({ path: redirect.value || permissionStore.addRouters[0].path })
    }
  } finally {
    loginLoading.value = false
    loading.value?.close?.()
  }
}
watch(
  () => currentRoute.value,
  (route: RouteLocationNormalizedLoaded) => {
    redirect.value = route?.query?.redirect as string
  },
  {
    immediate: true
  }
)
onMounted(() => {
  if (defaultTenantId) {
    selectTenant(defaultTenantId)
  }
  loadTenantOptions()
})
</script>

<style lang="scss" scoped>
.login-form {
  --el-color-primary: #0f766e;
  --el-color-primary-light-3: #3f918b;
  --el-color-primary-dark-2: #0b5f59;

  width: 100%;

  :deep(.el-input__wrapper) {
    min-height: 50px;
    border-radius: 6px;
    box-shadow: 0 0 0 1px #d7ddd9 inset;
  }

  :deep(.el-select__wrapper) {
    min-height: 50px;
    border-radius: 6px;
    box-shadow: 0 0 0 1px #d7ddd9 inset;
  }

  :deep(.el-select__wrapper.is-focused) {
    box-shadow: 0 0 0 1px #0f766e inset;
  }

  .tenant-select {
    width: 100%;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #0f766e inset;
  }

  :deep(.el-button) {
    width: 100%;
    min-height: 50px;
    font-weight: 700;
    border-radius: 6px;
  }

  :deep(h2) {
    margin-bottom: 20px;
    color: #17211d;
    font-size: 28px;
    letter-spacing: 0;
  }
}
</style>
