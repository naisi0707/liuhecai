export interface IpWhitelistEntry {
  id?: string
  cidr: string
  note?: string | null
}

export interface IpWhitelistVO {
  enabled: boolean
  currentIp?: string | null
  entries: IpWhitelistEntry[]
}
