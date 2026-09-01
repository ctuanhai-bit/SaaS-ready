<template>
  <section class="hotel-profile__header">
    <div>
      <span>Hotel PMS Community / 酒店资料</span>
      <h1>{{ profile.name || '酒店资料' }}</h1>
      <p>维护酒店对外展示信息、地址、联系方式和媒体资料。</p>
    </div>
    <el-button v-hasPermi="['merchant:profile:update']" type="primary" @click="openEditor">
      编辑资料
    </el-button>
  </section>

  <el-alert
    v-if="errorMessage"
    class="mb-16px"
    :closable="false"
    show-icon
    type="error"
    :title="errorMessage"
  >
    <template #default>
      <el-button class="mt-8px" type="primary" @click="loadProfile">重试</el-button>
    </template>
  </el-alert>

  <ContentWrap v-loading="loading">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="酒店名称">{{ profile.name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="profile.status === 1 ? 'success' : 'info'">
          {{ profile.status === 1 ? '启用' : '未启用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="联系人">{{ profile.contactName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ profile.contactMobile || '-' }}</el-descriptions-item>
      <el-descriptions-item label="地址" :span="2">{{ profile.address || '-' }}</el-descriptions-item>
      <el-descriptions-item label="经度">{{ profile.longitude ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="纬度">{{ profile.latitude ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="酒店简介" :span="2">
        {{ profile.description || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="封面" :span="2">
        <el-image
          v-if="profile.coverUrl"
          class="hotel-profile__cover"
          fit="cover"
          :preview-src-list="[profile.coverUrl]"
          :src="profile.coverUrl"
        />
        <span v-else>-</span>
      </el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <Dialog v-model="editorVisible" title="编辑酒店资料" width="720px">
    <el-form ref="formRef" v-loading="saving" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="酒店名称" prop="name">
        <el-input v-model.trim="formData.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model.trim="formData.contactName" maxlength="64" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactMobile">
        <el-input v-model.trim="formData.contactMobile" maxlength="32" />
      </el-form-item>
      <el-form-item label="地址" prop="address">
        <el-input v-model.trim="formData.address" maxlength="255" show-word-limit />
      </el-form-item>
      <el-form-item label="经纬度">
        <div class="hotel-profile__coordinates">
          <el-input-number v-model="formData.longitude" :min="-180" :max="180" :precision="6" />
          <el-input-number v-model="formData.latitude" :min="-90" :max="90" :precision="6" />
        </div>
      </el-form-item>
      <el-form-item label="酒店简介" prop="description">
        <el-input
          v-model.trim="formData.description"
          :rows="4"
          maxlength="500"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="酒店封面" prop="coverUrl">
        <UploadImg
          v-model="formData.coverUrl"
          directory="hotel-pms/public/hotel"
          :height="'140px'"
          :width="'260px'"
        />
      </el-form-item>
      <el-form-item label="酒店相册">
        <UploadImgs
          v-model="imageList"
          directory="hotel-pms/public/hotel"
          :limit="8"
          :height="'112px'"
          :width="'156px'"
        />
      </el-form-item>
      <el-form-item label="主题色" prop="themeColor">
        <el-color-picker v-model="formData.themeColor" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editorVisible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="saveProfile">保存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { UploadImg, UploadImgs } from '@/components/UploadFile'
import {
  type HotelProfileUpdateReqVO,
  type HotelProfileVO,
  SaasMerchantApi
} from '@/api/saas/merchant'

defineOptions({ name: 'HotelProfile' })

const message = useMessage()
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const editorVisible = ref(false)
const profile = ref<HotelProfileVO>({})
const formData = ref<HotelProfileUpdateReqVO>({})
const imageList = ref<string[]>([])
const formRef = ref()
const rules = {
  name: [{ required: true, message: '酒店名称不能为空', trigger: 'blur' }],
  contactMobile: [
    {
      pattern: /^(?:1[3-9]\d{9}|[0-9+()\-\s]{6,32})$/,
      message: '联系电话格式不正确',
      trigger: 'blur'
    }
  ]
}

const parseImages = (value?: string): string[] => {
  if (!value?.trim()) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === 'string') : []
  } catch {
    return value.split(',').map((item) => item.trim()).filter(Boolean)
  }
}

const loadProfile = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    profile.value = await SaasMerchantApi.getMerchantContextDetail()
  } catch (error: any) {
    errorMessage.value = error?.message || '酒店资料加载失败'
  } finally {
    loading.value = false
  }
}

const openEditor = () => {
  formData.value = {
    name: profile.value.name,
    logoUrl: profile.value.logoUrl,
    coverUrl: profile.value.coverUrl,
    imageUrls: profile.value.imageUrls,
    videoUrl: profile.value.videoUrl,
    videoCoverUrl: profile.value.videoCoverUrl,
    themeColor: profile.value.themeColor || '#0f766e',
    licenseImageUrl: profile.value.licenseImageUrl,
    qualificationImageUrl: profile.value.qualificationImageUrl,
    contactName: profile.value.contactName,
    contactMobile: profile.value.contactMobile,
    address: profile.value.address,
    longitude: profile.value.longitude,
    latitude: profile.value.latitude,
    description: profile.value.description
  }
  imageList.value = parseImages(profile.value.imageUrls)
  editorVisible.value = true
}

const saveProfile = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    profile.value = await SaasMerchantApi.updateMerchantContext({
      ...formData.value,
      imageUrls: imageList.value.length ? JSON.stringify(imageList.value) : undefined
    })
    editorVisible.value = false
    message.success('酒店资料已保存')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.hotel-profile__header {
  display: flex;
  padding: 24px;
  margin-bottom: 16px;
  color: #fff;
  background: #153f35;
  border-bottom: 3px solid #c9a14a;
  border-radius: 6px;
  align-items: center;
  justify-content: space-between;
}

.hotel-profile__header span,
.hotel-profile__header p {
  margin: 0;
  color: #cbded8;
}

.hotel-profile__header h1 {
  margin: 8px 0;
  font-size: 28px;
  letter-spacing: 0;
}

.hotel-profile__cover {
  width: 260px;
  height: 140px;
  border-radius: 6px;
}

.hotel-profile__coordinates {
  display: flex;
  gap: 12px;
}
</style>
