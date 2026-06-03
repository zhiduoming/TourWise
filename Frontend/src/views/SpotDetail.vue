<template>
  <div class="spot-detail-container">
    <AppHeader />

    <el-row :gutter="24" v-loading="loading">
      <!-- 左侧详情 -->
      <el-col :xs="24" :lg="16">
        <!-- 地图 / 图片降级 -->
        <el-card class="image-card" shadow="hover">
          <div v-show="amapReady" class="spot-map-wrap">
            <div ref="mapContainer" class="spot-map" />
            <button class="map-recenter-btn" @click="recenterMap" title="回到景点位置">
              <el-icon><Aim /></el-icon>
            </button>
          </div>
          <div v-show="!amapReady" class="image-admin-shell">
            <el-image
              :src="spotDetail.image || '/spot-placeholder.jpg'"
              fit="cover"
              class="spot-image"
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
              target-type="poi"
              :target-id="spotDetail.id || route.params.id"
              @success="handleSpotImageUploaded"
            />
          </div>
        </el-card>

        <!-- 景点信息 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <h1 class="spot-name">{{ spotDetail.name }}</h1>
              <el-tag :type="getCategoryType(spotDetail.category)">{{ spotDetail.category }}</el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="评分">
              <el-rate v-model="spotDetail.rating" disabled show-score text-color="#ff9900" />
              <span style="color: #ff9900; margin-left: 8px">{{ spotDetail.rating }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="热度">
              <el-icon><Odometer /></el-icon>
              {{ spotDetail.hotness }}
            </el-descriptions-item>
            <el-descriptions-item label="位置" :span="2">
              <el-icon><Location /></el-icon>
              经度：{{ spotDetail.longitude }}，纬度：{{ spotDetail.latitude }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <div class="description-section">
            <h3 class="section-title">景点介绍</h3>
            <p class="spot-description">{{ spotDetail.description || '暂无详细介绍' }}</p>
          </div>

          <!-- AI 智能简介 -->
          <div class="ai-summary-section">
            <div class="ai-summary-header">
              <span class="section-title">✦ AI 智能解读</span>
              <el-button
                v-if="!aiLoading && (aiSummary || aiError)"
                text
                size="small"
                @click="loadAiSummary(true)"
              >重新生成</el-button>
            </div>
            <div v-if="aiLoading" class="ai-loading">
              <el-skeleton :rows="3" animated />
            </div>
            <p v-else-if="aiSummary" class="ai-summary-text">{{ aiSummary }}</p>
            <p v-else-if="aiError" class="ai-error">{{ aiError }}</p>
          </div>

          <div class="actions">
            <el-button type="primary" size="large" @click="handleNavigate">
              导航到这里
            </el-button>
            <el-button
              size="large"
              :type="spotAction.favorite ? 'warning' : 'default'"
              :loading="actionLoading === 'favorite'"
              @click="handleToggleSpotAction('favorite')"
            >
              <el-icon><StarFilled /></el-icon>
              {{ spotAction.favorite ? '已收藏' : '收藏' }}
            </el-button>
            <el-button
              size="large"
              :type="spotAction.wantToGo ? 'success' : 'default'"
              :loading="actionLoading === 'want'"
              @click="handleToggleSpotAction('want')"
            >
              <el-icon><Flag /></el-icon>
              {{ spotAction.wantToGo ? '已想去' : '想去' }}
            </el-button>
            <el-button
              size="large"
              :type="spotAction.visited ? 'primary' : 'default'"
              :loading="actionLoading === 'visited'"
              @click="handleToggleSpotAction('visited')"
            >
              <el-icon><Check /></el-icon>
              {{ spotAction.visited ? '已去过' : '去过' }}
            </el-button>
            <el-button size="large" @click="handleShare">
              <el-icon><Share /></el-icon>
              分享
            </el-button>
          </div>
        </el-card>

        <!-- 用户旅行日志 -->
        <el-card class="diary-card" shadow="never" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                景点评价与旅行日志
              </h3>
              <el-button type="primary" size="small" @click="openReviewDialog">
                <el-icon><Plus /></el-icon>
                写评价/日记
              </el-button>
            </div>
          </template>

          <div v-loading="diaryLoading" class="diary-list">
            <el-empty v-if="diaryList.length === 0" description="还没有人写日记哦~" />
            
            <el-timeline v-else>
              <el-timeline-item
                v-for="diary in diaryList"
                :key="diary.id"
                :timestamp="formatDate(diary.created_at)"
                placement="top"
              >
                <el-card class="diary-item" shadow="hover" @click="viewLogDetail(diary)">
                  <div class="diary-header">
                    <div class="user-info" @click.stop="viewUserProfile(diary)">
                      <el-avatar :size="32" :src="defaultAvatar" />
                      <span class="username">{{ diary.username || '游客' }}</span>
                    </div>
                    <div class="diary-actions">
                      <el-tag size="small" type="success">
                        <el-icon><Star /></el-icon>
                        {{ diary.rating || '未评分' }}
                      </el-tag>
                      <el-button
                        v-if="canDeleteDiary(diary)"
                        size="small"
                        type="danger"
                        plain
                        @click.stop="handleDeleteDiary(diary)"
                      >
                        删除
                      </el-button>
                      <span
                        class="diary-like"
                        :class="{ liked: isLogLiked(diary) }"
                        @click.stop="handleLike(diary)"
                      >
                        <HeartIcon class="like-icon" />
                        {{ diary.like_count || 0 }}
                      </span>
                    </div>
                  </div>
                  <h4 class="diary-title">{{ diary.title }}</h4>
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
                  <div v-if="hasDimensionRating(diary)" class="dimension-ratings">
                    <span v-for="item in dimensionLabels" :key="item.key">
                      {{ item.label }} {{ diary[item.key] || diary[toSnakeKey(item.key)] || '-' }}
                    </span>
                  </div>
                  <div class="diary-footer">
                    <span class="diary-hotness">
                      <el-icon><View /></el-icon>
                      {{ diary.hotness }}
                    </span>
                    <span class="diary-date">
                      <el-icon><Calendar /></el-icon>
                      {{ formatDate(diary.created_at) }}
                    </span>
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
              @current-change="loadSpotDiaries"
              class="pagination"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧信息栏 -->
      <el-col :xs="24" :lg="8">
        <!-- 景点缩略图（地图模式下显示） -->
        <el-card v-if="amapReady" class="sidebar-card thumb-card" shadow="hover" style="margin-bottom: 20px">
          <div class="image-admin-shell">
            <el-image
              :src="spotDetail.image || '/spot-placeholder.jpg'"
              fit="cover"
              class="spot-thumb"
            />
            <AdminImageUpload
              class="image-upload-overlay"
              target-type="poi"
              :target-id="spotDetail.id || route.params.id"
              @success="handleSpotImageUploaded"
            />
          </div>
        </el-card>

        <!-- 快速信息 -->
        <el-card class="sidebar-card" shadow="hover">
          <template #header>
            <h3 class="sidebar-title">景点信息</h3>
          </template>
          <el-statistic title="评分" :value="spotDetail.rating" :precision="1">
            <template #suffix>/5.0</template>
          </el-statistic>
          <el-statistic title="热度" :value="spotDetail.hotness" style="margin-top: 16px" />
        </el-card>

        <!-- 推荐路线 -->
        <el-card class="sidebar-card" shadow="hover" style="margin-top: 20px">
          <template #header>
            <h3 class="sidebar-title">
              <el-icon><MapLocation /></el-icon>
              推荐路线
            </h3>
          </template>
          <el-button type="primary" plain style="width: 100%" @click="handleRoutePlan">
            查看路线
          </el-button>
        </el-card>

        <!-- 附近美食 -->
        <el-card class="sidebar-card" shadow="hover" style="margin-top: 20px">
          <template #header>
            <h3 class="sidebar-title">
              <el-icon><Food /></el-icon>
              附近美食
            </h3>
          </template>
          <div v-loading="nearbyFoodLoading" class="food-list">
            <div
              v-for="food in nearbyFoods"
              :key="food.id"
              class="food-item"
              @click="viewFoodDetail(food.id)"
            >
              <div class="food-thumb-wrap">
                <el-image :src="food.image || '/food-placeholder.jpg'" fit="cover" class="food-thumb" />
                <AdminImageUpload
                  class="thumb-upload-overlay"
                  target-type="food"
                  :target-id="food.id"
                  label="上传"
                  @success="url => handleFoodImageUploaded(food, url)"
                />
              </div>
              <div class="food-info">
                <h4 class="food-name">{{ food.name }}</h4>
                <div class="food-meta">
                  <el-rate v-model="food.rating" disabled size="small" />
                  <span class="food-type">{{ food.cuisine_type }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="nearbyFoods.length === 0" description="暂无附近美食" :image-size="60" />
          </div>
          <el-button v-if="nearbyFoods.length > 0" plain class="view-all-foods" @click="goNearbyFoodsPage">
            查看全部美食
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="reviewDialogVisible" title="写评价/日记" width="620px">
      <el-form :model="reviewForm" label-width="92px" class="review-form">
        <el-form-item label="标题">
          <el-input v-model="reviewForm.title" maxlength="100" show-word-limit placeholder="例如：沙河校区一日参观记录" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="5"
            maxlength="10000"
            show-word-limit
            placeholder="写下你对这个景点的体验，也可以只写日记不评分"
          />
        </el-form-item>
        <el-form-item label="包含评分">
          <el-switch v-model="reviewForm.withRating" active-text="带评分" inactive-text="只写日记" />
        </el-form-item>
        <el-form-item label="同步圈子">
          <el-select
            v-model="reviewForm.circleId"
            placeholder="选择已加入圈子（可选）"
            filterable
            clearable
            :loading="circleLoading"
            style="width: 100%"
          >
            <el-option
              v-for="circle in joinedCircles"
              :key="circle.id"
              :label="circle.name"
              :value="circle.id"
            />
          </el-select>
        </el-form-item>
        <template v-if="reviewForm.withRating">
          <el-form-item
            v-for="item in dimensionLabels"
            :key="item.key"
            :label="item.label"
          >
            <el-rate
              v-model="reviewForm[item.key]"
              :max="5"
              show-score
              text-color="#ff9900"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="handleSubmitReview">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import HeartIcon from '@/components/HeartIcon.vue'
import { getFacilityDetail, getSpotAiSummary } from '@/api/search'
import { getAmapConfig } from '@/api/route'
import AMapLoader from '@amap/amap-jsapi-loader'
import { createLog, deleteLog, getLogList, toggleLike } from '@/api/log'
import { getFoodList } from '@/api/food'
import { getCircleList } from '@/api/circle'
import { getSpotActionState, recordSpotBrowse, toggleSpotFavorite, toggleSpotWant, toggleSpotVisited } from '@/api/spotAction'
import { Picture, Location, Odometer, Share, Document, Plus, Star, View, Food, MapLocation, Calendar, StarFilled, Flag, Check, Aim } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const spotDetail = ref({
  id: null,
  name: '',
  category: '',
  rating: 0,
  hotness: 0,
  longitude: 0,
  latitude: 0,
  description: '',
  image: ''
})

const diaryLoading = ref(false)
const diaryList = ref([])
const diaryPage = ref(1)
const pageSize = ref(5)
const diaryTotal = ref(0)

const nearbyFoodLoading = ref(false)
const nearbyFoods = ref([])
const aiSummary = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const actionLoading = ref('')

const mapContainer = ref(null)
const amapReady = ref(false)
const amapConfig = reactive({ enabled: false, jsKey: '', securityCode: '' })
const amapInstance = ref(null)
const spotAction = reactive({
  favorite: false,
  wantToGo: false,
  visited: false
})
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const circleLoading = ref(false)
const joinedCircles = ref([])
const reviewForm = reactive({
  title: '',
  content: '',
  circleId: null,
  withRating: true,
  sceneryRating: 5,
  facilityRating: 5,
  serviceRating: 5,
  trafficRating: 5,
  valueRating: 5
})

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
const currentUserId = computed(() => userStore.userInfo?.id)

const dimensionLabels = [
  { key: 'sceneryRating', label: '景观体验' },
  { key: 'facilityRating', label: '设施完善' },
  { key: 'serviceRating', label: '服务体验' },
  { key: 'trafficRating', label: '交通便利' },
  { key: 'valueRating', label: '性价比' }
]

const getCategoryType = (category) => {
  const typeMap = {
    '高校': 'primary',
    '教学楼': 'success',
    '食堂': 'warning',
    '图书馆': 'info',
    '运动场': 'danger',
    '宿舍': ''
  }
  return typeMap[category] || ''
}

const loadSpotDetail = async () => {
  loading.value = true
  try {
    const res = await getFacilityDetail(route.params.id)
    if (res.data) {
      spotDetail.value = {
        ...res.data,
        rating: parseFloat(res.data.rating) || 5.0,
        image: res.data.image || ''
      }
    }
  } catch (error) {
    console.error('获取景点详情失败:', error)
    // 使用模拟数据
    spotDetail.value = {
      id: route.params.id,
      name: '示例景点',
      category: '教学楼',
      rating: 4.5,
      hotness: 1000,
      longitude: 116.397428,
      latitude: 39.90923,
      description: '这是一个示例景点的详细介绍。这里环境优美，设施齐全，是学习和休闲的好去处。',
      image: '/spot-placeholder.jpg'
    }
  } finally {
    loading.value = false
  }
}

const currentTargetId = () => Number(spotDetail.value.id || route.params.id)

const actionPayload = () => ({
  targetType: 'poi',
  targetId: currentTargetId()
})

const applySpotAction = (data) => {
  if (!data) return
  spotAction.favorite = Boolean(data.favorite)
  spotAction.wantToGo = Boolean(data.wantToGo)
  spotAction.visited = Boolean(data.visited)
}

const loadSpotActionState = async () => {
  if (!userStore.isLoggedIn || !currentTargetId()) {
    return
  }
  try {
    const res = await getSpotActionState(currentTargetId(), 'poi')
    applySpotAction(res.data)
  } catch (error) {
    console.error('加载景点行为状态失败:', error)
  }
}

const initSpotMap = async () => {
  try {
    const res = await getAmapConfig()
    Object.assign(amapConfig, res.data || {})
  } catch {
    return
  }
  if (!amapConfig.enabled || !amapConfig.jsKey) return

  const lng = Number(spotDetail.value.longitude)
  const lat = Number(spotDetail.value.latitude)
  if (!lng || !lat) return

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
    zoom: 16,
    center: [lng, lat]
  })
  amapInstance.value = map
  map.addControl(new AMap.Scale())
  map.addControl(new AMap.ToolBar({ position: 'RT' }))

  const marker = new AMap.Marker({
    position: [lng, lat],
    title: spotDetail.value.name
  })
  marker.setMap(map)

  const infoWindow = new AMap.InfoWindow({
    content: `<div style="padding:6px 10px;font-size:14px;font-weight:600">${spotDetail.value.name}</div>`,
    offset: new AMap.Pixel(0, -36)
  })
  infoWindow.open(map, marker.getPosition())
  marker.on('click', () => infoWindow.open(map, marker.getPosition()))
}

const recenterMap = () => {
  if (!amapInstance.value) return
  const lng = Number(spotDetail.value.longitude)
  const lat = Number(spotDetail.value.latitude)
  if (lng && lat) {
    amapInstance.value.setCenter([lng, lat])
    amapInstance.value.setZoom(16)
  }
}

const loadAiSummary = async (force = false) => {
  const id = currentTargetId()
  if (!id) return
  aiLoading.value = true
  aiError.value = ''
  try {
    const res = await getSpotAiSummary(id, force)
    aiSummary.value = res.data?.summary || ''
  } catch (e) {
    aiError.value = e?.response?.data?.message || 'AI 简介加载失败'
  } finally {
    aiLoading.value = false
  }
}

const recordCurrentSpotBrowse = async () => {
  if (!userStore.isLoggedIn || !currentTargetId()) {
    return
  }
  try {
    const res = await recordSpotBrowse(actionPayload())
    applySpotAction(res.data)
  } catch (error) {
    console.error('记录浏览历史失败:', error)
  }
}

const ensureLoginForAction = () => {
  if (localStorage.getItem('token')) {
    return true
  }
  ElMessage.warning('请先登录后再操作')
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
  return false
}

const handleToggleSpotAction = async (type) => {
  if (!ensureLoginForAction()) {
    return
  }
  const actionMap = {
    favorite: toggleSpotFavorite,
    want: toggleSpotWant,
    visited: toggleSpotVisited
  }
  const textMap = {
    favorite: ['已取消收藏', '收藏成功'],
    want: ['已取消想去', '已加入想去'],
    visited: ['已取消去过', '已标记去过']
  }
  actionLoading.value = type
  try {
    const before = type === 'favorite' ? spotAction.favorite : type === 'want' ? spotAction.wantToGo : spotAction.visited
    const res = await actionMap[type](actionPayload())
    applySpotAction(res.data)
    ElMessage.success(textMap[type][before ? 0 : 1])
  } catch (error) {
    console.error('更新景点行为失败:', error)
  } finally {
    actionLoading.value = ''
  }
}

const loadSpotDiaries = async () => {
  diaryLoading.value = true
  try {
    const spotId = parseInt(route.params.id)
    const params = {
      page: diaryPage.value,
      pageSize: pageSize.value
    }
    // 只在 spotId 是有效数字时传递
    if (spotId && !isNaN(spotId)) {
      params.spotId = spotId
    }
    const res = await getLogList(params)
    diaryList.value = res.data?.list || []
    diaryTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载日志失败：' + (error.message || '未知错误'))
    diaryList.value = []
    diaryTotal.value = 0
  } finally {
    diaryLoading.value = false
  }
}

const resetReviewForm = () => {
  reviewForm.title = ''
  reviewForm.content = ''
  reviewForm.circleId = null
  reviewForm.withRating = true
  reviewForm.sceneryRating = 5
  reviewForm.facilityRating = 5
  reviewForm.serviceRating = 5
  reviewForm.trafficRating = 5
  reviewForm.valueRating = 5
}

const openReviewDialog = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再发布评价')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (joinedCircles.value.length === 0) {
    loadJoinedCircles()
  }
  reviewDialogVisible.value = true
}

