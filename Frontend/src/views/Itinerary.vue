<template>
  <div class="itinerary-page">
    <AppHeader />

    <section class="page-hero">
      <div>
        <span class="eyebrow">Trip Builder</span>
        <h1>旅行计划生成</h1>
        <p>根据城市、游玩时长、偏好和用户行为，生成景点与美食结合的行程草案。</p>
      </div>
      <div class="hero-actions">
        <el-button size="large" plain @click="openPlanDrawer">
          我的行程
        </el-button>
        <el-button size="large" plain @click="loadHotPlans">
          热门行程
        </el-button>
        <el-button size="large" :disabled="!plan" :loading="saving" @click="saveCurrentPlan">
          {{ isSharedPlan ? '复制到我的行程' : '保存行程' }}
        </el-button>
        <el-button size="large" :disabled="!plan" @click="openShareDialog">
          分享到圈子
        </el-button>
        <el-button type="primary" size="large" :loading="loading" @click="handleGenerate">
          生成行程
        </el-button>
      </div>
    </section>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="8">
        <el-card class="form-card" shadow="never">
          <template #header>
            <h3 class="card-title">
              <el-icon><Edit /></el-icon>
              行程条件
            </h3>
          </template>

          <el-form :model="form" label-width="86px">
            <el-form-item label="城市">
              <el-input v-model="form.city" placeholder="例如 北京、上海、杭州" clearable />
            </el-form-item>
            <el-form-item label="时长">
              <el-segmented v-model="form.duration" :options="durationOptions" />
            </el-form-item>
            <el-form-item label="节奏">
              <el-radio-group v-model="form.pace">
                <el-radio-button label="relaxed">轻松</el-radio-button>
                <el-radio-button label="normal">标准</el-radio-button>
                <el-radio-button label="compact">紧凑</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="目的">
              <el-select v-model="form.purpose" placeholder="这次更偏向什么" style="width: 100%">
                <el-option
                  v-for="item in purposeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="预算">
              <el-segmented v-model="form.budgetLevel" :options="budgetOptions" />
            </el-form-item>
            <el-form-item label="偏好">
              <el-checkbox-group v-model="form.preferences" class="preference-group">
                <el-checkbox-button v-for="item in preferenceOptions" :key="item.value" :label="item.value">
                  {{ item.label }}
                </el-checkbox-button>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="餐饮">
              <el-switch v-model="form.includeFood" active-text="插入美食" inactive-text="只排景点" />
            </el-form-item>
            <el-form-item label="约束">
              <div class="constraint-list">
                <el-checkbox v-model="form.routeRequired">优先可规划景点</el-checkbox>
                <el-checkbox v-model="form.avoidVisited">避开去过</el-checkbox>
              </div>
            </el-form-item>
          </el-form>

          <div class="form-actions">
            <el-button type="primary" :loading="loading" @click="handleGenerate">生成行程</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-card>

        <el-card v-if="plan" class="summary-card" shadow="never">
          <template #header>
            <h3 class="card-title">
              <el-icon><Compass /></el-icon>
              计划摘要
            </h3>
          </template>
          <el-alert
            v-if="isSharedPlan"
            title="这是圈子里分享的行程，复制后会保存到你的个人行程列表，之后可以继续修改和分享。"
            type="success"
            :closable="false"
            show-icon
            class="shared-plan-alert"
          />
          <el-input v-model="planTitle" maxlength="100" show-word-limit placeholder="给这份行程起个名字" />
          <p>{{ plan.summary }}</p>
          <el-alert
            v-if="plan.routeHint"
            :title="plan.routeHint"
            type="warning"
            :closable="false"
            show-icon
            class="route-hint-alert"
          />
          <div class="summary-grid">
            <div>
              <strong>{{ plan.totalDays }}</strong>
              <span>天数</span>
            </div>
            <div>
              <strong>{{ plan.spotCount }}</strong>
              <span>景点</span>
            </div>
            <div>
              <strong>{{ plan.copyCount || 0 }}</strong>
              <span>复制</span>
            </div>
            <div>
              <strong>{{ plan.favoriteCount || 0 }}</strong>
              <span>收藏</span>
            </div>
          </div>
          <el-button
            v-if="plan.id"
            class="favorite-plan-button"
            :type="plan.favorited ? 'warning' : 'default'"
            plain
            :loading="favoriteLoading === plan.id"
            @click="togglePlanFavorite(plan)"
          >
            {{ plan.favorited ? '已收藏行程' : '收藏这份行程' }}
          </el-button>
          <el-alert
            v-for="tip in plan.tips"
            :key="tip"
            :title="tip"
            type="info"
            :closable="false"
            show-icon
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card class="result-card" shadow="never" v-loading="loading">
          <template #header>
            <div class="result-header">
              <h3 class="card-title">
                <el-icon><Calendar /></el-icon>
                生成结果
              </h3>
              <div class="result-actions" v-if="plan">
                <el-button plain :loading="saving" @click="saveCurrentPlan">
                  {{ isSharedPlan ? '复制到我的行程' : '保存行程' }}
                </el-button>
                <el-button plain @click="openShareDialog">分享到圈子</el-button>
                <el-button plain type="primary" @click="goRoutePlan">去路线规划</el-button>
              </div>
            </div>
          </template>

          <el-empty v-if="!plan" description="填写条件后生成行程" />

          <div v-else class="day-list">
            <section v-for="day in plan.days" :key="day.dayNo" class="day-section">
              <div class="day-heading">
                <div class="day-heading-text">
                  <h2>{{ day.title }}</h2>
                  <span>{{ day.summary }}</span>
                  <em v-if="day.estimatedDistanceM">景点间直线约 {{ formatDistance(day.estimatedDistanceM) }}</em>
                </div>
                <div class="day-edit-actions">
                  <el-button size="small" type="primary" plain @click="openAddDialog(day, 'spot')">
                    <el-icon><Plus /></el-icon>添加景点
                  </el-button>
                  <el-button size="small" type="warning" plain @click="openAddDialog(day, 'food')">
                    <el-icon><Plus /></el-icon>添加美食
                  </el-button>
                </div>
              </div>

              <el-timeline>
                <el-timeline-item
                  v-for="(item, idx) in day.items"
                  :key="`${day.dayNo}-${idx}-${item.itemType}-${item.targetId}`"
                  :timestamp="item.timeSlot"
                  placement="top"
                >
                  <article class="plan-item">
                    <el-image :src="item.image || placeholderImage(item)" fit="cover" class="item-cover" @click="openItem(item)">
                      <template #error>
                        <div class="image-fallback">{{ item.itemType === 'food' ? '美食' : '景点' }}</div>
                      </template>
                    </el-image>
                    <div class="item-main" @click="openItem(item)">
                      <div class="item-title-row">
                        <h3>{{ item.name }}</h3>
                        <el-tag size="small" :type="item.itemType === 'food' ? 'warning' : 'primary'">
                          {{ item.itemType === 'food' ? '美食' : '景点' }}
                        </el-tag>
                      </div>
                      <p>{{ item.description || item.address || '暂无简介' }}</p>
                      <div class="item-meta">
                        <span>
                          <el-icon><Star /></el-icon>
                          {{ item.rating || '暂无评分' }}
                        </span>
                        <span v-if="item.hotness">
                          <el-icon><View /></el-icon>
                          {{ item.hotness }}
                        </span>
                        <span>{{ item.recommendReason }}</span>
                      </div>
                    </div>
                    <div class="item-edit-actions" @click.stop>
                      <el-button size="small" circle :disabled="idx === 0" @click.stop="moveItem(day, idx, -1)">
                        <el-icon><ArrowUp /></el-icon>
                      </el-button>
                      <el-button size="small" circle :disabled="idx === day.items.length - 1" @click.stop="moveItem(day, idx, 1)">
                        <el-icon><ArrowDown /></el-icon>
                      </el-button>
                      <el-button size="small" type="danger" circle @click.stop="removeItem(day, idx)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </article>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!day.items || day.items.length === 0" description="这一天还没有安排，点上方按钮添加" :image-size="80" />
            </section>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="planDrawerVisible" title="我的行程" size="520px">
      <div v-loading="planListLoading" class="saved-plan-list">
        <el-empty v-if="savedPlans.length === 0" description="还没有保存行程" />
        <article v-for="item in savedPlans" v-else :key="item.id" class="saved-plan-card">
          <div class="saved-plan-main" @click="loadSavedPlan(item.id)">
            <h3>{{ item.title || item.summary }}</h3>
            <p>{{ item.summary }}</p>
            <div class="saved-plan-meta">
              <span>{{ item.city }}</span>
              <span>{{ durationText(item.duration) }}</span>
              <span>{{ item.spotCount || 0 }} 个景点</span>
              <span>{{ item.copyCount || 0 }} 次复制</span>
              <span>{{ item.favoriteCount || 0 }} 次收藏</span>
              <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
            </div>
          </div>
          <div class="saved-plan-actions">
            <el-button
              plain
              size="small"
              :type="item.favorited ? 'warning' : 'default'"
              :loading="favoriteLoading === item.id"
              @click.stop="togglePlanFavorite(item)"
            >
              {{ item.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button type="danger" plain size="small" @click.stop="removeSavedPlan(item)">
              删除
            </el-button>
          </div>
        </article>

        <el-pagination
          v-if="savedPlanTotal > savedPlanQuery.pageSize"
          v-model:current-page="savedPlanQuery.page"
          :page-size="savedPlanQuery.pageSize"
          :total="savedPlanTotal"
          layout="total, prev, pager, next"
          @current-change="loadSavedPlans"
          class="drawer-pagination"
        />
      </div>
    </el-drawer>

    <el-drawer v-model="hotDrawerVisible" title="圈子热门行程" size="560px">
      <div v-loading="hotPlanLoading" class="hot-plan-list">
        <el-alert
          title="这里展示你已加入圈子里被复制、收藏和分享较多的行程。复制后会进入你的个人行程列表。"
          type="info"
          :closable="false"
          show-icon
          class="hot-plan-tip"
        />
        <el-empty v-if="hotPlans.length === 0" description="你加入的圈子里暂时没有热门行程" />
        <article v-for="item in hotPlans" v-else :key="item.id" class="hot-plan-card" @click="loadSharedPlanFromHot(item)">
          <div class="hot-plan-rank">#{{ hotPlans.indexOf(item) + 1 }}</div>
          <div class="hot-plan-main">
            <div class="hot-plan-title-row">
              <h3>{{ item.title || '旅行计划' }}</h3>
              <el-tag size="small" type="success">{{ item.city || '目的地' }}</el-tag>
            </div>
            <p>{{ item.summary || '打开查看完整行程。' }}</p>
            <div class="hot-plan-meta">
              <span>{{ item.ownerName || '圈子用户' }}</span>
              <span>{{ durationText(item.duration) }}</span>
              <span>{{ item.spotCount || 0 }} 个景点</span>
              <span>{{ item.copyCount || 0 }} 次复制</span>
              <span>{{ item.favoriteCount || 0 }} 次收藏</span>
            </div>
          </div>
          <div class="hot-plan-actions">
            <el-button
              size="small"
              plain
              :type="item.favorited ? 'warning' : 'default'"
              :loading="favoriteLoading === item.id"
              @click.stop="togglePlanFavorite(item)"
            >
              {{ item.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="small" type="primary" plain @click.stop="copyHotPlan(item)">
              复制
            </el-button>
          </div>
        </article>
      </div>
    </el-drawer>

    <el-dialog
      v-model="addDialogVisible"
      :title="addDialogType === 'food' ? '添加美食到行程' : '添加景点到行程'"
      width="640px"
      @open="onAddDialogOpen"
    >
      <div class="add-dialog">
        <el-input
          v-model="addKeyword"
          :placeholder="addDialogType === 'food' ? '搜索美食名称、菜系' : '搜索景点名称、城市'"
          clearable
          @keyup.enter="searchAddCandidates"
        >
          <template #append>
            <el-button :loading="addSearching" @click="searchAddCandidates">搜索</el-button>
          </template>
        </el-input>

        <div v-loading="addSearching" class="add-result-list">
          <el-empty v-if="addCandidates.length === 0 && !addSearching" :image-size="80" description="暂无结果，换个关键词试试" />
          <article
            v-for="cand in addCandidates"
            :key="`${addDialogType}-${cand.id}`"
            class="add-candidate"
            @click="confirmAddCandidate(cand)"
          >
            <div class="add-candidate-main">
              <div class="add-candidate-title">
                <strong>{{ cand.name }}</strong>
                <el-tag size="small" :type="addDialogType === 'food' ? 'warning' : 'primary'">
                  {{ addDialogType === 'food' ? '美食' : '景点' }}
                </el-tag>
              </div>
              <p>{{ cand.description || cand.address || cand.cuisineName || '暂无简介' }}</p>
              <div class="add-candidate-meta">
                <span v-if="cand.rating || cand.score">
                  <el-icon><Star /></el-icon>
                  {{ cand.rating || cand.score }}
                </span>
                <span v-if="cand.city || cand.placeGroupName">
                  {{ cand.city || cand.placeGroupName }}
                </span>
              </div>
            </div>
            <el-button size="small" type="primary" plain>添加</el-button>
          </article>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="shareDialogVisible" title="分享到圈子" width="560px">
      <el-alert
        v-if="!plan?.id"
        title="分享前需要先保存行程，系统会把保存后的行程关联到圈子日志里。"
        type="info"
        :closable="false"
        show-icon
        class="share-alert"
      />
      <el-form :model="shareForm" label-width="82px">
        <el-form-item label="选择圈子" required>
          <el-select
            v-model="shareForm.circleId"
            placeholder="选择你已加入的圈子"
            filterable
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
        <el-form-item label="标题">
          <el-input v-model="shareForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="shareForm.content"
            type="textarea"
            :rows="6"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shareDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sharing" @click="sharePlanToCircle">发布分享</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import {
  copyItineraryPlan,
  deleteItineraryPlan,
  generateItinerary,
  getHotSharedItineraryPlans,
  getItineraryPlanDetail,
  getItineraryPlans,
  getSharedItineraryPlan,
  saveItineraryPlan,
  toggleItineraryFavorite
} from '@/api/itinerary'
import { createLog } from '@/api/log'
import { getCircleList } from '@/api/circle'
import { searchFacilities } from '@/api/search'
import { getFoodList } from '@/api/food'
import { Calendar, Compass, Edit, Star, View, Plus, ArrowUp, ArrowDown, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const defaultForm = () => ({
  city: '北京',
  duration: 'one_day',
  pace: 'normal',
  purpose: 'balanced',
  budgetLevel: 'normal',
  preferences: ['university'],
  includeFood: true,
  routeRequired: false,
  avoidVisited: false
})

const form = reactive(defaultForm())
const loading = ref(false)
const saving = ref(false)
const plan = ref(null)
const planTitle = ref('')
const isSharedPlan = ref(false)
const planDrawerVisible = ref(false)
const planListLoading = ref(false)
const savedPlans = ref([])
const savedPlanTotal = ref(0)
const savedPlanQuery = reactive({
  page: 1,
  pageSize: 8
})
const hotDrawerVisible = ref(false)
const hotPlanLoading = ref(false)
const hotPlans = ref([])
const favoriteLoading = ref(null)
const shareDialogVisible = ref(false)
const sharing = ref(false)
const joinedCircles = ref([])
const shareForm = reactive({
  circleId: null,
  title: '',
  content: ''
})

const addDialogVisible = ref(false)
const addDialogType = ref('spot')
const addTargetDay = ref(null)
const addKeyword = ref('')
const addSearching = ref(false)
const addCandidates = ref([])

const durationOptions = [
  { label: '半日', value: 'half_day' },
  { label: '一日', value: 'one_day' },
  { label: '两日', value: 'two_day' },
  { label: '三日', value: 'three_day' }
]

const preferenceOptions = [
  { label: '高校', value: 'university' },
  { label: '风景', value: 'scenery' },
  { label: '文化', value: 'culture' },
  { label: '博物馆', value: 'museum' },
  { label: '美食', value: 'food' },
  { label: '摄影', value: 'photo' }
]

const purposeOptions = [
  { label: '综合决策', value: 'balanced' },
  { label: '拍照打卡', value: 'photo' },
  { label: '研学参观', value: 'study' },
  { label: '美食顺路', value: 'food' },
  { label: '轻松休闲', value: 'relax' },
  { label: '路线展示', value: 'route' }
]

const budgetOptions = [
  { label: '经济', value: 'economy' },
  { label: '标准', value: 'normal' },
  { label: '品质', value: 'premium' }
]

const handleGenerate = async () => {
  if (!form.city.trim()) {
    ElMessage.warning('请先填写城市')
    return
  }
  loading.value = true
  try {
    const res = await generateItinerary({
      city: form.city.trim(),
      duration: form.duration,
      pace: form.pace,
      purpose: form.purpose,
      budgetLevel: form.budgetLevel,
      preferences: form.preferences,
      includeFood: form.includeFood,
      routeRequired: form.routeRequired,
      avoidVisited: form.avoidVisited
    })
    plan.value = res.data
    isSharedPlan.value = false
    planTitle.value = `${form.city.trim()}${durationText(form.duration)}旅行计划`
  } catch (error) {
    console.error('生成行程失败:', error)
    plan.value = null
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, defaultForm())
  plan.value = null
  isSharedPlan.value = false
  planTitle.value = ''
}

const ensureLogin = () => {
  if (localStorage.getItem('token')) {
    return true
  }
  ElMessage.warning('请先登录后再保存或查看行程')
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
  return false
}

const saveCurrentPlan = async () => {
  if (!plan.value) {
    ElMessage.warning('请先生成行程')
    return null
  }
  if (!ensureLogin()) {
    return null
  }
  saving.value = true
  try {
    const res = await saveItineraryPlan({
      title: planTitle.value || plan.value.title,
      city: plan.value.city,
      duration: plan.value.duration,
      pace: plan.value.pace,
      totalDays: plan.value.totalDays,
      spotCount: plan.value.spotCount,
      summary: plan.value.summary,
      preferences: plan.value.preferences || [],
      days: plan.value.days || []
    })
    plan.value = res.data
    isSharedPlan.value = false
    planTitle.value = res.data?.title || planTitle.value
    ElMessage.success('行程已保存')
    if (planDrawerVisible.value) {
      await loadSavedPlans()
    }
    return plan.value
  } catch (error) {
    console.error('保存行程失败:', error)
    return null
  } finally {
    saving.value = false
  }
}

const openPlanDrawer = async () => {
  if (!ensureLogin()) {
    return
  }
  planDrawerVisible.value = true
  await loadSavedPlans()
}

const loadSavedPlans = async () => {
  planListLoading.value = true
  try {
    const res = await getItineraryPlans({
      page: savedPlanQuery.page,
      pageSize: savedPlanQuery.pageSize
    })
    savedPlans.value = res.data?.list || []
    savedPlanTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载保存行程失败:', error)
    savedPlans.value = []
    savedPlanTotal.value = 0
  } finally {
    planListLoading.value = false
  }
}

const loadSavedPlan = async (id) => {
  try {
    const res = await getItineraryPlanDetail(id)
    plan.value = res.data
    isSharedPlan.value = false
    planTitle.value = res.data?.title || ''
    if (res.data?.city) form.city = res.data.city
    if (res.data?.duration) form.duration = res.data.duration
    if (res.data?.pace) form.pace = res.data.pace
    if (Array.isArray(res.data?.preferences)) form.preferences = [...res.data.preferences]
    planDrawerVisible.value = false
    ElMessage.success('已载入保存的行程')
  } catch (error) {
    console.error('加载保存行程详情失败:', error)
  }
}

const loadSharedPlan = async (id) => {
  try {
    const res = await getSharedItineraryPlan(id)
    plan.value = res.data
    isSharedPlan.value = true
    planTitle.value = res.data?.title || ''
    if (res.data?.city) form.city = res.data.city
    if (res.data?.duration) form.duration = res.data.duration
    if (res.data?.pace) form.pace = res.data.pace
    if (Array.isArray(res.data?.preferences)) form.preferences = [...res.data.preferences]
    ElMessage.success('已打开圈子分享的行程')
  } catch (error) {
    console.error('加载分享行程失败:', error)
  }
}

const loadHotPlans = async () => {
  if (!ensureLogin()) {
    return
  }
  hotDrawerVisible.value = true
  hotPlanLoading.value = true
  try {
    const res = await getHotSharedItineraryPlans({ limit: 10 })
    hotPlans.value = res.data || []
  } catch (error) {
    console.error('加载热门行程失败:', error)
    hotPlans.value = []
  } finally {
    hotPlanLoading.value = false
  }
}

const loadSharedPlanFromHot = async (item) => {
  await loadSharedPlan(item.id)
  hotDrawerVisible.value = false
}

const togglePlanFavorite = async (item) => {
  if (!item?.id) {
    ElMessage.warning('请先保存行程后再收藏')
    return
  }
  if (!ensureLogin()) {
    return
  }
  favoriteLoading.value = item.id
  try {
    const res = await toggleItineraryFavorite(item.id)
    const state = res.data || {}
    applyFavoriteState(item.id, state.favorited, state.favoriteCount)
    ElMessage.success(state.favorited ? '已收藏行程' : '已取消收藏')
  } catch (error) {
    console.error('收藏行程失败:', error)
  } finally {
    favoriteLoading.value = null
  }
}

const applyFavoriteState = (planId, favorited, favoriteCount) => {
  const update = target => {
    if (!target || target.id !== planId) return
    target.favorited = Boolean(favorited)
    target.favoriteCount = favoriteCount ?? target.favoriteCount ?? 0
  }
  update(plan.value)
  savedPlans.value.forEach(update)
  hotPlans.value.forEach(update)
}

const copyHotPlan = async (item) => {
  if (!item?.id || !ensureLogin()) {
    return
  }
  try {
    const res = await copyItineraryPlan(item.id)
    item.copyCount = (item.copyCount || 0) + 1
    ElMessage.success('已复制到我的行程')
    hotDrawerVisible.value = false
    router.push({ path: '/itinerary', query: { planId: res.data?.id } })
    if (res.data?.id) {
      await loadSavedPlan(res.data.id)
    }
  } catch (error) {
    console.error('复制热门行程失败:', error)
  }
}

const removeSavedPlan = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除「${item.title || '这份行程'}」吗？`, '删除行程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteItineraryPlan(item.id)
    ElMessage.success('已删除')
    await loadSavedPlans()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除保存行程失败:', error)
    }
  }
}

const openShareDialog = async () => {
  if (!plan.value) {
    ElMessage.warning('请先生成或打开一份行程')
    return
  }
  if (!ensureLogin()) {
    return
  }
  await loadJoinedCircles()
  if (!joinedCircles.value.length) {
    ElMessage.warning('你还没有加入圈子，先加入圈子后再分享行程')
    router.push('/circle')
    return
  }
  shareForm.circleId = joinedCircles.value[0]?.id || null
  shareForm.title = planTitle.value || plan.value.title || `${plan.value.city || ''}旅行计划`
  shareForm.content = buildShareContent()
  shareDialogVisible.value = true
}

const loadJoinedCircles = async () => {
  try {
    const res = await getCircleList('')
    joinedCircles.value = res.data?.joinedCircles || []
  } catch (error) {
    console.error('加载已加入圈子失败:', error)
    joinedCircles.value = []
  }
}

const sharePlanToCircle = async () => {
  if (!shareForm.circleId) {
    ElMessage.warning('请选择要分享的圈子')
    return
  }
  if (!shareForm.content.trim()) {
    ElMessage.warning('请输入分享内容')
    return
  }
  sharing.value = true
  try {
    if (!plan.value?.id || isSharedPlan.value) {
      const saved = await saveCurrentPlan()
      if (!saved?.id) {
        return
      }
    }
    await createLog({
      title: shareForm.title.trim() || planTitle.value || '旅行计划分享',
      content: shareForm.content.trim(),
      circleId: shareForm.circleId,
      itineraryPlanId: plan.value.id,
      tags: ['行程分享', plan.value.city].filter(Boolean)
    })
    ElMessage.success('行程已分享到圈子')
    shareDialogVisible.value = false
    router.push(`/circle/${shareForm.circleId}`)
  } catch (error) {
    console.error('分享行程失败:', error)
  } finally {
    sharing.value = false
  }
}

const buildShareContent = () => {
  const title = planTitle.value || plan.value?.title || `${plan.value?.city || ''}旅行计划`
  const lines = [
    `分享一份「${title}」。`,
    plan.value?.summary || '',
    '',
    ...((plan.value?.days || []).map(day => {
      const names = (day.items || []).map(item => item.name).filter(Boolean).join(' -> ')
      return `${day.title || `第 ${day.dayNo} 天`}：${names}`
    }))
  ]
  return lines.filter(line => line !== null && line !== undefined).join('\n').trim()
}

const reindexDay = (day) => {
  if (!day || !Array.isArray(day.items)) return
  day.items.forEach((item, idx) => {
    item.orderNo = idx + 1
  })
  redistributeTimeSlots(day)
  refreshPlanCounters()
}

const redistributeTimeSlots = (day) => {
  const items = day.items || []
  if (items.length === 0) return
  const baseHour = 9
  const stepHour = day.items.length > 0 ? Math.max(1, Math.min(3, Math.floor(10 / items.length))) : 2
  items.forEach((item, idx) => {
    if (item.timeSlot && item._manualTime) return
    const start = baseHour + idx * stepHour
    const end = Math.min(22, start + stepHour)
    item.timeSlot = `${String(start).padStart(2, '0')}:00 - ${String(end).padStart(2, '0')}:00`
  })
}

const refreshPlanCounters = () => {
  if (!plan.value) return
  const days = plan.value.days || []
  plan.value.totalDays = days.length
  plan.value.spotCount = days.reduce(
    (sum, d) => sum + (d.items || []).filter(it => it.itemType === 'spot').length,
    0
  )
}

const moveItem = (day, idx, delta) => {
  const items = day.items || []
  const newIdx = idx + delta
  if (newIdx < 0 || newIdx >= items.length) return
  const [moved] = items.splice(idx, 1)
  items.splice(newIdx, 0, moved)
  reindexDay(day)
}

const removeItem = async (day, idx) => {
  const item = day.items[idx]
  try {
    await ElMessageBox.confirm(`确定从行程中移除「${item.name}」吗？`, '移除条目', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
    day.items.splice(idx, 1)
    reindexDay(day)
    ElMessage.success('已移除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除行程条目失败:', error)
    }
  }
}

const openAddDialog = (day, type) => {
  addTargetDay.value = day
  addDialogType.value = type
  addKeyword.value = ''
  addCandidates.value = []
  addDialogVisible.value = true
}

const onAddDialogOpen = () => {
  searchAddCandidates()
}

const searchAddCandidates = async () => {
  addSearching.value = true
  try {
    if (addDialogType.value === 'food') {
      const res = await getFoodList({
        keyword: addKeyword.value.trim() || undefined,
        sort: 'score'
      })
      const list = Array.isArray(res.data) ? res.data : (res.data?.list || [])
      addCandidates.value = list.slice(0, 30)
    } else {
      const res = await searchFacilities({
        keyword: addKeyword.value.trim() || undefined,
        spotOnly: true,
        page: 1,
        pageSize: 30
      })
      addCandidates.value = res.data?.list || []
    }
  } catch (error) {
    console.error('搜索候选失败:', error)
    addCandidates.value = []
  } finally {
    addSearching.value = false
  }
}

const confirmAddCandidate = (cand) => {
  const day = addTargetDay.value
  if (!day) return
  const isFood = addDialogType.value === 'food'
  if (!day.items) day.items = []
  const exists = day.items.some(it => it.itemType === addDialogType.value && String(it.targetId) === String(cand.id))
  if (exists) {
    ElMessage.warning('这一天已经包含该条目')
    return
  }
  day.items.push({
    itemType: isFood ? 'food' : 'spot',
    targetId: cand.id,
    spotId: isFood ? null : (cand.spotId || cand.id),
    name: cand.name,
    image: cand.image || cand.coverUrl || cand.cover || cand.imageUrl || '',
    address: cand.address || cand.location || cand.city || cand.placeGroupName || '',
    description: cand.description || cand.cuisineName || '',
    rating: cand.rating || cand.score || null,
    hotness: cand.hotness || null,
    recommendReason: '手动添加',
    timeSlot: '',
    orderNo: day.items.length + 1
  })
  reindexDay(day)
  ElMessage.success(`已添加「${cand.name}」到${day.title || '当天'}`)
  addDialogVisible.value = false
}

const openItem = (item) => {
  if (item.itemType === 'food') {
    router.push(`/food/${item.targetId}`)
  } else {
    router.push(`/spot/${item.targetId}`)
  }
}

const goRoutePlan = () => {
  const spotItems = routeableSpotItems()
  if (spotItems.length < 2) {
    ElMessage.warning('行程里至少需要 2 个带坐标的景点，才能生成景点之间路线')
    return
  }
  router.push({
    path: '/route-plan',
    query: {
      routeType: 'between',
      routeSpotIds: spotItems.map(item => item.spotId || item.targetId).join(','),
      sourcePlanId: plan.value?.id || '',
      routeTitle: planTitle.value || plan.value?.title || '行程路线'
    }
  })
}

const routeableSpotItems = () => {
  const seen = new Set()
  return (plan.value?.days || [])
    .flatMap(day => day.items || [])
    .filter(item => item.itemType === 'spot' && (item.spotId || item.targetId))
    .filter(item => {
      const id = String(item.spotId || item.targetId)
      if (seen.has(id)) {
        return false
      }
      seen.add(id)
      return true
    })
}

const placeholderImage = (item) => item.itemType === 'food' ? '/food-placeholder.jpg' : '/spot-placeholder.jpg'

const durationText = (duration) => {
  const map = {
    half_day: '半日',
    one_day: '一日',
    two_day: '两日',
    three_day: '三日'
  }
  return map[duration] || '一日'
}

const formatDistance = (meters) => {
  const value = Number(meters || 0)
  if (value >= 1000) {
    return `${(value / 1000).toFixed(value % 1000 === 0 ? 0 : 1)} 公里`
  }
  return `${value} 米`
}

const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  if (route.query.city) {
    form.city = String(route.query.city)
  }
  if (route.query.planId && ensureLogin()) {
    await loadSavedPlan(route.query.planId)
  } else if (route.query.sharedPlanId && ensureLogin()) {
    await loadSharedPlan(route.query.sharedPlanId)
  }
})
</script>

<style lang="scss" scoped>
.itinerary-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f6f8fb;
  min-height: 100vh;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 36px;
  margin-bottom: 20px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #6d28d9);

  h1 {
    margin: 8px 0;
    font-size: 38px;
  }

  p {
    margin: 0;
    color: rgba(255, 255, 255, 0.82);
  }
}

.hero-actions,
.result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.eyebrow {
  font-size: 13px;
  letter-spacing: 0;
  opacity: 0.8;
}

.card-title,
.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-card,
.summary-card,
.result-card {
  border-radius: 8px;
}

.summary-card {
  margin-top: 20px;

  p {
    margin: 14px 0 0;
  }

  .el-alert {
    margin-top: 10px;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin: 16px 0;

  div {
    padding: 14px;
    border-radius: 8px;
    background: #f5f7fa;
  }

  strong {
    display: block;
    font-size: 24px;
    color: #2563eb;
  }

  span {
    color: #909399;
  }
}

.favorite-plan-button {
  width: 100%;
  margin-bottom: 4px;
}

.preference-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.constraint-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.route-hint-alert {
  line-height: 1.5;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.result-header {
  justify-content: space-between;
}

.saved-plan-list {
  min-height: 260px;
}

.saved-plan-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}

.saved-plan-main {
  min-width: 0;
  cursor: pointer;

  h3 {
    margin: 0 0 8px;
    font-size: 17px;
    color: #303133;
  }

  p {
    margin: 0 0 10px;
    color: #606266;
    line-height: 1.6;
  }
}

.saved-plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #909399;
  font-size: 13px;
}

.saved-plan-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
}

.hot-plan-list {
  min-height: 280px;
}

.hot-plan-tip {
  margin-bottom: 14px;
}

.hot-plan-card {
  display: grid;
  grid-template-columns: 44px 1fr auto;
  gap: 14px;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.hot-plan-rank {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-weight: 700;
}

.hot-plan-main {
  min-width: 0;

  p {
    margin: 8px 0 10px;
    color: #606266;
    line-height: 1.6;
  }
}

.hot-plan-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    font-size: 17px;
    color: #303133;
  }
}

.hot-plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #909399;
  font-size: 13px;
}

.hot-plan-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-pagination {
  justify-content: center;
  margin-top: 18px;
}

.share-alert {
  margin-bottom: 16px;
}

.shared-plan-alert {
  margin-bottom: 12px;
}

.day-section + .day-section {
  margin-top: 28px;
}

.day-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;

  h2 {
    margin: 0 0 6px;
    font-size: 22px;
  }

  span {
    color: #909399;
  }

  em {
    display: inline-block;
    margin-top: 6px;
    color: #2563eb;
    font-style: normal;
    font-size: 13px;
  }
}

.day-heading-text {
  min-width: 0;
}

.day-edit-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.plan-item {
  position: relative;
  display: grid;
  grid-template-columns: 160px 1fr auto;
  gap: 16px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px rgba(31, 41, 55, 0.1);
  }
}

.item-cover {
  width: 160px;
  height: 104px;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
}

.item-main {
  cursor: pointer;
}

.item-edit-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-self: center;
}

.add-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.add-result-list {
  max-height: 460px;
  overflow-y: auto;
  min-height: 200px;
}

.add-candidate {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 6px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.15s ease;
}

.add-candidate:hover {
  background: #f5f7fa;
}

.add-candidate-main {
  min-width: 0;
  flex: 1;

  p {
    margin: 4px 0;
    color: #606266;
    font-size: 13px;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.add-candidate-title {
  display: flex;
  align-items: center;
  gap: 8px;

  strong {
    color: #303133;
    font-size: 15px;
  }
}

.add-candidate-meta {
  display: flex;
  gap: 12px;
  color: #909399;
  font-size: 12px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.image-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.item-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;

  h3 {
    margin: 0;
    font-size: 18px;
  }
}

.item-main {
  min-width: 0;

  p {
    margin: 8px 0;
    color: #606266;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #909399;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

@media (max-width: 768px) {
  .page-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions,
  .result-actions {
    flex-wrap: wrap;
  }

  .plan-item {
    grid-template-columns: 1fr;
  }

  .item-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
