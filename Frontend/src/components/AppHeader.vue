<template>
  <div class="app-header">
    <div class="header-left">
      <el-button text @click="$router.push('/')">
        <el-icon><House /></el-icon>
        回到主页
      </el-button>
    </div>
    <nav class="header-nav" aria-label="主要功能导航">
      <el-button text @click="$router.push('/search')">景点查询</el-button>
      <el-button text @click="$router.push('/recommend')">智能推荐</el-button>
      <el-button text @click="$router.push('/route-plan')">路线规划</el-button>
      <el-button text @click="$router.push('/itinerary')">旅行计划</el-button>
      <el-button text class="circle-link" @click="goToCircle">
        <el-icon><ChatDotRound /></el-icon>
        圈子
      </el-button>
    </nav>
    <div class="header-right">
      <el-badge v-if="isLogin" :value="unreadCount" :hidden="unreadCount <= 0" :max="99" class="notification-badge">
        <el-button text class="notification-button" @click="goToNotifications">
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
      <el-dropdown v-if="isLogin" @command="handleCommand">
        <div class="avatar-wrapper">
          <el-avatar :size="36" :src="userStore.userInfo?.avatar || defaultAvatar" />
          <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '游客' }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item command="notifications">
              <el-icon><Bell /></el-icon>
              消息通知
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>
              设置
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="adminDashboard">
              <el-icon><DataBoard /></el-icon>
              管理后台
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="spotManage">
              <el-icon><Location /></el-icon>
              景点管理
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="logManage">
              <el-icon><Document /></el-icon>
              日志治理
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="circleManage">
              <el-icon><ChatDotRound /></el-icon>
              圈子管理
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="routeGraph">
              <el-icon><MapLocation /></el-icon>
              路网标定
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button v-else type="primary" text @click="$router.push('/login')">
        登录
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadNotificationCount } from '@/api/notification'
import { House, User, Setting, SwitchButton, ChatDotRound, MapLocation, DataBoard, Location, Bell, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const isLogin = computed(() => userStore.isLoggedIn)
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
const unreadCount = ref(0)

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.getUserInfoAction()
    } catch (error) {
      userStore.clearLoginState()
    }
  }
  await loadUnreadCount()
})

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'notifications') {
    goToNotifications()
  } else if (command === 'settings') {
    ElMessage.info('设置功能开发中')
  } else if (command === 'adminDashboard') {
    router.push('/admin')
  } else if (command === 'spotManage') {
    router.push('/admin/spots')
  } else if (command === 'logManage') {
    router.push('/admin/logs')
  } else if (command === 'circleManage') {
    router.push('/admin/circles')
  } else if (command === 'routeGraph') {
    router.push('/admin/route-graph')
  } else if (command === 'logout') {
    handleLogout()
  }
}

const loadUnreadCount = async () => {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await getUnreadNotificationCount()
    unreadCount.value = res.data?.count || 0
  } catch (error) {
    unreadCount.value = 0
  }
}

const goToNotifications = () => {
  router.push({ path: '/profile', query: { section: 'notifications' } })
}

const goToCircle = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.info('登录后可以进入圈子')
    router.push({ name: 'Login', query: { redirect: '/circle' } })
    return
  }
  router.push('/circle')
}

const handleLogout = async () => {
  await userStore.logoutAction()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style lang="scss" scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  .el-button {
    font-size: 15px;
    color: #667eea;
    
    .el-icon {
      margin-right: 4px;
    }
    
    &:hover {
      background: #f5f7fa;
    }
  }
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 6px;

  .el-button {
    color: #4b5563;
    font-weight: 500;

    &:hover {
      color: #409eff;
      background: #f5f7fa;
    }
  }

  .circle-link {
    color: #6d28d9;
    background: #f5f3ff;

    .el-icon {
      margin-right: 4px;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .notification-button {
    width: 34px;
    height: 34px;
    color: #4b5563;
    border-radius: 50%;

    &:hover {
      color: #409eff;
      background: #f5f7fa;
    }
  }

  .avatar-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    padding: 4px 12px 4px 4px;
    border-radius: 20px;
    transition: background 0.3s;
    
    &:hover {
      background: #f5f7fa;
    }
    
    .username {
      font-size: 14px;
      color: #303133;
    }
  }
}

@media (max-width: 760px) {
  .header-nav {
    display: none;
  }
}
</style>
