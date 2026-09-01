import request from '@/config/axios'

export interface HotelProfileVO {
  id?: number
  tenantId?: number
  name?: string
  businessType?: string
  logoUrl?: string
  coverUrl?: string
  imageUrls?: string
  videoUrl?: string
  videoCoverUrl?: string
  themeColor?: string
  licenseImageUrl?: string
  qualificationImageUrl?: string
  contactName?: string
  contactMobile?: string
  address?: string
  longitude?: number
  latitude?: number
  description?: string
  status?: number
}

export type SaasMerchantVO = HotelProfileVO

export type HotelProfileUpdateReqVO = Pick<
  HotelProfileVO,
  | 'name'
  | 'logoUrl'
  | 'coverUrl'
  | 'imageUrls'
  | 'videoUrl'
  | 'videoCoverUrl'
  | 'themeColor'
  | 'licenseImageUrl'
  | 'qualificationImageUrl'
  | 'contactName'
  | 'contactMobile'
  | 'address'
  | 'longitude'
  | 'latitude'
  | 'description'
>

export const SaasMerchantApi = {
  getMerchantContext: async (): Promise<HotelProfileVO> => {
    return await request.get({ url: '/merchant/context' })
  },
  getMerchantContextDetail: async (): Promise<HotelProfileVO> => {
    return await request.get({ url: '/merchant/context/detail' })
  },
  updateMerchantContext: async (data: HotelProfileUpdateReqVO): Promise<HotelProfileVO> => {
    return await request.post({ url: '/merchant/context/update', data })
  }
}
