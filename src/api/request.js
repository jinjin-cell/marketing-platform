import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一错误码文案映射（对应后端 ResponseCode 与 ERR_BIZ_*）
const ERROR_MESSAGES = {
  '0001': '网络异常，请稍后重试',
  '0002': '参数错误',
  '0003': '操作已受理，请勿重复提交',
  '0004': '活动太火爆，请稍后再试',
  '0005': '操作太频繁啦，休息一下再试',
  '0006': '服务繁忙，请稍后再试',
  ERR_BIZ_001: '策略规则权重规则未配置',
  ERR_BIZ_002: '活动准备中，请稍后重试',
  ERR_BIZ_003: '活动未开启',
  ERR_BIZ_004: '活动不在日期范围内',
  ERR_BIZ_005: '活动库存不足',
  ERR_BIZ_006: '抽奖总额度不足，去签到或兑换吧',
  ERR_BIZ_007: '本月抽奖次数已用完',
  ERR_BIZ_008: '今日抽奖次数已用完，明天再来或去兑换',
  ERR_BIZ_009: '请勿重复抽奖',
  ERR_BIZ_010: '操作太频繁，请稍后重试',
  ERR_BIZ_011: '操作太频繁，请稍后重试'
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

request.interceptors.request.use((config) => config)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === '0000') {
      return res.data
    }
    const code = res?.code
    const message = ERROR_MESSAGES[code] || res?.info || '请求失败，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(Object.assign(new Error(message), { code, info: res?.info }))
  },
  (error) => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else if (!error.code || !ERROR_MESSAGES[error.code]) {
      ElMessage.error('网络异常，请确认后端服务已启动')
    }
    return Promise.reject(error)
  }
)

export default request
