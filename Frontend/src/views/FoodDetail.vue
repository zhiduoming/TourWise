<template>
  <div class="food-detail-container">
    <AppHeader />

    <el-row :gutter="24" v-loading="loading">
      <!-- 左侧详情 -->
      <el-col :xs="24" :lg="16">
        <!-- 美食图片 -->
        <el-card class="image-card" shadow="hover">
          <div class="image-admin-shell">
            <el-image
              :src="foodDetail.image || '/food-placeholder.jpg'"
              fit="cover"
              class="food-image"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
              </template>
            </el-image>
            <AdminImageUpload
              class="image-upload-overlay"
              target-type="food"
              :target-id="foodDetail.id || route.params.id"
              @success="handleFoodImageUploaded"
            />
          </div>
        </el-card>

        <!-- 美食信息 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h1 class="food-name">{{ foodDetail.name }}</h1>
              <el-tag type="success" size="large">{{ foodDetail.cuisine_type }}</el-tag>
            </div>
          </template>

          <div class="food-meta">
            <div class="meta-item">
              <el-icon><Star /></el-icon>
              <span class="rating">{{ foodDetail.rating || 5.0 }}</span>
            </div>
            <div class="meta-item">
              <el-icon><TrendCharts /></el-icon>
              <span>热度 {{ foodDetail.hotness || 0 }}</span>
            </div>
            <div class="meta-item" v-if="foodDetail.spot_name">
              <el-icon><Location /></el-icon>
              <span>{{ foodDetail.spot_name }}</span>
            </div>
          </div>

          <div class="food-description">
            <h3>美食介绍</h3>
            <p>{{ foodDetail.description || '暂无详细介绍' }}</p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧地图 -->
      <el-col :xs="24" :lg="8">
        <el-card class="map-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <h3>位置信息</h3>
            </div>
          </template>
          <div class="map-placeholder">
            <el-icon><Location /></el-icon>
            <span>地图加载中...</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户日志区域 -->
    <el-card class="logs-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>
            <el-icon><Document /></el-icon>
            用户日志
          </h2>
        </div>
      </template>

      <div class="log-list" v-loading="diaryLoading">
        <el-empty v-if="!diaryList.length" description="暂无日志" />
        
        <div v-else>
          <div class="log-item" v-for="log in diaryList" :key="log.id">
            <div class="log-header">
              <el-avatar :size="40" :icon="User" />
              <div class="log-author">
                <span class="author-name">{{ log.username }}</span>
                <span class="log-time">{{ formatDate(log.created_at) }}</span>
              </div>
            </div>
            <h4 class="log-title">{{ log.title }}</h4>
            <p class="log-content">{{ truncateContent(log.content) }}</p>
            <div class="log-footer">
              <span><el-icon><Star /></el-icon> {{ log.rating }}</span>
              <span><el-icon><View /></el-icon> {{ log.hotness }}</span>
            </div>
          </div>

          <el-pagination
            v-if="diaryTotal > pageSize"
            v-model:current-page="diaryPage"
            :page-size="pageSize"
            :total="diaryTotal"
            layout="prev, pager, next"
            @current-change="loadLogs"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getFoodDetail } from '@/api/food'
import { getLogList } from '@/api/log'
import { Picture, Star, TrendCharts, Location, Document, User, View } from '@element-plus/icons-vue'

const route = useRoute()
const loading = ref(true)
const diaryLoading = ref(false)
const foodDetail = ref({})
const diaryList = ref([])
const diaryPage = ref(1)
const pageSize = ref(5)
const diaryTotal = ref(0)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const truncateContent = (content, maxLength = 150) => {
  if (!content) return ''
  return content.length > maxLength ? content.substring(0, maxLength) + '...' : content
}

const loadFoodDetail = async () => {
  loading.value = true
  try {
    const res = await getFoodDetail(route.params.id)
    if (res.data) {
      foodDetail.value = {
        ...res.data,
        rating: parseFloat(res.data.rating) || 5.0,
        image: res.data.image || ''
      }
    }
  } catch (error) {
    console.error('获取美食详情失败:', error)
    foodDetail.value = {
      id: route.params.id,
      name: '示例美食',
      cuisine_type: '川菜',
      rating: 4.5,
      hotness: 2000,
      spot_name: '学生第一食堂',
      description: '这是一道美味的川菜'
    }
  } finally {
    loading.value = false
  }
}

const loadLogs = async () => {
  diaryLoading.value = true
  try {
    const res = await getLogList({
      spotId: route.params.id,
      page: diaryPage.value,
      pageSize: pageSize.value
    })
    diaryList.value = res.data?.list || []
    diaryTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载日志失败:', error)
    diaryList.value = []
  } finally {
    diaryLoading.value = false
  }
}

const handleFoodImageUploaded = (imageUrl) => {
  if (imageUrl) {
    foodDetail.value.image = imageUrl
  }
}

onMounted(() => {
  loadFoodDetail()
  loadLogs()
})
</script>

<style lang="scss" scoped>
.food-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.image-card {
  margin-bottom: 20px;
}

.image-admin-shell {
  position: relative;
}

.food-image {
  width: 100%;
  height: 400px;
}

.image-upload-overlay {
  position: absolute;
  right: 16px;
  top: 16px;
  z-index: 2;
}

.info-card {
  .food-name {
    font-size: 28px;
    margin: 0;
  }

  .food-meta {
    display: flex;
    gap: 20px;
    margin: 20px 0;
    padding: 15px 0;
    border-top: 1px solid #f0f0f0;
    border-bottom: 1px solid #f0f0f0;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #606266;

      .rating {
        color: #f7ba2a;
        font-weight: bold;
        font-size: 18px;
      }
    }
  }

  .food-description {
    h3 {
      margin: 0 0 15px 0;
      color: #303133;
    }

    p {
      color: #606266;
      line-height: 1.8;
    }
  }
}

.logs-section {
  margin-top: 30px;

  .log-item {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .log-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 15px;

      .log-author {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .author-name {
          font-weight: 500;
          color: #303133;
        }

        .log-time {
          font-size: 13px;
          color: #909399;
        }
      }
    }

    .log-title {
      margin: 0 0 10px 0;
      color: #303133;
    }

    .log-content {
      color: #606266;
      margin: 0 0 15px 0;
      line-height: 1.6;
    }

    .log-footer {
      display: flex;
      gap: 20px;
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>