const handleSubmitReview = async () => {
  const content = reviewForm.content.trim()
  if (!content) {
    ElMessage.warning('请填写日记内容')
    return
  }
  const payload = {
    spotId: Number(route.params.id),
    circleId: reviewForm.circleId,
    title: reviewForm.title.trim() || `${spotDetail.value.name || '景点'}游览记录`,
    content
  }
  if (reviewForm.withRating) {
    dimensionLabels.forEach(item => {
      payload[item.key] = reviewForm[item.key]
    })
  }
  reviewSubmitting.value = true
  try {
    await createLog(payload)
    ElMessage.success('发布成功')
    reviewDialogVisible.value = false
    resetReviewForm()
    diaryPage.value = 1
    await loadSpotDiaries()
    await loadSpotDetail()
  } catch (error) {
    console.error('发布评价失败:', error)
  } finally {
    reviewSubmitting.value = false
  }
}

const loadJoinedCircles = async () => {
  circleLoading.value = true
  try {
    const res = await getCircleList('')
    joinedCircles.value = res.data?.joinedCircles || []
  } catch (error) {
    console.error('加载已加入圈子失败:', error)
    joinedCircles.value = []
  } finally {
    circleLoading.value = false
  }
}

const hasDimensionRating = (diary) => {
  return dimensionLabels.some(item => diary[item.key] || diary[toSnakeKey(item.key)])
}

