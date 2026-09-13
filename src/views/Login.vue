<template>
  <div class="login-page">
    <!-- 节日装饰：纯色圆点与灯笼（非渐变） -->
    <span class="deco deco-circle-tl" />
    <span class="deco deco-circle-br" />
    <span class="deco deco-lantern-l">🏮</span>
    <span class="deco deco-lantern-r">🏮</span>

    <div class="login-box">
      <div class="login-logo">抽</div>
      <h1 class="login-title">大营销抽奖平台</h1>
      <p class="login-sub">输入用户 ID 即可参与活动</p>
      <el-input
        v-model="userId"
        size="large"
        placeholder="请输入用户 ID"
        maxlength="32"
        class="login-input"
        @keyup.enter="onLogin"
      />
      <el-button
        type="primary"
        size="large"
        class="login-btn"
        :disabled="!userId.trim()"
        @click="onLogin"
      >
        进入活动
      </el-button>
      <el-button text size="small" class="random-btn" @click="onRandom">
        没有账号？随机生成一个
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const userId = ref('')

function onLogin() {
  const id = userId.value.trim()
  if (!id) return
  userStore.login(id)
  ElMessage.success(`欢迎，${id}`)
  router.push('/')
}

function onRandom() {
  userId.value = `user_${Math.random().toString(36).slice(2, 8)}`
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  position: relative;
  overflow: hidden;
}

/* 节日装饰 */
.deco {
  position: absolute;
  pointer-events: none;
}

.deco-circle-tl {
  top: -80px;
  left: -80px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: var(--brand-cream);
  border: 2px dashed var(--brand-gold);
}

.deco-circle-br {
  bottom: -100px;
  right: -100px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: var(--brand-cream);
  border: 2px dashed var(--brand-gold);
}

.deco-lantern-l {
  top: 32px;
  left: 8%;
  font-size: 32px;
  animation: sway 3s ease-in-out infinite;
}

.deco-lantern-r {
  top: 32px;
  right: 8%;
  font-size: 32px;
  animation: sway 3s ease-in-out 0.6s infinite;
}

@keyframes sway {
  0%, 100% {
    transform: rotate(-6deg);
  }
  50% {
    transform: rotate(6deg);
  }
}

.login-box {
  width: 90%;
  max-width: 360px;
  background: var(--brand-cream);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  padding: 36px 28px;
  text-align: center;
  box-shadow: 0 12px 32px rgba(91, 58, 41, 0.15);
  position: relative;
  z-index: 1;
}

/* 印章 Logo：红底金边“抽”字 */
.login-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: var(--brand-red);
  border: 3px solid var(--brand-gold);
  color: var(--brand-gold-light);
  font-size: 30px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(168, 30, 30, 0.35);
}

.login-title {
  margin: 0;
  font-size: 24px;
  color: var(--brand-red-dark);
  letter-spacing: 2px;
}

.login-sub {
  margin: 8px 0 24px;
  font-size: 13px;
  color: #999;
}

.login-input {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 4px;
}

.random-btn {
  margin-top: 12px;
  color: var(--brand-gold-deep);
}
</style>
