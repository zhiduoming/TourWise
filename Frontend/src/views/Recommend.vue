<template>
  <div class="recommend-container">
    <AppHeader />
    <section class="recommend-hero">
      <div>
        <h1>智能推荐</h1>
        <p>先描述这次出行目的，系统再结合热度、评分、行为和可规划能力给出候选目的地。</p>
      </div>
      <div class="hero-tools">
        <el-button plain :loading="preferenceLoading" @click="togglePreferencePanel">
          {{ preferenceVisible ? '收起兴趣画像' : '查看兴趣画像' }}
        </el-button>
        <el-segmented
          v-model="recommendForm.strategy"
          :options="strategyOptions"
          @change="handleStrategyChange"
        />
      </div>
    </section>

    <section v-if="preferenceVisible" class="preference-panel" v-loading="preferenceLoading">
      <div class="preference-summary">
        <div>
          <p class="panel-eyebrow">Personal Interest Profile</p>
          <h2>我的兴趣画像</h2>
          <p>{{ preferenceProfile.summary }}</p>
        </div>
        <div class="preference-stats">
          <span>兴趣标签 {{ preferenceProfile.tagCount || 0 }}</span>
          <span>行为信号 {{ preferenceProfile.signalCount || 0 }}</span>
          <span>总权重 {{ formatScore(preferenceProfile.totalWeight) }}</span>
        </div>
      </div>

      <div v-if="preferenceProfile.topTags?.length" class="preference-content">
        <div class="preference-block">
          <h3>偏好标签</h3>
          <div class="preference-tags">
            <el-tag
              v-for="tag in preferenceProfile.topTags"
              :key="tag.tagId"
              effect="plain"
              size="large"
            >
              {{ tag.tagName }} · {{ formatScore(tag.weight) }}
            </el-tag>
          </div>
        </div>
        <div class="preference-block">
          <h3>信号来源</h3>
          <div class="source-list">
            <div v-for="source in preferenceProfile.sources" :key="source.source" class="source-item">
              <span>{{ source.sourceLabel }}</span>
              <strong>{{ formatScore(source.weight) }}</strong>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无兴趣画像，先浏览或收藏几个景点" :image-size="80" />

      <el-alert
        class="preference-tip"
        :title="preferenceProfile.recommendationTip || '选择兴趣匹配后，推荐排序会叠加你的偏好标签。'"
        type="info"
        show-icon
        :closable="false"
      />
    </section>

    <section class="scene-grid">
      <button
        v-for="scene in sceneOptions"
        :key="scene.value"
        class="scene-card"
        :class="{ active: recommendForm.scene === scene.value }"
        @click="selectScene(scene.value)"
      >
        <el-icon :size="30"><component :is="scene.icon" /></el-icon>
        <span class="scene-title">{{ scene.label }}</span>
        <span class="scene-desc">{{ scene.desc }}</span>
      </button>
    </section>

    <section class="decision-panel">
      <div class="decision-main">
        <div class="decision-label">这次更偏向</div>
        <el-radio-group v-model="recommendForm.purpose" @change="handleDecisionChange">
          <el-radio-button
            v-for="purpose in purposeOptions"
            :key="purpose.value"
            :label="purpose.value"
          >
            {{ purpose.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="decision-filters">
        <el-input
          v-model="recommendForm.city"
          clearable
          placeholder="限定城市，可不填"
          @change="handleDecisionChange"
          @clear="handleDecisionChange"
        />
        <el-checkbox v-model="recommendForm.routeRequired" @change="handleDecisionChange">
          需要内部路线
        </el-checkbox>
        <el-checkbox v-model="recommendForm.foodRequired" @change="handleDecisionChange">
          顺便找美食
        </el-checkbox>
        <el-checkbox v-model="recommendForm.avoidVisited" @change="handleDecisionChange">
          避开去过
        </el-checkbox>
      </div>
    </section>

    <el-card class="result-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Star /></el-icon>
            {{ currentScene.label }}推荐
          </h3>
          <span class="result-count">{{ currentStrategyLabel }} · 共 {{ total }} 个景点</span>
        </div>
      </template>

      <el-row v-loading="loading" :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in recommendList" :key="item.id">
          <el-card class="item-card" shadow="hover">
            <div class="image-admin-shell">
              <el-image
                class="item-image"
                :src="item.image || '/placeholder.jpg'"
                fit="cover"
              />
              <AdminImageUpload
                class="image-upload-overlay"
                target-type="spot"
                :target-id="item.spotId"
                @success="url => handleItemImageUploaded(item, url)"
              />
            </div>
            <div class="item-info">
              <h4 class="item-name">{{ item.name }}</h4>
              <p class="item-desc">{{ item.description }}</p>
              <div class="item-tags">
                <el-tag size="small" v-if="item.category">{{ item.categoryName }}</el-tag>
                <el-tag size="small" type="info" v-if="item.city">{{ item.city }}</el-tag>
                <el-tag size="small" type="success" v-if="item.score">
                  <el-icon><Star /></el-icon>
                  {{ item.score }}
                </el-tag>
                <el-tag size="small" type="warning" v-if="hasRouteGraph(item)">可规划</el-tag>
                <el-tag size="small" type="danger" v-if="item.foodCount > 0">美食 {{ item.foodCount }}</el-tag>
                <el-tag size="small" v-if="item.logCount > 0">日志 {{ item.logCount }}</el-tag>
              </div>
              <div v-if="item.tagNames" class="tag-line">{{ item.tagNames }}</div>
              <div v-if="hasPersonalSignals(item)" class="personal-signals">
                <el-tag v-if="item.favoriteMatched" size="small" type="warning">已收藏</el-tag>
                <el-tag v-if="item.wantMatched" size="small" type="success">想去</el-tag>
                <el-tag v-if="item.visitedMatched" size="small" type="primary">去过</el-tag>
                <el-tag v-if="item.browseCount > 0" size="small" type="info">浏览 {{ item.browseCount }} 次</el-tag>
                <el-tag v-if="Number(item.preferenceScore || 0) > 0" size="small" type="success">
                  兴趣 {{ formatScore(item.preferenceScore) }}
                </el-tag>
                <el-tag v-if="Number(item.userRating || 0) > 0" size="small" type="warning">
                  我的评分 {{ item.userRating }}
                </el-tag>
                <el-tag v-if="item.similarActionCount > 0" size="small">同类匹配</el-tag>
              </div>
              <p class="recommend-reason">{{ item.recommendReason }}</p>
              <div class="item-meta">
                <span>热度 {{ item.hotness || 0 }}</span>
                <span v-if="item.recommendScore">推荐分 {{ Math.round(Number(item.recommendScore)) }}</span>
                <span class="item-visits">
                  <el-icon><View /></el-icon>
                  {{ item.visits }}
                </span>
              </div>
              <div class="item-actions">
                <el-button size="small" type="primary" @click="handleViewDetail(item)">
                  详情
                </el-button>
                <el-button size="small" @click="handleAddToRoute(item)">
                  加入路线
                </el-button>
                <el-button size="small" text type="danger" @click="handleDislike(item)">
                  不感兴趣
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 36]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSearch"
        @current-change="handleSearch"
        class="pagination"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="景点详情" width="600px">
      <div v-if="currentDetail" class="detail-content">
        <div class="image-admin-shell">
          <el-image :src="currentDetail.image || '/placeholder.jpg'" fit="cover" class="detail-image" />
          <AdminImageUpload
            class="image-upload-overlay"
            target-type="spot"
            :target-id="currentDetail.spotId"
            @success="url => handleItemImageUploaded(currentDetail, url)"
          />
        </div>
        <h3>{{ currentDetail.name }}</h3>
        <p>{{ currentDetail.description }}</p>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="类型">{{ currentDetail.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate v-model="currentDetail.score" disabled />
          </el-descriptions-item>
          <el-descriptions-item label="热度">{{ currentDetail.hotness }}</el-descriptions-item>
          <el-descriptions-item label="访问量">{{ currentDetail.visits }}</el-descriptions-item>
          <el-descriptions-item label="推荐理由" :span="2">
            {{ currentDetail.recommendReason }}
          </el-descriptions-item>
          <el-descriptions-item label="开放时间" :span="2">
            {{ currentDetail.openTime }} - {{ currentDetail.closeTime }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getRecommendations } from '@/api/recommend'
import { getPreferenceProfile } from '@/api/profile'
import { dislikeSpot } from '@/api/spotAction'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const recommendForm = reactive({
  strategy: 'hot',
  scene: 'all',
  purpose: 'balanced',
  city: '',
  routeRequired: false,
  foodRequired: false,
  avoidVisited: false
})

const strategyOptions = [
  { label: '热门优先', value: 'hot' },
  { label: '高评分优先', value: 'rating' },
  { label: '兴趣匹配', value: 'interest' }
]

const sceneOptions = [
  { label: '综合推荐', value: 'all', icon: 'Compass', desc: '综合热度、评分和访问量' },
  { label: '高校研学', value: 'campus', icon: 'School', desc: '适合校园参观和课设演示' },
  { label: '文化历史', value: 'culture', icon: 'Collection', desc: '博物馆、古迹与城市文化' },
  { label: '自然风景', value: 'nature', icon: 'Sunny', desc: '山水、公园和开放景区' },
  { label: '城市漫游', value: 'city', icon: 'OfficeBuilding', desc: '地标、商圈和城市街区' },
  { label: '亲子游乐', value: 'family', icon: 'MagicStick', desc: '主题乐园和轻松行程' }
]

const purposeOptions = [
  { label: '综合决策', value: 'balanced' },
  { label: '拍照打卡', value: 'photo' },
  { label: '研学参观', value: 'study' },
  { label: '美食顺路', value: 'food' },
  { label: '轻松休闲', value: 'relax' },
  { label: '路线展示', value: 'route' }
]

const loading = ref(false)
const recommendList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const detailVisible = ref(false)
const currentDetail = ref(null)
const preferenceVisible = ref(false)
const preferenceLoading = ref(false)
const preferenceProfile = ref({
  totalWeight: 0,
  tagCount: 0,
  signalCount: 0,
  summary: '',
  recommendationTip: '',
  topTags: [],
  sources: []
})

const currentScene = computed(() => sceneOptions.find(item => item.value === recommendForm.scene) || sceneOptions[0])
const currentStrategyLabel = computed(() => {
  return strategyOptions.find(item => item.value === recommendForm.strategy)?.label || '热门优先'
})

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await getRecommendations({
      ...recommendForm,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    recommendList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取推荐失败:', error)
    ElMessage.error('推荐数据加载失败，请检查后端服务和数据库')
    recommendList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const selectScene = (scene) => {
  recommendForm.scene = scene
  currentPage.value = 1
  handleSearch()
}

const handleStrategyChange = () => {
  currentPage.value = 1
  handleSearch()
}

const handleDecisionChange = () => {
  currentPage.value = 1
  handleSearch()
}

const handleViewDetail = (item) => {
  router.push(`/spot/${item.id}`)
}

const handleAddToRoute = (item) => {
  router.push({
    path: '/route-plan',
    query: {
      addPOI: item.id,
      spotId: item.id,
      placeGroupId: item.placeGroupId,
      scopeName: item.name
    }
  })
}

const handleDislike = async (item) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能调整推荐')
    router.push({ name: 'Login', query: { redirect: '/recommend' } })
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认减少“${item.name}”及相似内容的推荐吗？`,
      '不感兴趣反馈',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await dislikeSpot({
      targetType: 'poi',
      targetId: item.id
    })
    recommendList.value = recommendList.value.filter(target => target.id !== item.id)
    total.value = Math.max(0, Number(total.value || 0) - 1)
    ElMessage.success('已减少此类推荐')
    if (preferenceVisible.value) {
      await loadPreferenceProfile()
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('提交不感兴趣反馈失败:', error)
    }
  }
}

const handleItemImageUploaded = (item, imageUrl) => {
  if (imageUrl) {
    item.image = imageUrl
  }
}

const togglePreferencePanel = async () => {
  if (preferenceVisible.value) {
    preferenceVisible.value = false
    return
  }
  if (!localStorage.getItem('token')) {
    ElMessage.warning('登录后才能查看兴趣画像')
    router.push({ name: 'Login', query: { redirect: '/recommend' } })
    return
  }
  preferenceVisible.value = true
  await loadPreferenceProfile()
}

const loadPreferenceProfile = async () => {
  preferenceLoading.value = true
  try {
    const res = await getPreferenceProfile()
    preferenceProfile.value = res.data || preferenceProfile.value
  } catch (error) {
    console.error('加载兴趣画像失败:', error)
  } finally {
    preferenceLoading.value = false
  }
}

const hasPersonalSignals = (item) => {
  return item.favoriteMatched
    || item.wantMatched
    || item.visitedMatched
    || Number(item.browseCount || 0) > 0
    || Number(item.preferenceScore || 0) > 0
    || Number(item.userRating || 0) > 0
    || Number(item.similarActionCount || 0) > 0
}

const hasRouteGraph = (item) => {
  return ['draft', 'verified'].includes(item.routeGraphStatus)
}

const formatScore = (value) => {
  const number = Number(value || 0)
  return Number.isInteger(number) ? number : number.toFixed(1)
}

onMounted(() => {
  handleSearch()
})
</script>

<style lang="scss" scoped>
.recommend-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.recommend-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 28px 32px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;

  h1 {
    margin: 0 0 8px;
    font-size: 28px;
    color: #303133;
  }

  p {
    margin: 0;
    color: #606266;
    font-size: 15px;
  }
}

.hero-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.preference-panel {
  padding: 22px;
  margin-bottom: 20px;
  border: 1px solid #dbe7ff;
  border-radius: 8px;
  background: #f8fbff;
}

.preference-summary {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;

  h2 {
    margin: 4px 0 8px;
    color: #1f2a3d;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: #5f6f89;
    line-height: 1.7;
  }
}

.panel-eyebrow {
  color: #3b82f6 !important;
  font-size: 12px;
  font-weight: 800;
}

.preference-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;

  span {
    padding: 8px 10px;
    border-radius: 6px;
    background: #fff;
    color: #3b4a60;
    border: 1px solid #e3ebf8;
    font-size: 13px;
    font-weight: 700;
  }
}

.preference-content {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
  margin-top: 18px;
}

.preference-block {
  padding: 16px;
  border: 1px solid #e3ebf8;
  border-radius: 8px;
  background: #fff;

  h3 {
    margin: 0 0 12px;
    color: #263243;
    font-size: 16px;
  }
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.source-list {
  display: grid;
  gap: 10px;
}

.source-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f8;
  color: #5f6f89;

  &:last-child {
    padding-bottom: 0;
    border-bottom: none;
  }

  strong {
    color: #1f2a3d;
  }
}

.preference-tip {
  margin-top: 16px;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.scene-card {
  min-height: 132px;
  padding: 18px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;

  &:hover,
  &.active {
    border-color: #409eff;
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.14);
    transform: translateY(-2px);
  }

  &.active {
    background: #ecf5ff;
  }

  .el-icon {
    color: #409eff;
  }
}

.scene-title {
  color: #303133;
  font-size: 16px;
  font-weight: 700;
}

.scene-desc {
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.decision-panel {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
  align-items: center;
  padding: 18px 22px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.decision-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.decision-label {
  flex: 0 0 auto;
  color: #303133;
  font-weight: 700;
}

.decision-filters {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) repeat(3, auto);
  gap: 12px;
  align-items: center;
}

.result-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #303133;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-count {
  color: #909399;
  font-size: 14px;
}

.item-card {
  margin-bottom: 20px;
  
  &:hover {
    .item-image {
      transform: scale(1.05);
    }
  }
}

.item-image {
  width: 100%;
  height: 160px;
  transition: transform 0.3s;
}

.image-admin-shell {
  position: relative;
}

.image-upload-overlay {
  position: absolute;
  right: 10px;
  top: 10px;
  z-index: 2;
}

.item-info {
  padding: 12px;
}

.item-name {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-line {
  margin: -4px 0 10px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.personal-signals {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.recommend-reason {
  min-height: 42px;
  padding: 8px 10px;
  margin: 0 0 12px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.item-visits {
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.detail-content {
  .detail-image {
    width: 100%;
    height: 300px;
    margin-bottom: 16px;
    border-radius: 8px;
  }
  
  h3 {
    font-size: 24px;
    margin-bottom: 12px;
  }
  
  p {
    color: #606266;
    margin-bottom: 16px;
    line-height: 1.6;
  }
}

@media (max-width: 1100px) {
  .scene-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .decision-panel {
    grid-template-columns: 1fr;
  }

  .decision-filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .recommend-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-tools,
  .preference-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .preference-stats {
    justify-content: flex-start;
  }

  .preference-content {
    grid-template-columns: 1fr;
  }

  .scene-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .decision-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .decision-filters {
    grid-template-columns: 1fr;
  }
}
</style>
