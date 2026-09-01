import { readonly, ref } from 'vue'
import { getPlatformBrand } from '@/api/infra/config'
import { useAppStoreWithOut } from '@/store/modules/app'

export interface PlatformBrand {
  name: string
  logoUrl: string
  loginBackgroundUrl: string
  loginSubtitle: string
  loginEyebrow: string
  loginHeadline: string
}

export const PLATFORM_BRAND_CONFIG_KEY = 'saas.platform.brand'
export const DEFAULT_PLATFORM_BRAND: PlatformBrand = {
  name: 'Hotel PMS Community',
  logoUrl: '',
  loginBackgroundUrl: '/login-hotel-lobby.webp',
  loginSubtitle: '开放的酒店运营管理系统',
  loginEyebrow: '前台、房态与订单',
  loginHeadline: '把酒店日常运营\n放在一处管理'
}

const platformBrand = ref<PlatformBrand>({ ...DEFAULT_PLATFORM_BRAND })
let loaded = false
let loadingPromise: Promise<PlatformBrand> | undefined

const assetCdnUrl = String(import.meta.env.VITE_APP_ASSET_CDN_URL || '')
  .trim()
  .replace(/\/+$/, '')

export const normalizePlatformAssetUrl = (value: unknown) => {
  const source = String(value || '').trim()
  if (!source) return ''
  try {
    const url = new URL(source)
    if (assetCdnUrl && url.hostname.includes('.cos.')) {
      return `${assetCdnUrl}${url.pathname}`
    }
    return source
  } catch {
    return source
  }
}

const normalizePlatformBrand = (value: unknown): PlatformBrand => {
  if (!value) return { ...DEFAULT_PLATFORM_BRAND }
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    const name = String((parsed as PlatformBrand)?.name || '').trim()
    return {
      name: name || DEFAULT_PLATFORM_BRAND.name,
      logoUrl: normalizePlatformAssetUrl((parsed as PlatformBrand)?.logoUrl),
      loginBackgroundUrl:
        normalizePlatformAssetUrl((parsed as PlatformBrand)?.loginBackgroundUrl) ||
        DEFAULT_PLATFORM_BRAND.loginBackgroundUrl,
      loginSubtitle:
        String((parsed as PlatformBrand)?.loginSubtitle || '').trim() ||
        DEFAULT_PLATFORM_BRAND.loginSubtitle,
      loginEyebrow:
        String((parsed as PlatformBrand)?.loginEyebrow || '').trim() ||
        DEFAULT_PLATFORM_BRAND.loginEyebrow,
      loginHeadline:
        String((parsed as PlatformBrand)?.loginHeadline || '').trim() ||
        DEFAULT_PLATFORM_BRAND.loginHeadline
    }
  } catch {
    return { ...DEFAULT_PLATFORM_BRAND }
  }
}

const applyPlatformBrand = (brand: PlatformBrand) => {
  const appStore = useAppStoreWithOut()
  const previousTitle = appStore.getTitle
  platformBrand.value = normalizePlatformBrand(brand)
  appStore.setTitle(platformBrand.value.name)
  if (typeof document !== 'undefined') {
    let favicon = document.querySelector<HTMLLinkElement>('link[rel~="icon"]')
    if (!favicon) {
      favicon = document.createElement('link')
      favicon.rel = 'icon'
      document.head.appendChild(favicon)
    }
    favicon.href = platformBrand.value.logoUrl || '/favicon.ico'
    document.title = document.title.startsWith(previousTitle)
      ? `${platformBrand.value.name}${document.title.slice(previousTitle.length)}`
      : platformBrand.value.name
  }
}

export const loadPlatformBrand = async (force = false): Promise<PlatformBrand> => {
  if (loaded && !force) return platformBrand.value
  if (loadingPromise && !force) return loadingPromise

  loadingPromise = getPlatformBrand()
    .then((value) => {
      applyPlatformBrand(normalizePlatformBrand(value))
      loaded = true
      return platformBrand.value
    })
    .catch(() => {
      applyPlatformBrand(DEFAULT_PLATFORM_BRAND)
      return platformBrand.value
    })
    .finally(() => {
      loadingPromise = undefined
    })

  return loadingPromise
}

export const usePlatformBrand = () => ({
  brand: readonly(platformBrand),
  loadPlatformBrand
})
