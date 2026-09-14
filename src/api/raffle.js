import request from './request'

const FORM_HEADERS = { 'Content-Type': 'application/x-www-form-urlencoded' }

// ---------- 认证模块 /auth ----------

export function loginAccount(accountName, password) {
  return request.post('/auth/login', { accountName, password })
}

export function registerAccount(accountName, password) {
  return request.post('/auth/register', { accountName, password })
}

// ---------- 活动模块 /raffle/activity ----------

/** 活动抽奖（大转盘核心接口） */
export function draw(userId, activityId) {
  return request.post('/raffle/activity/draw', { userId, activityId })
}

/** 日历签到返利（form 表单参数） */
export function calendarSignRebate(userId) {
  return request.post('/raffle/activity/calendar_sign_rebate', new URLSearchParams({ userId }), {
    headers: FORM_HEADERS
  })
}

/** 查询今日是否已签到（form 表单参数） */
export function isCalendarSignRebate(userId) {
  return request.post('/raffle/activity/is_calendar_sign_rebate', new URLSearchParams({ userId }), {
    headers: FORM_HEADERS
  })
}

/**
 * 查询指定日期区间内的签到记录（日历展示用）
 * 返回 { serverDate, beginDate, endDate, signDates[] }，签到标记以服务端数据为准
 */
export function queryCalendarSignRebateList(userId, beginDate, endDate) {
  return request.get('/raffle/activity/query_calendar_sign_rebate_list', {
    params: { userId, beginDate, endDate }
  })
}

/** 查询用户活动账户额度（总/月/日） */
export function queryUserActivityAccount(userId, activityId) {
  return request.post('/raffle/activity/query_user_activity_account', { userId, activityId })
}

/** 查询 SKU 商品列表 */
export function querySkuProductList(activityId) {
  return request.get('/raffle/activity/query_sku_product_list', { params: { activityId } })
}

/** 查询用户积分余额 */
export function queryUserCredit(userId) {
  return request.get('/raffle/activity/query_user_credit', { params: { userId } })
}

/** 积分兑换活动商品 */
export function creditPayExchangeSku(userId, sku) {
  return request.post('/raffle/activity/credit_pay_exchange_sku', { userId, sku })
}

/** 查询用户中奖记录列表 */
export function queryUserAwardRecordList(userId, activityId) {
  return request.get('/raffle/activity/query_user_award_record', { params: { userId, activityId } })
}

/** 查询用户积分流水（积分明细） */
export function queryUserCreditOrderList(userId, limit) {
  return request.get('/raffle/activity/query_user_credit_order_list', { params: { userId, limit } })
}

/** 查询用户活动订单（兑换/充值记录） */
export function queryUserActivityOrderList(userId) {
  return request.get('/raffle/activity/query_user_activity_order_list', { params: { userId } })
}

/** 查询活动列表（多活动切换） */
export function queryActivityList() {
  return request.get('/raffle/activity/query_activity_list')
}

// ---------- 策略模块 /raffle/strategy ----------

/** 查询抽奖奖品列表（转盘数据源） */
export function queryRaffleAwardList(userId, activityId) {
  return request.post('/raffle/strategy/query_raffle_award_list', { userId, activityId })
}

/** 查询抽奖策略权重规则（N 次必中范围） */
export function queryRaffleStrategyRuleWeight(userId, activityId) {
  return request.post('/raffle/strategy/query_raffle_strategy_rule_weight', { userId, activityId })
}
