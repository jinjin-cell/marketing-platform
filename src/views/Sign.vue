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
    </div>

    <BottomNav />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import UserBar from '../components/UserBar.vue'
import BottomNav from '../components/BottomNav.vue'
import { calendarSignRebate, isCalendarSignRebate } from '../api/raffle'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const calendarDate = ref(new Date())
const signedToday = ref(false)
const signing = ref(false)

// 本地按天记录已签到日期（以接口返回为准，本地记录仅作日历标记）
const signedDates = ref(new Set(JSON.parse(localStorage.getItem(signKey()) || '[]')))

function signKey() {
  return `sign_dates_${userStore.userId}`
}

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function isSigned(day) {
  return signedDates.value.has(day)
}

function markSigned(day) {
  signedDates.value = new Set([...signedDates.value, day])
  localStorage.setItem(signKey(), JSON.stringify([...signedDates.value]))
}

async function onSign() {
  if (signing.value || signedToday.value) return
  signing.value = true
  try {
    await calendarSignRebate(userStore.userId)
    signedToday.value = true
    markSigned(todayStr())
    ElMessage.success('签到成功，积分稍后到账')
    // 返利为 MQ 异步发放，延迟刷新积分余额
    setTimeout(() => userStore.refreshCredit().catch(() => {}), 3000)
  } catch (e) {
    // 0003 唯一索引冲突按"今日已签到"处理
    if (e.code === '0003') {
      signedToday.value = true
      markSigned(todayStr())
    }
  } finally {
    signing.value = false
  }
}

onMounted(async () => {
  try {
    const signed = await isCalendarSignRebate(userStore.userId)
    signedToday.value = Boolean(signed)
    if (signedToday.value) {
      markSigned(todayStr())
    }
  } catch (e) {
    // 查询失败不阻塞页面
  }
})
</script>

<style scoped>
.sign-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
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
