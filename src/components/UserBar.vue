<template>
  <div class="user-bar">
    <!-- 节日横幅标题 -->
    <div class="banner">
      <span class="banner-lantern">🏮</span>
      <span class="banner-title">幸运大转盘</span>
      <span class="banner-lantern">🏮</span>
    </div>

    <div class="user-info">
      <div class="user-left">
        <span class="avatar">{{ avatarText }}</span>
        <span class="user-id">{{ userStore.userId }}</span>
      </div>
      <el-tag size="small" effect="dark" round class="logout-tag" @click="onLogout">退出</el-tag>
    </div>

    <div class="assets">
      <div class="asset-item">
        <span class="asset-num">{{ creditText }}</span>
        <span class="asset-label">积分余额</span>
      </div>
      <div class="asset-divider" />
      <div class="asset-item">
        <span class="asset-num">{{ userStore.usableCount }}</span>
        <span class="asset-label">剩余抽奖次数</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const router = useRouter()

const creditText = computed(() =>
  userStore.credit === null ? '--' : Number(userStore.credit).toFixed(0)
)

// 头像取用户 ID 尾号两位，纯展示用途
const avatarText = computed(() => {
  const id = String(userStore.userId || '')
  return id.length > 2 ? id.slice(-2) : id || '--'
})

onMounted(() => {
  userStore.refreshCredit().catch(() => {})
  userStore.refreshAccount().catch(() => {})
})

async function onLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.user-bar {
  background: var(--brand-red);
  color: #fff;
  padding: 10px 16px 16px;
  border-bottom: 3px solid var(--brand-gold);
}

/* 节日横幅 */
.banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 6px 0 12px;
}

.banner-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 6px;
  color: var(--brand-gold-light);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.25);
}

.banner-lantern {
  font-size: 16px;
  animation: sway 3s ease-in-out infinite;
}

@keyframes sway {
  0%, 100% {
    transform: rotate(-6deg);
  }
  50% {
    transform: rotate(6deg);
  }
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.user-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-gold);
  color: var(--brand-red-deep);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--brand-gold-light);
}

.user-id {
  font-size: 15px;
  font-weight: 600;
}

.logout-tag {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.2);
  border-color: transparent;
  color: #fff;
}

.logout-tag:hover {
  background: rgba(255, 255, 255, 0.35);
}

/* 资产面板：金边票券风 */
.assets {
  display: flex;
  align-items: center;
  background: var(--brand-red-deep);
  border: 1px dashed var(--brand-gold);
  border-radius: 12px;
  padding: 12px 0;
}

.asset-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.asset-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--brand-gold-light);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.asset-label {
  font-size: 12px;
  opacity: 0.85;
}

.asset-divider {
  width: 1px;
  height: 28px;
  background: rgba(255, 216, 115, 0.4);
}
</style>