const canDeleteDiary = (diary) => {
  return isAdmin.value || (currentUserId.value && Number(diary.user_id || diary.userId) === Number(currentUserId.value))
}

const isLogLiked = (log) => {
  return Number(log?.is_liked ?? log?.isLiked ?? 0) === 1
}

const handleDeleteDiary = async (diary) => {
  try {
    await ElMessageBox.confirm('确定删除这篇日记吗？删除后列表中将不再显示。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteLog(diary.id)
    ElMessage.success('删除成功')
    await loadSpotDiaries()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除日记失败:', error)
    }
  }
}

const toSnakeKey = (key) => key.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)

const loadNearbyFoods = async () => {
  nearbyFoodLoading.value = true
  try {
    const params = {
      limit: 5,
      spotId: route.params.id
    }
    if (spotDetail.value.placeGroupId) {
      params.placeGroupId = spotDetail.value.placeGroupId
    }
    const res = await getFoodList(params)
    nearbyFoods.value = res.data?.slice(0, 5) || []
  } catch (error) {
    console.error('加载附近美食失败:', error)
    nearbyFoods.value = Array.from({ length: 3 }, (_, i) => ({
      id: i + 1,
      name: `美食${i + 1}`,
      rating: 4 + Math.random(),
      cuisine_type: ['川菜', '粤菜', '快餐'][i % 3],
      image: '/food-placeholder.jpg'
    }))
  } finally {
    nearbyFoodLoading.value = false
  }
}

