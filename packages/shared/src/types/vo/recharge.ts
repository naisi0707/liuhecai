export type RechargeVO = {
  id: string
  username?: string
  amount: number
  payChannel?: string | null
  remark?: string | null
  status: number
  statusLabel: string
  rejectReason?: string | null
  coinBalance?: number | null
  createdAt?: string
  handledAt?: string | null
}
