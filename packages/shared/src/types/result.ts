export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

export function isOk<T>(r: Result<T>): boolean {
  return r.code === 0
}