const handleNavigate = () => {
  ElMessage.success(`开始导航到 ${spotDetail.value.name}`)
  // 可以跳转到路线规划页面并设置终点
  router.push({
    path: '/route-plan',
    query: {
      spotId: spotDetail.value.id || route.params.id,
      placeGroupId: spotDetail.value.placeGroupId,
      scopeName: spotDetail.value.name,
      end: `${spotDetail.value.latitude},${spotDetail.value.longitude}`,
      endName: spotDetail.value.name
    }
  })
}

const handleShare = () => {
  ElMessage.success('分享链接已复制')
  // 可以添加分享功能
}

const handleRoutePlan = () => {
  router.push({
    path: '/route-plan',
    query: {
      placeGroupId: spotDetail.value.placeGroupId,
      scopeName: spotDetail.value.placeGroupName || spotDetail.value.name,
      endName: spotDetail.value.name
    }
  })
}

const viewFoodDetail = (foodId) => {
  router.push({ path: `/food/${foodId}` })
}

const goNearbyFoodsPage = () => {
  router.push({ path: `/spot/${route.params.id}/foods` })
}

const handleSpotImageUploaded = (imageUrl) => {
  if (imageUrl) {
    spotDetail.value.image = imageUrl
  }
}

const handleFoodImageUploaded = (food, imageUrl) => {
  if (imageUrl) {
    food.image = imageUrl
  }
}

