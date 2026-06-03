<template>
  <div class="food-detail-container">
    <AppHeader />

    <el-row :gutter="24" v-loading="loading">
      <!-- 左侧：图片 + 基本信息 + 地图 -->
      <el-col :xs="24" :lg="16">
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

        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h1 class="food-name">{{ foodDetail.name }}</h1>
              <div class="header-actions">
                <el-tag type="success" size="large">{{ foodDetail.cuisine_type }}</el-tag>
                <el-button type="primary" :disabled="!canPlanRoute" @click="goRoutePlan">
                  <el-icon><Guide /></el-icon>
                  路线规划
                </el-button>
              </div>
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
            <div class="meta-item" v-if="foodDetail.address">
              <el-icon><MapLocation /></el-icon>
              <span>{{ foodDetail.address }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="map-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <h3>
                <el-icon><MapLocation /></el-icon>
                位置信息
              </h3>
              <el-button v-if="amapReady" size="small" link @click="recenterMap">
                <el-icon><Aim /></el-icon>居中
              </el-button>
            </div>
          </template>
          <div v-show="amapReady" ref="mapContainer" class="food-map"></div>
          <div v-show="!amapReady" class="map-placeholder">
            <el-icon><Location /></el-icon>
            <span>{{ mapHint }}</span>
          </div>
          <div v-if="foodDetail.address" class="map-address">
            <el-icon><MapLocation /></el-icon>
            {{ foodDetail.address }}
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：AI 美食介绍 -->
      <el-col :xs="24" :lg="8">
        <el-card class="ai-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <h3>
                <el-icon><MagicStick /></el-icon>
                AI 美食介绍
              </h3>
              <el-button
                size="small"
                plain
                :loading="aiLoading"
                @click="regenerateAiSummary"
                :disabled="!foodDetail.id"
              >
                <el-icon><Refresh /></el-icon>
                {{ aiSummary ? '重新生成' : '生成简介' }}
              </el-button>
            </div>
          </template>
          <el-alert v-if="aiError" :title="aiError" type="warning" :closable="false" show-icon class="ai-alert" />
          <p v-if="aiSummary" class="ai-summary">{{ aiSummary }}</p>
          <p v-else-if="!aiLoading" class="ai-empty">{{ foodDetail.description || '暂无介绍，点击上方按钮让 AI 写一段。' }}</p>
          <el-skeleton v-if="aiLoading && !aiSummary" :rows="6" animated />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="logs-section" shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>
            <el-icon><Document /></el-icon>
            用户日志
          </h2>
          <el-button type="primary" @click="openLogDialog">
            <el-icon><EditPen /></el-icon>
            写美食日志
          </el-button>
        </div>
      </template>

      <div class="log-list" v-loading="diaryLoading">
        <el-empty v-if="!diaryList.length" description="还没有人写过这家店，来当第一个分享的人" />

        <div v-else>
          <div class="log-item" v-for="log in diaryList" :key="log.id">
            <div class="log-header">
              <el-avatar :size="40" :icon="User" :src="log.userAvatar" />
              <div class="log-author">
                <span class="author-name">{{ log.username || log.userName || '匿名用户' }}</span>
                <span class="log-time">{{ formatDate(log.createdAt || log.created_at) }}</span>
              </div>
            </div>
            <h4 class="log-title">{{ log.title }}</h4>
            <p class="log-content">{{ truncateContent(log.content) }}</p>
            <div class="log-footer">
              <span><el-icon><Star /></el-icon> {{ log.rating || '-' }}</span>
              <span><el-icon><View /></el-icon> {{ log.viewCount || log.hotness || 0 }}</span>
            </div>
          </div>

          <el-pagination
            v-if="diaryTotal > pageSize"
            v-model:current-page="diaryPage"
            :page-size="pageSize"
            :total="diaryTotal"
            layout="prev, pager, next"
            @current-change="loadLogs"
            class="log-pagination"
          />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="logDialogVisible" title="写美食日志" width="640px" @open="resetLogForm">
      <el-form :model="logForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="logForm.title" placeholder="一句话标题（可选）" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="logForm.rating" :max="5" allow-half show-score />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="logForm.content"
            type="textarea"
            :rows="6"
            placeholder="说说这家店的味道、价格、环境..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="logSubmitting" @click="submitLog">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AMapLoader from '@amap/amap-jsapi-loader'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getFoodDetail, getFoodAiSummary } from '@/api/food'
import { getLogList, createLog } from '@/api/log'
import { getAmapConfig } from '@/api/route'
import {
  Picture, Star, TrendCharts, Location, Document, User, View,
  MapLocation, MagicStick, Refresh, EditPen, Aim, Guide
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const diaryLoading = ref(false)
const foodDetail = ref({})
const diaryList = ref([])
const diaryPage = ref(1)
const pageSize = ref(5)
const diaryTotal = ref(0)

// AI summary
const aiSummary = ref('')
const aiLoading = ref(false)
const aiError = ref('')

// Map
const mapContainer = ref(null)
const amapReady = ref(false)
const amapInstance = ref(null)
const amapConfig = reactive({ enabled: false, jsKey: '', securityCode: '' })
const mapHint = ref('地图加载中...')

// Log dialog
const logDialogVisible = ref(false)
const logSubmitting = ref(false)
const logForm = reactive({ title: '', content: '', rating: 5 })

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
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
      aiSummary.value = res.data.aiSummary || ''
    }
  } catch (error) {
    console.error('获取美食详情失败:', error)
  } finally {
    loading.value = false
  }
}

