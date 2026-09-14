<template>
  <div class="login-page">
    <span class="deco deco-lantern-l">🏮</span>
    <span class="deco deco-lantern-r">🏮</span>

    <div class="login-box">
      <div class="login-logo">抽</div>
      <h1 class="login-title">大营销抽奖平台</h1>
      <el-segmented v-model="mode" :options="modeOptions" class="mode-switch" />
      <el-input
        v-model="accountName"
        size="large"
        placeholder="账号"
        maxlength="32"
        class="login-input"
        :prefix-icon="User"
        autocomplete="username"
      />
      <el-input
        v-model="password"
        size="large"
        type="password"
        show-password
        placeholder="密码"
        maxlength="72"
        class="login-input"
        :prefix-icon="Lock"
        :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
        @keyup.enter="onSubmit"
      />
      <el-input
        v-if="mode === 'register'"
        v-model="confirmPassword"
        size="large"
        type="password"
        show-password
        placeholder="确认密码"
        maxlength="72"
        class="login-input"
        :prefix-icon="Lock"
        autocomplete="new-password"
        @keyup.enter="onSubmit"
      />
      <el-button
        type="primary"
        size="large"
        class="login-btn"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="onSubmit"
      >
        {{ mode === 'login' ? '登录' : '注册并登录' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { loginAccount, registerAccount } from '../api/raffle'

const router = useRouter()
const userStore = useUserStore()
const mode = ref('login')
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' }
]
const accountName = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

const canSubmit = computed(() => {
  if (!accountName.value.trim() || password.value.length < 6) return false
  return mode.value === 'login' || password.value === confirmPassword.value
})

async function onSubmit() {
  if (!canSubmit.value || submitting.value) return
  if (mode.value === 'register' && password.value !== confirmPassword.value) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    const account = accountName.value.trim()
    const session = mode.value === 'login'
      ? await loginAccount(account, password.value)
      : await registerAccount(account, password.value)
    userStore.login(session)
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    await router.push('/')
  } catch (e) {
    // 业务错误由请求拦截器统一展示。
  } finally {
    submitting.value = false
  }
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
  border-radius: 8px;
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

.mode-switch {
  width: 100%;
  margin: 20px 0 16px;
}

.login-input {
  margin-bottom: 16px;
}

.login-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 4px;
}

</style>
