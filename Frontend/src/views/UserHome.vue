<template>
  <div class="user-home-container">
    <AppHeader />

    <div v-loading="loading" class="content">
      <!-- 用户信息卡片 -->
      <el-card class="user-card" shadow="hover">
        <div class="user-header">
          <el-avatar :size="100" :src="userInfo.avatar || defaultAvatar" class="avatar" />
          <div class="user-info">
            <h1 class="username">{{ userInfo.nickname || userInfo.username || '未知用户' }}</h1>
            <p class="signature">{{ userInfo.signature || '这个人很懒，什么都没写~' }}</p>
            <div class="user-meta">
              <span class="meta-item">
                <el-icon><Document /></el-icon>
                {{ diaryCount }} 篇日记
              </span>
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ isSelf ? '自己' : '访客' }}
              </span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 用户的日记列表 -->
      <el-card class="diary-card" shadow="never" style="margin-top: 20px">
        <template #header>
          <h3 class="card-title">
            <el-icon><Document /></el-icon>
            {{ isSelf ? '我的日记' : 'TA 的日记' }}
          </h3>
        </template>

        <div v-loading="diaryLoading" class="diary-list">
          <el-empty v-if="diaryList.length === 0" :description="isSelf ? '你还没有写日记哦~' : 'TA 还没有写日记'" />
          
          <el-timeline v-else>
            <el-timeline-item
              v-for="diary in diaryList"
              :key="diary.id"
              :timestamp="formatDate(diary.created_at)"
              placement="top"
            >
              <el-card class="diary-item" shadow="hover" @click="viewDiaryDetail(diary)">
                <div class="diary-header">
                  <h4 class="diary-title">{{ diary.title }}</h4>
                  <el-tag size="small" type="success">
                    <el-icon><Star /></el-icon>
                    {{ diary.rating }}
                  </el-tag>
                </div>
                <p class="diary-content">{{ diary.content }}</p>
                <div v-if="diary.images?.length" class="diary-images">
                  <el-image
                    v-for="(img, index) in diary.images"
                    :key="`${diary.id}-${index}`"
                    :src="img"
                    class="diary-image"
                    fit="cover"
                    :preview-src-list="diary.images"
                    :initial-index="index"
                    preview-teleported
                    @click.stop
                  />
                </div>
                <div class="diary-footer">
                  <span class="diary-hotness">
                    <el-icon><View /></el-icon>
                    {{ diary.hotness }}
                  </span>
                  <el-button size="small" type="primary" text>
                    查看详情
                  </el-button>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>

          <!-- 分页 -->
          <el-pagination
            v-if="diaryTotal > pageSize"
            v-model:current-page="diaryPage"
            :page-size="pageSize"
            :total="diaryTotal"
            layout="prev, pager, next"
            @current-change="loadUserDiaries"
            class="pagination"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import { getLogList } from '@/api/log'
import { Document, User, Star, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const userInfo = ref({
  id: null,
  username: '',
  nickname: '',
  signature: '',
  avatar: ''
})

const diaryLoading = ref(false)
const diaryList = ref([])
const diaryPage = ref(1)
const pageSize = ref(10)
const diaryTotal = ref(0)
const diaryCount = ref(0)

// 判断是否是当前登录用户自己
const isSelf = computed(() => {
  const currentUserId = userStore.userInfo?.id
  const viewUserId = parseInt(route.params.userId)
  return currentUserId && (currentUserId === viewUserId || currentUserId.toString() === route.params.userId)
})

const loadUserInfo = async () => {
  loading.value = true
  try {
    const userId = route.params.userId
    
    if (isSelf.value) {
      // 如果是自己，使用当前用户信息
      userInfo.value = {
        id: userStore.userInfo?.id,
        username: userStore.userInfo?.username,
        nickname: userStore.userInfo?.nickname || userStore.userInfo?.username,
        signature: userStore.userInfo?.signature || '暂无签名',
        avatar: userStore.userInfo?.avatar
      }
    } else {
      // 如果是其他用户，从 store 或模拟数据获取
      // 实际项目中应该调用 API 获取用户信息
      userInfo.value = {
        id: parseInt(userId),
        username: `user${userId}`,
        nickname: `用户${userId}`,
        signature: '这个用户很懒，什么都没写~',
        avatar: defaultAvatar
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const loadUserDiaries = async () => {
  diaryLoading.value = true
  try {
    const res = await getLogList({
      page: diaryPage.value,
      pageSize: pageSize.value,
      userId: route.params.userId  // 传递用户 ID，后端会过滤
    })
    diaryList.value = res.data?.list || []
    diaryTotal.value = res.data?.total || 0
    diaryCount.value = diaryTotal.value
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载日志失败')
    diaryList.value = []
    diaryTotal.value = 0
    diaryCount.value = 0
  } finally {
    diaryLoading.value = false
  }
}

const viewDiaryDetail = (diary) => {
  router.push({ path: '/diary', query: { view: diary.id } })
}

const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  // 检查是否登录
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录查看')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  
  loadUserInfo()
  loadUserDiaries()
})
</script>

<style lang="scss" scoped>
.user-home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 80px);
}

.content {
  margin-top: 20px;
}

.user-card {
  .user-header {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 20px 0;
  }

  .avatar {
    border: 4px solid #f0f2f5;
    flex-shrink: 0;
  }

  .user-info {
    flex: 1;
  }

  .username {
    font-size: 28px;
    color: #303133;
    margin: 0 0 8px 0;
    font-weight: 600;
  }

  .signature {
    color: #909399;
    font-size: 15px;
    line-height: 1.6;
    margin: 0 0 16px 0;
  }

  .user-meta {
    display: flex;
    gap: 24px;
  }

  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #606266;
    font-size: 14px;
  }
}

.diary-card {
  .card-title {
    font-size: 18px;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.diary-list {
  min-height: 300px;
}

.diary-item {
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .diary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .diary-title {
    font-size: 18px;
    color: #303133;
    margin: 0;
  }

  .diary-content {
    color: #606266;
    line-height: 1.6;
    margin-bottom: 12px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .diary-images {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
    margin-bottom: 12px;
  }

  .diary-image {
    width: 100%;
    height: 120px;
    border-radius: 8px;
    cursor: pointer;
    background: #f5f7fa;
  }

  .diary-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #909399;
    font-size: 14px;
  }

  .diary-hotness {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
