<template>
  <div class="home-page">
    <AppHeader />

    <section class="hero-section" :style="heroStyle">
      <div class="hero-content">
        <span class="hero-eyebrow">景点查询 · 推荐 · 路线 · 行程 · 社区</span>
        <h1 class="hero-title">TourWise 个性化旅游系统</h1>
        <p class="hero-subtitle">
          面向景区、校区和城市目的地，完成景点检索、智能推荐、路线规划和旅行分享的完整闭环。
        </p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="router.push('/search')">
            查找景点
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" class="hero-secondary" @click="router.push('/recommend')">
            智能推荐
          </el-button>
          <el-button size="large" class="hero-secondary" @click="router.push('/itinerary')">
            生成行程
          </el-button>
          <el-button size="large" class="hero-community" @click="goToCircle">
            <el-icon><ChatDotRound /></el-icon>
            进入圈子
          </el-button>
        </div>

        <div class="hero-search">
          <el-input
            v-model="searchKeyword"
            size="large"
            placeholder="输入景点、城市或简称，例如 北邮、外滩、故宫"
            clearable
            @keyup.enter="handleQuickSearch()"
          />
          <el-button type="primary" size="large" @click="handleQuickSearch()">搜索</el-button>
        </div>

        <div class="quick-tags">
          <button v-for="keyword in quickKeywords" :key="keyword" @click="handleQuickSearch(keyword)">
            {{ keyword }}
          </button>
        </div>
      </div>

      <div class="hero-panel">
        <div class="panel-header">
          <span>当前数据概览</span>
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="panel-grid">
          <div>
            <strong>{{ hotList.length || 10 }}</strong>
            <span>热门目的地</span>
          </div>
          <div>
            <strong>{{ averageScore }}</strong>
            <span>平均评分</span>
          </div>
          <div>
            <strong>{{ totalVisits }}</strong>
            <span>累计热度</span>
          </div>
        </div>
        <div class="featured-mini" v-if="featuredSpot">
          <img :src="featuredSpot.image || '/placeholder.jpg'" alt="" />
          <div>
            <span>今日推荐</span>
            <strong>{{ featuredSpot.name }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="section-heading">
      <div>
        <span class="section-kicker">Core Modules</span>
        <h2>核心功能</h2>
      </div>
      <p>首页只做入口和概览，具体检索、推荐、路线、圈子和内容互动进入对应模块完成。</p>
    </section>

    <section class="features-section">
      <div
        v-for="feature in features"
        :key="feature.title"
        class="feature-card"
        :class="feature.tone"
        @click="feature.onClick"
      >
        <div class="feature-top">
          <span class="feature-icon">
            <el-icon :size="28"><component :is="feature.icon" /></el-icon>
          </span>
          <el-icon class="feature-arrow"><ArrowRight /></el-icon>
        </div>
        <h3 class="feature-title">{{ feature.title }}</h3>
        <p class="feature-desc">{{ feature.desc }}</p>
        <span class="feature-foot">{{ feature.foot }}</span>
      </div>
    </section>

    <section class="section-heading hot-heading">
      <div>
        <span class="section-kicker">Hot List</span>
        <h2>
          <el-icon><StarFilled /></el-icon>
          热门推荐 TOP 10
        </h2>
      </div>
      <el-button text type="primary" @click="router.push('/recommend')">
        查看更多推荐
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
    </section>

    <section class="hot-section">
      <div
        v-for="(item, index) in hotList"
        :key="item.id"
        class="hot-card"
        @click="handleHotItemClick(item)"
      >
        <div class="hot-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
        <div class="image-admin-shell">
          <el-image
            class="hot-image"
            :src="item.image || '/placeholder.jpg'"
            fit="cover"
          />
          <AdminImageUpload
            class="image-upload-overlay"
            target-type="spot"
            :target-id="item.spotId || item.id"
            @success="url => handleHotImageUploaded(item, url)"
          />
        </div>
        <div class="hot-info">
          <div class="hot-tags">
            <el-tag size="small" v-if="item.city">{{ item.city }}</el-tag>
            <el-tag size="small" type="success" v-if="item.categoryName">{{ item.categoryName }}</el-tag>
          </div>
          <h4 class="hot-name">{{ item.name }}</h4>
          <p class="hot-desc">{{ item.description || '暂无简介' }}</p>
          <div class="hot-meta">
            <span class="hot-score">
              <el-icon><Star /></el-icon>
              {{ item.score || item.rating || 4.5 }}
            </span>
            <span class="hot-visits">
              <el-icon><View /></el-icon>
              {{ formatNumber(item.visits || item.hotness || 0) }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <section class="section-heading itinerary-heading">
      <div>
        <span class="section-kicker">Shared Trips</span>
        <h2>
          <el-icon><Calendar /></el-icon>
          圈子热门行程
        </h2>
      </div>
      <el-button text type="primary" @click="router.push('/itinerary')">
        去生成行程
        <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
    </section>

    <section class="itinerary-section">
      <div v-if="!isLoggedIn" class="itinerary-empty-panel">
        <div>
          <h3>登录后查看圈子里的热门行程</h3>
          <p>热门行程来自你加入的圈子，复制后可以直接保存到“我的行程”，再去路线规划页生成高德路线。</p>
        </div>
        <el-button type="primary" @click="router.push({ name: 'Login', query: { redirect: '/' } })">
          去登录
        </el-button>
      </div>

      <div v-else-if="hotItineraryLoading" class="itinerary-grid">
        <el-skeleton v-for="index in 3" :key="index" animated class="itinerary-skeleton">
          <template #template>
            <el-skeleton-item variant="h3" style="width: 55%" />
            <el-skeleton-item variant="text" style="width: 92%; margin-top: 18px" />
            <el-skeleton-item variant="text" style="width: 76%" />
            <el-skeleton-item variant="button" style="width: 120px; margin-top: 20px" />
          </template>
        </el-skeleton>
      </div>

      <div v-else-if="hotItineraries.length === 0" class="itinerary-empty-panel">
        <div>
          <h3>暂时没有可展示的圈子行程</h3>
          <p>你可以先加入圈子，或者把自己的行程分享到圈子，后续这里会按复制、收藏和分享热度排序。</p>
        </div>
        <el-button plain type="primary" @click="goToCircle">进入圈子</el-button>
      </div>

      <div v-else class="itinerary-grid">
        <article
          v-for="(item, index) in hotItineraries"
          :key="item.id"
          class="itinerary-card"
          @click="openSharedItinerary(item)"
        >
          <div class="itinerary-card-head">
            <span class="itinerary-rank">#{{ index + 1 }}</span>
            <el-tag size="small" type="success">{{ item.city || '目的地' }}</el-tag>
          </div>
          <h3>{{ item.title || '旅行计划' }}</h3>
          <p>{{ item.summary || '打开查看完整行程安排。' }}</p>
          <div class="itinerary-meta">
            <span>{{ item.ownerName || '圈子用户' }}</span>
            <span>{{ durationText(item.duration) }}</span>
            <span>{{ item.spotCount || 0 }} 个景点</span>
          </div>
          <div class="itinerary-stats">
            <span>
              <el-icon><DocumentCopy /></el-icon>
              {{ item.copyCount || 0 }} 次复制
            </span>
            <span>
              <el-icon><Star /></el-icon>
              {{ item.favoriteCount || 0 }} 次收藏
            </span>
          </div>
          <div class="itinerary-actions">
            <el-button
              size="small"
              plain
              :type="item.favorited ? 'warning' : 'default'"
              :loading="itineraryActionLoading === `favorite-${item.id}`"
              @click.stop="toggleHotItineraryFavorite(item)"
            >
              {{ item.favorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              :loading="itineraryActionLoading === `copy-${item.id}`"
              @click.stop="copyHotItinerary(item)"
            >
              复制行程
            </el-button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getHotTop10 } from '@/api/recommend'
import { copyItineraryPlan, getHotSharedItineraryPlans, toggleItineraryFavorite } from '@/api/itinerary'
import { ArrowRight, Calendar, DocumentCopy, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const isLoggedIn = computed(() => !!userStore.token)
const searchKeyword = ref('')
const quickKeywords = ['北邮', '北航', '上海外滩', '故宫']
const hotList = ref([])
const hotItineraries = ref([])
const hotItineraryLoading = ref(false)
const itineraryActionLoading = ref('')

const featuredSpot = computed(() => hotList.value.find(item => item.image) || hotList.value[0])
const heroStyle = computed(() => {
  const image = featuredSpot.value?.image
  return image ? { '--hero-image': `url("${image}")` } : {}
})

const averageScore = computed(() => {
  const scores = hotList.value
    .map(item => Number(item.score || item.rating))
    .filter(score => Number.isFinite(score) && score > 0)
  if (!scores.length) return '4.8'
  return (scores.reduce((sum, score) => sum + score, 0) / scores.length).toFixed(1)
})

const totalVisits = computed(() => {
  const total = hotList.value.reduce((sum, item) => sum + Number(item.visits || item.hotness || 0), 0)
  return total ? formatNumber(total) : '10万+'
})

const features = [
  {
    title: '智能推荐',
    desc: '综合热度、评分和偏好，推荐适合参观的景点',
    foot: '场景化推荐',
    icon: 'Star',
    tone: 'tone-blue',
    onClick: () => router.push('/recommend')
  },
  {
    title: '路线规划',
    desc: '为景区、校区和场馆生成可解释的游览路线',
    foot: 'Dijkstra 最短路径',
    icon: 'MapLocation',
    tone: 'tone-green',
    onClick: () => router.push('/route-plan')
  },
  {
    title: '景点查询',
    desc: '按省份、城市、标签和简称筛选目标景点',
    foot: '多条件叠加',
    icon: 'Search',
    tone: 'tone-amber',
    onClick: () => router.push('/search')
  },
  {
    title: '美食推荐',
    desc: '发现景点周边、商圈和校园附近的特色美食',
    foot: '按景点关联',
    icon: 'Food',
    tone: 'tone-red',
    onClick: () => router.push('/food')
  },
  {
    title: '旅行计划',
    desc: '按城市、时长和偏好生成景点与美食结合的行程草案',
    foot: '推荐 + 美食 + 路线',
    icon: 'Calendar',
    tone: 'tone-purple',
    onClick: () => router.push('/itinerary')
  },
  {
    title: '旅行圈子',
    desc: '围绕景点、校区和兴趣主题发布日志、评论和互动',
    foot: '项目社区亮点',
    icon: 'ChatDotRound',
    tone: 'tone-slate',
    onClick: () => goToCircle()
  }
]

const handleQuickSearch = (keyword = searchKeyword.value) => {
  const value = (keyword || '').trim()
  router.push({
    path: '/search',
    query: value ? { keyword: value } : {}
  })
}

const goToCircle = () => {
  if (!isLoggedIn.value) {
    ElMessage.info('登录后可以进入圈子、发布日志和参与评论')
    router.push({ name: 'Login', query: { redirect: '/circle' } })
    return
  }
  router.push('/circle')
}

const handleHotItemClick = (item) => {
  console.log('点击热门推荐:', item)
  if (!item.id) {
    ElMessage.warning('缺少景点 ID，无法跳转')
    return
  }
  // 根据类型跳转到景点或美食详情页
  // 如果有 type 字段且为 food，则跳转到美食详情页
  if (item.type === 'food') {
    router.push({ path: `/food/${item.id}` })
  } else {
    // 默认跳转到景点详情页
    router.push({ path: `/spot/${item.id}` })
  }
}

const handleHotImageUploaded = (item, imageUrl) => {
  if (imageUrl) {
    item.image = imageUrl
  }
}

const loadHotItineraries = async () => {
  if (!isLoggedIn.value) {
    hotItineraries.value = []
    return
  }
  hotItineraryLoading.value = true
  try {
    const res = await getHotSharedItineraryPlans({ limit: 6 })
    hotItineraries.value = res.data || []
  } catch (error) {
    console.error('获取热门行程失败:', error)
    hotItineraries.value = []
  } finally {
    hotItineraryLoading.value = false
  }
}

const openSharedItinerary = (item) => {
  if (!item?.id) return
  router.push({ path: '/itinerary', query: { sharedPlanId: item.id } })
}

const copyHotItinerary = async (item) => {
  if (!item?.id) return
  itineraryActionLoading.value = `copy-${item.id}`
  try {
    const res = await copyItineraryPlan(item.id)
    item.copyCount = (item.copyCount || 0) + 1
    ElMessage.success('已复制到我的行程')
    router.push({ path: '/itinerary', query: { planId: res.data?.id } })
  } catch (error) {
    console.error('复制热门行程失败:', error)
  } finally {
    itineraryActionLoading.value = ''
  }
}

const toggleHotItineraryFavorite = async (item) => {
  if (!item?.id) return
  itineraryActionLoading.value = `favorite-${item.id}`
  try {
    const res = await toggleItineraryFavorite(item.id)
    item.favorited = Boolean(res.data?.favorited)
    item.favoriteCount = res.data?.favoriteCount ?? item.favoriteCount ?? 0
    ElMessage.success(item.favorited ? '已收藏行程' : '已取消收藏')
  } catch (error) {
    console.error('收藏热门行程失败:', error)
  } finally {
    itineraryActionLoading.value = ''
  }
}

const durationText = (duration) => {
  const map = {
    half_day: '半日',
    one_day: '一日',
    two_day: '两日',
    three_day: '三日'
  }
  return map[duration] || '一日'
}

const formatNumber = (value) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return '0'
  if (number >= 10000) {
    return `${(number / 10000).toFixed(number >= 100000 ? 0 : 1)}万`
  }
  return String(number)
}

onMounted(async () => {
  try {
    const res = await getHotTop10()
    hotList.value = res.data || []
  } catch (error) {
    console.error('获取热门推荐失败:', error)
    // 使用模拟数据
    hotList.value = Array.from({ length: 8 }, (_, i) => ({
      id: i + 1,
      name: `热门景点${i + 1}`,
      score: (4 + Math.random()).toFixed(1),
      visits: Math.floor(Math.random() * 10000),
      type: 'spot'
    }))
  }
  await loadHotItineraries()
})
</script>

<style lang="scss" scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding-bottom: 48px;
  background:
    linear-gradient(180deg, #f6f8fb 0, #ffffff 320px);
}

.hero-section {
  min-height: 430px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: end;
  gap: 32px;
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  padding: 58px;
  color: white;
  margin: 0 0 46px;
  background:
    linear-gradient(90deg, rgba(12, 24, 39, 0.86), rgba(25, 59, 85, 0.58), rgba(88, 83, 138, 0.45)),
    var(--hero-image, linear-gradient(135deg, #1f6f8b 0%, #6b5fb5 100%));
  background-size: cover;
  background-position: center;
  box-shadow: 0 22px 60px rgba(22, 47, 79, 0.22);
}

.hero-section::after {
  content: "";
  position: absolute;
  inset: auto 0 0 0;
  height: 42%;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.34));
  pointer-events: none;
}

.hero-content,
.hero-panel {
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  display: inline-flex;
  margin-bottom: 18px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  font-size: 13px;
  letter-spacing: 0;
}

.hero-title {
  max-width: 760px;
  font-size: 52px;
  line-height: 1.12;
  font-weight: 800;
  margin-bottom: 16px;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 680px;
  font-size: 18px;
  line-height: 1.8;
  opacity: 0.94;
  margin-bottom: 28px;
}

.hero-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.hero-secondary {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.62);
  background: rgba(255, 255, 255, 0.12);
}

.hero-community {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.68);
  background: rgba(24, 24, 27, 0.28);

  .el-icon {
    margin-right: 6px;
  }
}

.hero-search {
  max-width: 650px;
  display: grid;
  grid-template-columns: 1fr 104px;
  gap: 10px;
  padding: 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(12px);
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  
  button {
    border: 1px solid rgba(255, 255, 255, 0.28);
    background: rgba(255, 255, 255, 0.12);
    color: white;
    border-radius: 999px;
    padding: 6px 12px;
    cursor: pointer;
  }
}

.hero-panel {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(16px);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  opacity: 0.92;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 34px 0;
  
  div {
    min-width: 0;
  }
  
  strong {
    display: block;
    font-size: 24px;
    line-height: 1.2;
  }
  
  span {
    display: block;
    margin-top: 8px;
    font-size: 12px;
    opacity: 0.78;
  }
}

.featured-mini {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  
  img {
    width: 66px;
    height: 52px;
    border-radius: 8px;
    object-fit: cover;
  }
  
  span,
  strong {
    display: block;
  }
  
  span {
    font-size: 12px;
    opacity: 0.74;
    margin-bottom: 5px;
  }
  
  strong {
    font-size: 15px;
    line-height: 1.3;
  }
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 24px;
  margin: 0 0 18px;
  padding: 0 2px;
  
  h2 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 4px 0 0;
    font-size: 26px;
    color: #1f2937;
  }
  
  p {
    max-width: 560px;
    margin: 0;
    color: #6b7280;
    line-height: 1.7;
    text-align: right;
  }
}