const viewUserProfile = (diary) => {
  console.log('查看用户资料:', diary)
  // 检查是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录查看')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  
  // 如果是自己的日记，跳转到个人中心
  const currentUserId = userStore.userInfo?.id
  if (diary.user_id && currentUserId && diary.user_id === currentUserId) {
    router.push({ path: '/profile' })
  } else {
    // 跳转到其他用户的个人主页
    // 暂时使用 diary.user_id 或模拟 ID
    const userId = diary.user_id || Math.floor(Math.random() * 100)
    router.push({ path: `/user/${userId}` })
  }
}

const viewLogDetail = (diary) => {
  if (diary?.id) {
    router.push(`/log/${diary.id}`)
  }
}

// 点赞日志
const handleLike = async (diary) => {
  // 检查是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  try {
    const res = await toggleLike(diary.id)
    if (res.data?.liked) {
      diary.like_count = (diary.like_count || 0) + 1
      diary.is_liked = 1
      diary.isLiked = 1
      ElMessage.success('点赞成功')
    } else {
      diary.like_count = Math.max(0, (diary.like_count || 0) - 1)
      diary.is_liked = 0
      diary.isLiked = 0
      ElMessage.info('已取消点赞')
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
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

const initPage = () => {
  diaryPage.value = 1
  diaryList.value = []
  aiSummary.value = ''
  aiError.value = ''
  if (amapInstance.value) {
    amapInstance.value.destroy()
    amapInstance.value = null
    amapReady.value = false
  }
  loadSpotDetail().then(async () => {
    await Promise.all([loadNearbyFoods(), loadSpotActionState()])
    recordCurrentSpotBrowse()
    loadAiSummary()
    initSpotMap()
  })
  loadSpotDiaries()
  if (userStore.isLoggedIn) {
    loadJoinedCircles()
  }
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.getUserInfoAction().catch(() => userStore.clearLoginState())
  }
  initPage()
})

watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    initPage()
  }
})
</script>

<style lang="scss" scoped>
.spot-detail-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
}

.back-btn {
  margin-bottom: 16px;
}

.image-card {
  margin-bottom: 20px;
}

.image-admin-shell {
  position: relative;
}

.spot-map-wrap {
  position: relative;
  width: 100%;
  height: 500px;
  border-radius: 8px;
  overflow: hidden;
}

.spot-map {
  width: 100%;
  height: 100%;
}

.map-recenter-btn {
  position: absolute;
  bottom: 60px;
  right: 12px;
  z-index: 100;
  width: 36px;
  height: 36px;
  background: #fff;
  border: none;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #333;
  transition: background 0.2s;

  &:hover {
    background: #f0f7ff;
    color: #409eff;
  }
}

.spot-image {
  width: 100%;
  height: 500px;
  border-radius: 8px;
}

.spot-thumb {
  width: 100%;
  height: 160px;
  border-radius: 8px;
}

.image-upload-overlay {
  position: absolute;
  right: 16px;
  top: 16px;
  z-index: 2;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 48px;
  
  span {
    font-size: 14px;
    margin-top: 8px;
  }
}

.food-thumb-wrap {
  position: relative;
  flex: 0 0 auto;
}

.thumb-upload-overlay {
  position: absolute;
  right: 6px;
  bottom: 6px;
  z-index: 2;
}

