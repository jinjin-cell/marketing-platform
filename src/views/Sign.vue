<template>
  <div class="page">
    <UserBar />

    <div class="card sign-card">
      <div class="sign-header">
        <div>
          <div class="sign-title">每日签到</div>
          <div class="sign-sub">签到送积分，积分可兑换抽奖次数</div>
        </div>
        <el-button
          type="primary"
          round
          :loading="signing"
          :disabled="signedToday"
          @click="onSign"
        >
          {{ signedToday ? '今日已签到' : '立即签到' }}
        </el-button>
      </div>

      <el-calendar v-model="calendarDate">
        <template #date-cell="{ data }">
          <div class="date-cell" :class="{ today: data.day === todayStr() }">
            <span>{{ data.day.split('-')[2] }}</span>
            <span v-if="isSigned(data.day)" class="sign-mark">✓</span>
          </div>
        </template>
      </el-calendar>

      <div v-if="error" class="sign-error" role="alert">
        <span>{{ error }}</span>
        <el-button size="small" @click="loadSignDates">重试</el-button>
      </div>
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { calendarSignRebate, queryCalendarSignRebateList } from '../api/raffle'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const calendarDate = ref(new Date())
const signedToday = ref(false)
const signing = ref(false)
const error = ref('')
// 日历可视范围内已签到的日期，统一由服务端返回（不再是浏览器本地记录）
const signedDates = ref(new Set())
// 服务端“今天”：签到业务日期以服务端为准，浏览器时区可能与之不一致
const serverToday = ref(formatDay(new Date()))

function formatDay(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 日历一屏展示的日期区间：本月 1 号所在周的周日 ~ 本月最后一天所在周的周六 */
function visibleRange(date) {
  const first = new Date(date.getFullYear(), date.getMonth(), 1)
  const last = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  const begin = new Date(first)
  begin.setDate(first.getDate() - first.getDay())
  const end = new Date(last)
  end.setDate(last.getDate() + (6 - last.getDay()))
  return [begin, end]
}

async function loadSignDates() {
  if (!userStore.userId) return
  error.value = ''
  const [begin, end] = visibleRange(calendarDate.value)
  const beginStr = formatDay(begin)
  const endStr = formatDay(end)
  try {
    const res = await queryCalendarSignRebateList(userStore.userId, beginStr, endStr)
    signedDates.value = new Set(res?.signDates || [])
    if (res?.serverDate) {
      serverToday.value = res.serverDate
    }
    // 仅当本屏包含“今天”时才更新按钮状态，翻月不会误判为未签到
    if (serverToday.value >= beginStr && serverToday.value <= endStr) {
      signedToday.value = signedDates.value.has(serverToday.value)
    }
  } catch (e) {
    error.value = '签到记录加载失败'
  }
}

function todayStr() {
  return serverToday.value
}

function isSigned(day) {
  return signedDates.value.has(day)
}

async function onSign() {
  if (signing.value || signedToday.value) return
  signing.value = true
  try {
    await calendarSignRebate(userStore.userId)
    ElMessage.success('签到成功，积分稍后到账')
    await loadSignDates()
    // 返利为 MQ 异步发放，延迟刷新积分余额
    setTimeout(() => userStore.refreshCredit().catch(() => {}), 3000)
  } catch (e) {
    // 0003 唯一索引冲突按“今日已签到”处理
    if (e.code === '0003') {
      await loadSignDates()
    }
  } finally {
    signing.value = false
  }
}

// 切换月份时重新拉取该屏的签到记录
watch(calendarDate, (val, old) => {
  if (!old || val.getFullYear() !== old.getFullYear() || val.getMonth() !== old.getMonth()) {
    loadSignDates()
  }
})

onMounted(loadSignDates)
</script>

<style scoped>
.sign-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sign-error {
  padding: 10px 0 2px;
  color: #b42318;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.sign-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--brand-red-dark);
}

.sign-sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.date-cell {
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 今天：金色描边圆圈提示 */
.date-cell.today > span:first-child {
  width: 28px;
  height: 28px;
  line-height: 24px;
  border-radius: 50%;
  border: 2px solid var(--brand-gold);
  color: var(--brand-gold-deep);
  font-weight: 700;
  text-align: center;
}

/* 已签到：红底白勾徽章 */
.sign-mark {
  position: absolute;
  bottom: 2px;
  width: 16px;
  height: 16px;
  line-height: 16px;
  border-radius: 50%;
  background: var(--brand-red);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  text-align: center;
}

:deep(.el-calendar__body) {
  --el-calendar-cell-width: 44px;
}

:deep(.el-calendar-table td.is-selected .date-cell) {
  color: var(--brand-red);
  font-weight: 700;
}
</style>
