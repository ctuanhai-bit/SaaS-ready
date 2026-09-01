export type SaasOrderStatus = number

export interface SaasPageResult<T> {
  list: T[]
  total: number
}
