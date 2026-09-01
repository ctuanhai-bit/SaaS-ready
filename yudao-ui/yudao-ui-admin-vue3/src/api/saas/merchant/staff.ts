import request from '@/config/axios'

/** 商户人员分页查询参数 */
export interface MerchantStaffPageReqVO extends PageParam {
  username?: string
  mobile?: string
  status?: number
  createTime?: [string, string]
}

/** 商户人员响应 */
export interface MerchantStaffRespVO {
  id: number
  username: string
  nickname: string
  mobile: string
  status: number
  createTime: string
}

/** 商户人员保存（创建/编辑共用）*/
export interface MerchantStaffSaveReqVO {
  id?: number
  username: string
  nickname: string
  mobile?: string
  password?: string
}

/** 商户人员状态更新 */
export interface MerchantStaffUpdateStatusReqVO {
  id: number
  status: number
}

export const MerchantStaffApi = {
  /** 分页查询当前商户人员 */
  getStaffPage: async (params: MerchantStaffPageReqVO): Promise<PageResult<MerchantStaffRespVO>> => {
    return await request.get({ url: '/merchant/staff/page', params })
  },

  /** 新增商户人员 */
  createStaff: async (data: MerchantStaffSaveReqVO): Promise<number> => {
    return await request.post({ url: '/merchant/staff/create', data })
  },

  /** 编辑商户人员 */
  updateStaff: async (data: MerchantStaffSaveReqVO): Promise<boolean> => {
    return await request.put({ url: '/merchant/staff/update', data })
  },

  /** 启停商户人员状态 */
  updateStaffStatus: async (data: MerchantStaffUpdateStatusReqVO): Promise<boolean> => {
    return await request.put({ url: '/merchant/staff/update-status', data })
  }
}