const loadAiSummary = async (force = false) => {
  if (!foodDetail.value.id) return
  if (!force && aiSummary.value) return
  aiLoading.value = true
  aiError.value = ''
  try {
    const res = await getFoodAiSummary(foodDetail.value.id, force)
    aiSummary.value = res.data?.summary || ''
  } catch (e) {
    aiError.value = e?.response?.data?.message || 'AI 简介加载失败，可稍后重试'
  } finally {
    aiLoading.value = false
  }
}

const regenerateAiSummary = () => loadAiSummary(true)

const canPlanRoute = computed(() => {
  const fd = foodDetail.value
  return Boolean(fd?.longitude && fd?.latitude && fd?.spot_id)
})

const goRoutePlan = () => {
  const fd = foodDetail.value
  if (!canPlanRoute.value) {
    ElMessage.warning('该美食缺少坐标或未关联景点，暂无法规划路线')
    return
  }
  router.push({
    path: '/route-plan',
    query: {
      startSpotId: fd.spot_id,
      endLng: fd.longitude,
      endLat: fd.latitude,
      endName: fd.name
    }
  })
}

const loadLogs = async () => {
  diaryLoading.value = true
  try {
    const res = await getLogList({
      foodId: route.params.id,
      page: diaryPage.value,
      pageSize: pageSize.value
    })
    diaryList.value = res.data?.list || []
    diaryTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载美食日志失败:', error)
    diaryList.value = []
  } finally {
    diaryLoading.value = false
  }
}

const initMap = async () => {
  try {
    const res = await getAmapConfig()
    Object.assign(amapConfig, res.data || {})
  } catch {
    mapHint.value = '高德地图配置未启用'
    return
  }
  if (!amapConfig.enabled || !amapConfig.jsKey) {
    mapHint.value = '高德地图未启用'
    return
  }

  const lng = Number(foodDetail.value.longitude)
  const lat = Number(foodDetail.value.latitude)
  if (!lng || !lat) {
    mapHint.value = '该美食暂无坐标'
    return
  }

  await nextTick()
  window._AMapSecurityConfig = { securityJsCode: amapConfig.securityCode || '' }
  const AMap = await AMapLoader.load({
    key: amapConfig.jsKey,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar']
  })

  amapReady.value = true
  await nextTick()

  const map = new AMap.Map(mapContainer.value, {
    zoom: 17,
    center: [lng, lat]
  })
  amapInstance.value = map
  map.addControl(new AMap.Scale())
  map.addControl(new AMap.ToolBar({ position: 'RT' }))

  const marker = new AMap.Marker({
    position: [lng, lat],
    title: foodDetail.value.name
  })
  marker.setMap(map)

  const infoWindow = new AMap.InfoWindow({
    content: `<div style="padding:6px 10px;font-size:14px;font-weight:600">${foodDetail.value.name}</div>`,
    offset: new AMap.Pixel(0, -36)
  })
  infoWindow.open(map, marker.getPosition())
  marker.on('click', () => infoWindow.open(map, marker.getPosition()))
}