.info-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .spot-name {
    font-size: 28px;
    margin: 0;
    color: #303133;
  }
}

.description-section {
  margin: 20px 0;
}

.section-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.spot-description {
  color: #606266;
  line-height: 1.8;
  font-size: 15px;
}

.ai-summary-section {
  margin: 20px 0;
  padding: 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #f5f0ff 100%);
  border-radius: 8px;
  border-left: 3px solid #409eff;

  .ai-summary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      color: #409eff;
    }
  }

  .ai-loading {
    padding: 4px 0;
  }

  .ai-summary-text {
    color: #303133;
    line-height: 1.9;
    font-size: 14px;
    margin: 0;
    white-space: pre-wrap;
  }

  .ai-error {
    color: #909399;
    font-size: 13px;
    margin: 0;
  }
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.diary-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .section-title {
    margin: 0;
  }
}

.diary-item {
  cursor: pointer;
  transition: transform 0.3s;
  
  &:hover {
    transform: translateY(-2px);
  }

  .diary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .diary-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .diary-like {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    cursor: pointer;
    transition: color 0.2s;

    &:hover,
    &.liked {
      color: #f56c6c;
    }

    &.liked .like-icon {
      fill: currentColor;
    }

    .like-icon {
      width: 16px;
      height: 16px;
      flex: 0 0 auto;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .username {
    color: #606266;
    font-size: 14px;
  }

  .diary-title {
    font-size: 18px;
    color: #303133;
    margin-bottom: 8px;
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

  .dimension-ratings {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;

    span {
      padding: 4px 8px;
      border-radius: 6px;
      background: #f5f7fa;
      color: #606266;
      font-size: 12px;
    }
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

.review-form {
  :deep(.el-rate) {
    height: 32px;
    display: flex;
    align-items: center;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.sidebar-card {
  .sidebar-title {
    font-size: 16px;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.view-all-foods {
  width: 100%;
  margin-top: 12px;
}

.food-list {
  .food-item {
    display: flex;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background 0.3s;
    
    &:hover {
      background: #f5f7fa;
    }
    
    &:last-child {
      border-bottom: none;
    }
  }

  .food-thumb {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    object-fit: cover;
  }

  .food-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .food-name {
    font-size: 15px;
    color: #303133;
    margin: 0 0 8px 0;
  }

  .food-meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .food-type {
    color: #909399;
    font-size: 13px;
  }
}
</style>