.section-kicker {
  font-size: 12px;
  color: #409eff;
  font-weight: 700;
  text-transform: uppercase;
}

.features-section {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 48px;
}

.feature-card {
  min-height: 190px;
  padding: 22px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.22s, box-shadow 0.22s, border-color 0.22s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 14px 30px rgba(31, 41, 55, 0.10);
    border-color: #cbd5e1;
  }
}

.feature-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feature-icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.feature-arrow {
  color: #94a3b8;
}

.feature-title {
  font-size: 19px;
  margin: 22px 0 8px;
  color: #111827;
}

.feature-desc {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
  min-height: 48px;
  margin: 0 0 12px;
}

.feature-foot {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.tone-blue .feature-icon {
  color: #2563eb;
  background: #eff6ff;
}

.tone-green .feature-icon {
  color: #059669;
  background: #ecfdf5;
}

.tone-amber .feature-icon {
  color: #d97706;
  background: #fffbeb;
}

.tone-red .feature-icon {
  color: #dc2626;
  background: #fef2f2;
}

.tone-purple .feature-icon {
  color: #7c3aed;
  background: #f5f3ff;
}

.tone-slate .feature-icon {
  color: #475569;
  background: #f1f5f9;
}

.hot-heading {
  margin-top: 4px;
}

.hot-section {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.hot-card {
  position: relative;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.22s, box-shadow 0.22s, border-color 0.22s;
  
  &:hover {
    transform: translateY(-4px);
    border-color: #cbd5e1;
    box-shadow: 0 14px 30px rgba(31, 41, 55, 0.12);
    
    .hot-image {
      transform: scale(1.05);
    }
  }
}

.hot-rank {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  z-index: 1;
  
  &.rank-1 { background: #f56c6c; }
  &.rank-2 { background: #e6a23c; }
  &.rank-3 { background: #f5a623; }
}

.hot-image {
  width: 100%;
  height: 172px;
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

.hot-info {
  padding: 16px;
}

.hot-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.hot-name {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-desc {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  min-height: 42px;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hot-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
}

.hot-score, .hot-visits {
  display: flex;
  align-items: center;
  gap: 4px;
}

.itinerary-heading {
  margin-top: 46px;
}

.itinerary-section {
  margin-bottom: 52px;
}

.itinerary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.itinerary-card,
.itinerary-empty-panel,
.itinerary-skeleton {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.itinerary-card {
  min-height: 230px;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.22s, box-shadow 0.22s, border-color 0.22s;

  &:hover {
    transform: translateY(-4px);
    border-color: #cbd5e1;
    box-shadow: 0 14px 30px rgba(31, 41, 55, 0.10);
  }

  h3 {
    margin: 18px 0 10px;
    color: #111827;
    font-size: 19px;
    line-height: 1.4;
  }

  p {
    margin: 0 0 14px;
    min-height: 46px;
    color: #6b7280;
    line-height: 1.65;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.itinerary-card-head,
.itinerary-actions,
.itinerary-stats,
.itinerary-meta {
  display: flex;
  align-items: center;
}

.itinerary-card-head {
  justify-content: space-between;
}

.itinerary-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 32px;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-weight: 800;
}

.itinerary-meta,
.itinerary-stats {
  flex-wrap: wrap;
  gap: 10px;
  color: #909399;
  font-size: 13px;
}

.itinerary-stats {
  margin-top: 12px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.itinerary-actions {
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

.itinerary-empty-panel {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;

  h3 {
    margin: 0 0 8px;
    color: #111827;
    font-size: 20px;
  }

  p {
    max-width: 720px;
    margin: 0;
    color: #6b7280;
    line-height: 1.7;
  }
}

.itinerary-skeleton {
  padding: 20px;
}

@media (max-width: 1180px) {
  .hero-section {
    grid-template-columns: 1fr;
    padding: 42px;
  }

  .hero-panel {
    max-width: 560px;
  }

  .features-section,
  .hot-section,
  .itinerary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-page {
    padding: 0 12px 32px;
  }

  .hero-section {
    min-height: auto;
    padding: 32px 22px;
    border-radius: 16px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-subtitle {
    font-size: 15px;
  }

  .hero-search {
    grid-template-columns: 1fr;
  }

  .section-heading {
    align-items: start;
    flex-direction: column;
    
    p {
      text-align: left;
    }
  }

  .features-section,
  .hot-section,
  .itinerary-grid {
    grid-template-columns: 1fr;
  }

  .itinerary-empty-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
