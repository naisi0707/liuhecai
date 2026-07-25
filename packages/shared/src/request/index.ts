export {
  ApiError,
  UNAUTHORIZED_CODE,
  request,
  setApiBaseURL,
  setForwardedHostResolver,
  setRequestErrorHandler,
  setUnauthorizedHandler,
  notifyUnauthorized,
  consumeUnauthorizedResult,
  setTenantHostOverride,
  getTenantHostOverride,
  buildApiHeaders,
} from './client'
export type { RequestOptions } from './client'
