<template>
  <div class="admin-page">
    <AppHeader />

    <main class="admin-main">
      <section class="admin-hero">
        <div>
          <p class="eyebrow">TourWise Admin</p>
          <h1>管理后台</h1>
          <p class="hero-copy">
            集中查看系统数据、内部路网质量和需要处理的问题。路线规划是否准确，核心取决于这里的数据维护质量。
          </p>
        </div>
        <div class="hero-actions">
          <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">
            刷新
          </el-button>
          <el-button :icon="Location" @click="router.push('/admin/spots')">
            景点管理
          </el-button>
          <el-button :icon="Warning" @click="router.push('/admin/reports')">
            内容审核
          </el-button>
          <el-button :icon="Document" @click="router.push('/admin/logs')">
            日志治理
          </el-button>
          <el-button :icon="ChatDotRound" @click="router.push('/admin/circles')">
            圈子管理
          </el-button>
          <el-button type="primary" :icon="MapLocation" @click="router.push('/admin/route-graph')">
            路网标定
          </el-button>
        </div>
      </section>

      <section class="metric-grid">
        <article v-for="metric in metrics" :key="metric.key" class="metric-card">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-value">{{ metric.value ?? 0 }}</div>
          <div class="metric-desc">{{ metric.description }}</div>
        </article>
      </section>

      <section class="quality-section">
        <header class="section-header">
          <div>
            <h2>景点数据完整度</h2>
            <p>集中检查封面、经纬度、分类、标签、代表 POI、介绍、地址、可见 POI 和内部路网状态。</p>
          </div>
          <el-button :icon="Location" @click="router.push('/admin/spots')">维护景点</el-button>
        </header>

        <div class="completeness-overview">
          <article class="completeness-card">
            <div class="completeness-label">平均完整度</div>
            <div class="completeness-value">{{ completenessStats.average }}%</div>
            <el-progress :percentage="completenessStats.average" :show-text="false" />
          </article>
          <article class="completeness-card">
            <div class="completeness-label">可上线</div>
            <div class="completeness-value">{{ completenessStats.ok }}</div>
            <div class="completeness-desc">暂无明显配置问题</div>
          </article>
          <article class="completeness-card">
            <div class="completeness-label">待优化</div>
            <div class="completeness-value warning">{{ completenessStats.warning }}</div>
            <div class="completeness-desc">影响推荐、展示或搜索体验</div>
          </article>
          <article class="completeness-card">
            <div class="completeness-label">需处理</div>
            <div class="completeness-value danger">{{ completenessStats.error }}</div>
            <div class="completeness-desc">可能导致路线、详情或定位不可用</div>
          </article>
        </div>

        <el-table
          v-loading="loading"
          :data="dataQuality"
          border
          class="quality-table"
          empty-text="暂无景点数据"
        >
          <el-table-column label="景点" min-width="190">
            <template #default="{ row }">
              <div class="scope-name">{{ row.name }}</div>
              <div class="scope-meta">Spot {{ row.spotId }} · {{ row.spotType || 'other' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="地区" min-width="130">
            <template #default="{ row }">
              {{ [row.province, row.city].filter(Boolean).join(' / ') || '未填写' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag :type="levelType(row.level)">
                {{ levelText(row.level) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="完整度" width="150">
            <template #default="{ row }">
              <div class="score-cell">
                <el-progress
                  :percentage="row.completenessScore || 0"
                  :status="progressStatus(row)"
                  :stroke-width="8"
                />
                <div class="score-meta">
                  {{ row.completedItemCount || 0 }}/{{ row.totalItemCount || 0 }} 项
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="POI" width="90">
            <template #default="{ row }">
              {{ row.visiblePoiCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="标签" width="90">
            <template #default="{ row }">
              {{ row.tagCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="内部路网" width="110">
            <template #default="{ row }">
              {{ graphStatusText(row.routeGraphStatus) }}
            </template>
          </el-table-column>
          <el-table-column label="配置问题" min-width="320">
            <template #default="{ row }">
              <div v-if="row.issues?.length" class="issue-list">
                <el-tag
                  v-for="issue in row.issues"
                  :key="issue"
                  size="small"
                  :type="row.level === 'error' ? 'danger' : 'warning'"
                >
                  {{ issue }}
                </el-tag>
              </div>
              <span v-else class="no-issue">暂无明显问题</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="116" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="goSpotManage(row)">
                处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="quality-section">
        <header class="section-header">
          <div>
            <h2>路网数据质量检查</h2>
            <p>用于检查平面图、POI 坐标、路线边、孤立节点和不可达点位。</p>
          </div>
          <div class="issue-summary">
            <el-tag :type="errorCount > 0 ? 'danger' : 'success'">严重 {{ errorCount }}</el-tag>
            <el-tag :type="warningCount > 0 ? 'warning' : 'success'">警告 {{ warningCount }}</el-tag>
            <el-tag type="info">问题 {{ issueCount }}</el-tag>
          </div>
        </header>

        <el-table
          v-loading="loading"
          :data="routeQuality"
          border
          class="quality-table"
          empty-text="暂无内部路网景点"
        >
          <el-table-column label="景点" min-width="180">
            <template #default="{ row }">
              <div class="scope-name">{{ row.name }}</div>
              <div class="scope-meta">ID {{ row.placeGroupId }} · {{ graphStatusText(row.routeGraphStatus) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag :type="levelType(row.level)">
                {{ levelText(row.level) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="底图" width="92">
            <template #default="{ row }">
              <el-tag :type="row.hasMap ? 'success' : 'danger'">
                {{ row.hasMap ? '已配置' : '缺失' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="visiblePoiCount" label="POI" width="76" />
          <el-table-column prop="routeNodeCount" label="路口" width="76" />
          <el-table-column prop="edgeCount" label="路线边" width="86" />
          <el-table-column label="坐标问题" width="116">
            <template #default="{ row }">
              <span>{{ row.missingMapPointCount }} / {{ row.missingGeoCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="连通问题" width="130">
            <template #default="{ row }">
              <span>{{ row.isolatedPoiCount }} 孤立，{{ row.unreachablePoiCount }} 不可达</span>
            </template>
          </el-table-column>
          <el-table-column label="问题说明" min-width="260">
            <template #default="{ row }">
              <div v-if="row.issues?.length" class="issue-list">
                <el-tag
                  v-for="issue in row.issues"
                  :key="issue"
                  size="small"
                  :type="row.level === 'error' ? 'danger' : 'warning'"
                >
                  {{ issue }}
                </el-tag>
              </div>
              <span v-else class="no-issue">暂无明显问题</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="128" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="goRouteGraph(row)">
                标定
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Document, Location, MapLocation, Refresh, Warning } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getAdminDashboard } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const dashboard = ref({
  metrics: [],
  routeQuality: [],
  issueCount: 0,
  errorCount: 0,
  warningCount: 0
})

const metrics = computed(() => dashboard.value.metrics || [])
const dataQuality = computed(() => dashboard.value.dataQuality || [])
const routeQuality = computed(() => dashboard.value.routeQuality || [])
const issueCount = computed(() => dashboard.value.issueCount || 0)
const errorCount = computed(() => dashboard.value.errorCount || 0)
const warningCount = computed(() => dashboard.value.warningCount || 0)
const completenessStats = computed(() => {
  const rows = dataQuality.value
  if (!rows.length) {
    return {
      average: 0,
      ok: 0,
      warning: 0,
      error: 0
    }
  }
  const total = rows.reduce((sum, row) => sum + Number(row.completenessScore || 0), 0)
  return {
    average: Math.round(total / rows.length),
    ok: rows.filter(row => row.level === 'ok').length,
    warning: rows.filter(row => row.level === 'warning').length,
    error: rows.filter(row => row.level === 'error').length
  }
})

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入管理后台')
    router.push('/')
    return
  }
  await loadDashboard()
})

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await getAdminDashboard()
    dashboard.value = res.data || dashboard.value
  } finally {
    loading.value = false
  }
}

const goRouteGraph = (row) => {
  router.push({ path: '/admin/route-graph', query: { placeGroupId: row.placeGroupId } })
}

const goSpotManage = (row) => {
  const query = { keyword: row.name }
  const qualityIssue = inferQualityIssue(row)
  if (qualityIssue) {
    query.qualityIssue = qualityIssue
  }
  router.push({ path: '/admin/spots', query })
}

const inferQualityIssue = (row) => {
  if (row.missingRepresentativePoi) return 'missingRepresentativePoi'
  if (row.representativePoiDisabled) return 'representativePoiDisabled'
  if (row.missingGeo) return 'missingGeo'
  if (row.missingVisiblePoi) return 'missingVisiblePoi'
  if (row.campusRouteMissing) return 'campusRouteMissing'
  if (row.missingCover) return 'missingCover'
  if (row.missingCategory) return 'missingCategory'
  return ''
}

const levelType = (level) => {
  if (level === 'error') return 'danger'
  if (level === 'warning') return 'warning'
  return 'success'
}

const levelText = (level) => {
  if (level === 'error') return '需处理'
  if (level === 'warning') return '待优化'
  return '正常'
}

const progressStatus = (row) => {
  if (row.level === 'error') return 'exception'
  if (row.level === 'warning') return 'warning'
  return 'success'
}

const graphStatusText = (status) => {
  if (status === 'verified') return '已校准'
  if (status === 'draft') return '草稿'
  return '未配置'
}
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

.admin-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(30, 45, 70, 0.06);

  h1 {
    margin: 6px 0 10px;
    font-size: 30px;
    line-height: 1.25;
    color: #1f2a3d;
  }
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  color: #4f7cff;
  font-weight: 700;
}

.hero-copy {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.metric-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
}

.metric-label {
  color: #6b7280;
  font-size: 14px;
}

.metric-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 800;
  color: #1f2a3d;
}

.metric-desc {
  margin-top: 6px;
  color: #9aa3b2;
  font-size: 13px;
}

.quality-section {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
  overflow: hidden;
}

.completeness-overview {
  display: grid;
  grid-template-columns: 1.35fr repeat(3, minmax(0, 1fr));
  gap: 14px;
  padding: 18px 24px;
  border-bottom: 1px solid #e6ebf4;
  background: #f8fbff;
}

.completeness-card {
  min-height: 92px;
  padding: 14px;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
  background: #fff;
}

.completeness-label {
  color: #6b7280;
  font-size: 13px;
}

.completeness-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  color: #1f2a3d;

  &.warning {
    color: #b7791f;
  }

  &.danger {
    color: #c2410c;
  }
}

.completeness-desc {
  margin-top: 6px;
  color: #98a2b3;
  font-size: 12px;
  line-height: 1.45;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 22px 24px;
  border-bottom: 1px solid #e6ebf4;

  h2 {
    margin: 0;
    font-size: 22px;
    color: #1f2a3d;
  }

  p {
    margin: 8px 0 0;
    color: #7b8494;
  }
}

.issue-summary {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.quality-table {
  width: 100%;
}

.score-cell {
  display: grid;
  gap: 4px;
}

.score-meta {
  color: #98a2b3;
  font-size: 12px;
  text-align: right;
}

.scope-name {
  font-weight: 700;
  color: #263243;
}

.scope-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #98a2b3;
}

.issue-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.no-issue {
  color: #9aa3b2;
}

@media (max-width: 960px) {
  .admin-main {
    padding: 18px 14px 32px;
  }

  .admin-hero,
  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .completeness-overview {
    grid-template-columns: 1fr;
  }
}
</style>
