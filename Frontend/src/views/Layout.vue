<template>
  <div class="app-container">
    <el-header class="app-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <el-icon><Location /></el-icon>
          <span>个性化旅游系统</span>
        </div>
        <el-menu
          mode="horizontal"
          :router="true"
          :default-active="$route.path"
          class="nav-menu"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            首页
          </el-menu-item>
          <el-menu-item index="/recommend">
            <el-icon><Star /></el-icon>
            智能推荐
          </el-menu-item>
          <el-menu-item index="/route-plan">
            <el-icon><MapLocation /></el-icon>
            路线规划
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            设施查询
          </el-menu-item>
          <el-menu-item index="/diary">
            <el-icon><Document /></el-icon>
            旅行日记
          </el-menu-item>
          <el-menu-item index="/food">
            <el-icon><Food /></el-icon>
            美食推荐
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            个人中心
          </el-menu-item>
        </el-menu>
        <div class="user-info">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="user-name">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">
              登录
            </el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = async () => {
  await userStore.logoutAction()
  router.push('/')
}
</script>

<style lang="scss" scoped>
.app-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  color: white;
  cursor: pointer;
  
  .el-icon {
    font-size: 24px;
  }
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
  background: transparent;
  border: none;
  
  .el-menu-item {
    color: rgba(255, 255, 255, 0.9);
    
    &:hover, &.is-active {
      color: white;
      background: rgba(255, 255, 255, 0.2);
    }
  }
}

.user-info {
  .user-name {
    display: flex;
    align-items: center;
    gap: 4px;
    color: white;
    cursor: pointer;
  }
}

.app-main {
  flex: 1;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