const recenterMap = () => {
  if (!amapInstance.value) return
  const lng = Number(foodDetail.value.longitude)
  const lat = Number(foodDetail.value.latitude)
  if (lng && lat) {
    amapInstance.value.setCenter([lng, lat])
    amapInstance.value.setZoom(17)
  }
}

const openLogDialog = () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再发布美食日志')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  logDialogVisible.value = true
}

const resetLogForm = () => {
  logForm.title = ''
  logForm.content = ''
  logForm.rating = 5
}

const submitLog = async () => {
  if (!logForm.content.trim()) {
    ElMessage.warning('请填写内容')
    return
  }
  logSubmitting.value = true
  try {
    await createLog({
      title: logForm.title.trim() || `${foodDetail.value.name} 的探店笔记`,
      content: logForm.content.trim(),
      foodId: foodDetail.value.id,
      rating: logForm.rating
    })
    ElMessage.success('日志已发布')
    logDialogVisible.value = false
    diaryPage.value = 1
    await loadLogs()
  } catch (error) {
    console.error('发布美食日志失败:', error)
    ElMessage.error(error?.response?.data?.message || '发布失败，请稍后重试')
  } finally {
    logSubmitting.value = false
  }
}

const handleFoodImageUploaded = (imageUrl) => {
  if (imageUrl) {
    foodDetail.value.image = imageUrl
  }
}

onMounted(async () => {
  await loadFoodDetail()
  loadLogs()
  // 首访自动生成 AI 简介（仅当还没有缓存时）
  if (!aiSummary.value) {
    loadAiSummary(false)
  }
  initMap()
})

onBeforeUnmount(() => {
  if (amapInstance.value) {
    amapInstance.value.destroy?.()
    amapInstance.value = null
  }
})
</script>

<style lang="scss" scoped>
.food-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.image-card { margin-bottom: 20px; }
.image-admin-shell { position: relative; }
.food-image { width: 100%; height: 400px; }
.image-upload-overlay { position: absolute; right: 16px; top: 16px; z-index: 2; }

.info-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .food-name { font-size: 24px; margin: 0; }

  .food-meta {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #606266;

      .rating { color: #f7ba2a; font-weight: bold; font-size: 18px; }
    }
  }
}

.ai-card {
  position: sticky;
  top: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 10px;

    h3 {
      margin: 0;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 16px;
    }
  }

  .ai-summary {
    color: #303133;
    line-height: 1.85;
    background: linear-gradient(135deg, #eef2ff, #fef3c7);
    padding: 16px 18px;
    border-radius: 8px;
    margin: 0;
    white-space: pre-line;
  }

  .ai-empty {
    color: #909399;
    line-height: 1.8;
    margin: 0;
  }

  .ai-alert { margin-bottom: 10px; }
}

.map-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 16px;
      color: #303133;
    }
  }

  .food-map {
    width: 100%;
    height: 280px;
    border-radius: 6px;
    overflow: hidden;
  }

  .map-placeholder {
    height: 280px;
    background: #f5f7fa;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #909399;
    gap: 8px;
  }

  .map-address {
    margin-top: 12px;
    color: #606266;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.logs-section {
  margin-top: 30px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .log-item {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    &:last-child { border-bottom: none; }

    .log-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 15px;

      .log-author {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .author-name { font-weight: 500; color: #303133; }
        .log-time { font-size: 13px; color: #909399; }
      }
    }

    .log-title { margin: 0 0 10px 0; color: #303133; }
    .log-content { color: #606266; margin: 0 0 15px 0; line-height: 1.6; }

    .log-footer {
      display: flex;
      gap: 20px;
      color: #909399;
      font-size: 14px;

      span { display: inline-flex; align-items: center; gap: 4px; }
    }
  }

  .log-pagination {
    justify-content: center;
    margin-top: 14px;
  }
}
</style>
