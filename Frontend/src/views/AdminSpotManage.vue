<template>
  <div class="admin-spot-page">
    <AppHeader />

    <main class="admin-main">
      <section class="page-head">
        <div>
          <p class="eyebrow">Data Management</p>
          <h1>景点与 POI 管理</h1>
          <p>维护大景点信息、内部 POI 坐标和启用状态。这里的质量会直接影响查询、推荐和路线规划。</p>
        </div>
        <div class="head-actions">
          <el-button @click="router.push('/admin')">返回后台</el-button>
          <el-button type="primary" @click="router.push('/admin/route-graph')">路网标定</el-button>
        </div>
      </section>

      <section class="panel">
        <div class="toolbar">
          <el-input
            v-model="spotQuery.keyword"
            placeholder="搜索景点、城市或简称"
            clearable
            class="toolbar-input"
            @keyup.enter="loadSpots"
            @clear="loadSpots"
          />
          <el-select v-model="spotQuery.status" placeholder="状态" clearable class="toolbar-select" @change="loadSpots">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-select v-model="spotQuery.routeGraphStatus" placeholder="内部路网" clearable class="toolbar-select" @change="loadSpots">
            <el-option label="未配置" value="none" />
            <el-option label="草稿" value="draft" />
            <el-option label="已校准" value="verified" />
          </el-select>
          <el-select v-model="spotQuery.qualityIssue" placeholder="问题类型" clearable class="toolbar-select issue-select" @change="loadSpots">
            <el-option
              v-for="item in qualityIssueOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button type="primary" :loading="spotLoading" @click="loadSpots">查询</el-button>
          <el-button type="success" @click="openSpotDialog()">新增景点</el-button>
        </div>

        <div class="quick-filters">
          <span class="quick-filter-label">快捷筛选</span>
          <el-button
            v-for="item in qualityIssueOptions"
            :key="item.value"
            size="small"
            :type="spotQuery.qualityIssue === item.value ? 'primary' : 'default'"
            plain
            @click="applyQualityIssue(item.value)"
          >
            {{ item.label }}
          </el-button>
          <el-button
            v-if="spotQuery.qualityIssue || spotQuery.routeGraphStatus"
            size="small"
            text
            type="primary"
            @click="clearQualityFilters"
          >
            清除质量筛选
          </el-button>
        </div>

        <el-table v-loading="spotLoading" :data="spots" border>
          <el-table-column label="封面" width="132">
            <template #default="{ row }">
              <div class="image-cell">
                <el-image
                  v-if="row.coverImage"
                  class="admin-thumb"
                  :src="row.coverImage"
                  fit="cover"
                  :preview-src-list="[row.coverImage]"
                  preview-teleported
                />
                <div v-else class="image-placeholder">暂无封面</div>
                <AdminImageUpload
                  target-type="spot"
                  :target-id="row.id"
                  label="上传"
                  @success="handleSpotImageUploaded(row, $event)"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="景点" min-width="220">
            <template #default="{ row }">
              <div class="name-cell">{{ row.name }}</div>
              <div class="muted">ID {{ row.id }} · {{ row.shortName || '无简称' }}</div>
              <div class="spot-warning-tags">
                <el-tag
                  v-for="issue in spotInlineIssues(row)"
                  :key="issue"
                  size="small"
                  type="warning"
                >
                  {{ issue }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="地区" min-width="150">
            <template #default="{ row }">
              {{ [row.province, row.city, row.district].filter(Boolean).join(' / ') || '未填写' }}
            </template>
          </el-table-column>
          <el-table-column prop="spotType" label="类型" width="120" />
          <el-table-column prop="tagCount" label="标签" width="76" />
          <el-table-column label="内部路网" width="110">
            <template #default="{ row }">
              <el-tag :type="routeStatusType(row.routeGraphStatus)">
                {{ routeStatusText(row.routeGraphStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="poiCount" label="POI" width="76" />
          <el-table-column prop="hotness" label="热度" width="86" />
          <el-table-column label="状态" width="86">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="openSpotDialog(row)">编辑</el-button>
              <el-button type="primary" text @click="openPois(row)">POI</el-button>
              <el-button text @click="goRouteGraph(row)">路网</el-button>
              <el-button
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="toggleSpotStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="spotTotal"
            :page-sizes="[10, 20, 50]"
            v-model:current-page="spotQuery.page"
            v-model:page-size="spotQuery.pageSize"
            @current-change="loadSpots"
            @size-change="loadSpots"
          />
        </div>
      </section>
    </main>

    <el-dialog v-model="spotDialogVisible" :title="spotForm.id ? '编辑景点' : '新增景点'" width="760px">
      <el-form :model="spotForm" label-width="110px" class="edit-form">
        <el-form-item label="景点名称">
          <el-input v-model="spotForm.name" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="spotForm.shortName" />
        </el-form-item>
        <el-form-item label="景点分类">
          <el-select v-model="spotForm.categoryId" clearable filterable placeholder="不选则按类型自动匹配">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="`${category.name} (${category.code})`"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="景点类型">
          <el-select v-model="spotForm.spotType" filterable>
            <el-option label="高校" value="university" />
            <el-option label="校区" value="campus" />
            <el-option label="景区" value="scenic" />
            <el-option label="博物馆" value="museum" />
            <el-option label="公园" value="park" />
            <el-option label="地标" value="landmark" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="景点标签">
          <div class="tag-editor">
            <el-select
              v-model="spotTagNames"
              multiple
              filterable
              allow-create
              default-first-option
              collapse-tags
              collapse-tags-tooltip
              :max-collapse-tags="4"
              placeholder="选择或输入标签，例如 高校、摄影、博物馆"
            >
              <el-option
                v-for="tag in availableTags"
                :key="tag.id || tag.name"
                :label="tag.name"
                :value="tag.name"
              />
            </el-select>
            <el-alert
              type="info"
              :closable="false"
              title="标签会参与景点查询、个性化推荐和用户偏好匹配。输入新标签后保存会自动创建。"
            />
          </div>
        </el-form-item>
        <el-form-item label="地区">
          <div class="inline-fields">
            <el-input v-model="spotForm.province" placeholder="省份" />
            <el-input v-model="spotForm.city" placeholder="城市" />
            <el-input v-model="spotForm.district" placeholder="区县" />
          </div>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="spotForm.address" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="image-editor-row">
            <el-image
              v-if="spotForm.coverImage"
              class="form-preview"
              :src="spotForm.coverImage"
              fit="cover"
              :preview-src-list="[spotForm.coverImage]"
              preview-teleported
            />
            <div v-else class="form-preview empty">暂无封面</div>
            <div class="image-editor-main">
              <el-input v-model="spotForm.coverImage" placeholder="可直接填写 OSS/图片 URL" />
              <AdminImageUpload
                v-if="spotForm.id"
                target-type="spot"
                :target-id="spotForm.id"
                label="上传封面"
                @success="handleSpotFormImageUploaded"
              />
              <el-alert
                v-else
                type="info"
                :closable="false"
                title="新增景点需要先保存，生成 ID 后才能上传封面。"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="中心坐标">
          <div class="inline-fields">
            <el-input-number v-model="spotForm.longitude" :precision="6" :step="0.000001" controls-position="right" placeholder="经度" />
            <el-input-number v-model="spotForm.latitude" :precision="6" :step="0.000001" controls-position="right" placeholder="纬度" />
          </div>
        </el-form-item>
        <el-form-item label="定位半径">
          <el-input-number v-model="spotForm.locationRadiusM" :min="20" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="内部路网">
          <el-select v-model="spotForm.routeGraphStatus">
            <el-option label="未配置" value="none" />
            <el-option label="草稿" value="draft" />
            <el-option label="已校准" value="verified" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分/热度">
          <div class="inline-fields">
            <el-input-number v-model="spotForm.rating" :precision="1" :step="0.1" :min="0" :max="5" controls-position="right" />
            <el-input-number v-model="spotForm.hotness" :min="0" :step="100" controls-position="right" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="spotForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="介绍">
          <el-input v-model="spotForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingSpot" @click="saveSpot">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="poiDrawerVisible" :title="poiDrawerTitle" size="78%">
      <section class="drawer-panel">
        <div class="toolbar">
          <el-input
            v-model="poiQuery.keyword"
            placeholder="搜索 POI 名称、区域、地址"
            clearable
            class="toolbar-input"
            @keyup.enter="loadPois"
            @clear="loadPois"
          />
          <el-select v-model="poiQuery.status" placeholder="状态" clearable class="toolbar-select" @change="loadPois">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-select v-model="poiQuery.qualityIssue" placeholder="POI 问题" clearable class="toolbar-select issue-select" @change="loadPois">
            <el-option
              v-for="item in poiQualityIssueOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button :loading="poiLoading" @click="loadPois">查询</el-button>
          <el-button type="primary" @click="openPoiDialog()">新增 POI</el-button>
        </div>

        <el-table v-loading="poiLoading" :data="pois" border>
          <el-table-column label="图片" width="132">
            <template #default="{ row }">
              <div class="image-cell">
                <el-image
                  v-if="row.imageUrl"
                  class="admin-thumb"
                  :src="row.imageUrl"
                  fit="cover"
                  :preview-src-list="[row.imageUrl]"
                  preview-teleported
                />
                <div v-else class="image-placeholder">暂无图片</div>
                <AdminImageUpload
                  target-type="poi"
                  :target-id="row.id"
                  label="上传"
                  @success="handlePoiImageUploaded(row, $event)"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="POI" min-width="180">
            <template #default="{ row }">
              <div class="name-cell">{{ row.name }}</div>
              <div class="muted">ID {{ row.id }} · {{ row.categoryName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="区域" width="130">
            <template #default="{ row }">
              {{ row.areaName || row.areaCode || '未填写' }}
            </template>
          </el-table-column>
          <el-table-column label="平面图坐标" width="130">
            <template #default="{ row }">
              {{ row.mapX ?? '-' }}, {{ row.mapY ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="经纬度" min-width="170">
            <template #default="{ row }">
              {{ row.longitude ?? '-' }}, {{ row.latitude ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="数据问题" min-width="190">
            <template #default="{ row }">
              <div v-if="poiIssueTags(row).length" class="issue-list">
                <el-tag
                  v-for="issue in poiIssueTags(row)"
                  :key="issue"
                  size="small"
                  type="warning"
                >
                  {{ issue }}
                </el-tag>
              </div>
              <span v-else class="no-issue">正常</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="86">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="openPoiDialog(row)">编辑</el-button>
              <el-button
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="togglePoiStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="poiTotal"
            :page-sizes="[20, 50, 100]"
            v-model:current-page="poiQuery.page"
            v-model:page-size="poiQuery.pageSize"
            @current-change="loadPois"
            @size-change="loadPois"
          />
        </div>
      </section>
    </el-drawer>

    <el-dialog v-model="poiDialogVisible" :title="poiForm.id ? '编辑 POI' : '新增 POI'" width="760px">
      <el-form :model="poiForm" label-width="110px" class="edit-form">
        <el-form-item label="POI 名称">
          <el-input v-model="poiForm.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="poiForm.categoryId" filterable>
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="`${category.name} (${category.code})`"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="场景">
          <el-radio-group v-model="poiForm.scene">
            <el-radio label="campus">校园</el-radio>
            <el-radio label="city">城市</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="区域">
          <div class="inline-fields">
            <el-input v-model="poiForm.areaCode" placeholder="区域编码，例如 teaching" />
            <el-input v-model="poiForm.areaName" placeholder="区域名称，例如 教学区" />
          </div>
        </el-form-item>
        <el-form-item label="地区">
          <div class="inline-fields">
            <el-input v-model="poiForm.province" placeholder="省份" />
            <el-input v-model="poiForm.city" placeholder="城市" />
          </div>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="poiForm.address" />
        </el-form-item>
        <el-form-item label="展示位置">
          <el-input v-model="poiForm.locationText" />
        </el-form-item>
        <el-form-item label="展示图片">
          <div class="image-editor-row">
            <el-image
              v-if="poiForm.imageUrl"
              class="form-preview"
              :src="poiForm.imageUrl"
              fit="cover"
              :preview-src-list="[poiForm.imageUrl]"
              preview-teleported
            />
            <div v-else class="form-preview empty">暂无图片</div>
            <div class="image-editor-main">
              <el-input v-model="poiForm.imageUrl" placeholder="可直接填写 OSS/图片 URL" />
              <AdminImageUpload
                v-if="poiForm.id"
                target-type="poi"
                :target-id="poiForm.id"
                label="上传图片"
                @success="handlePoiFormImageUploaded"
              />
              <el-alert
                v-else
                type="info"
                :closable="false"
                title="新增 POI 需要先保存，生成 ID 后才能上传图片。"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="经纬度">
          <div class="inline-fields">
            <el-input-number v-model="poiForm.longitude" :precision="6" :step="0.000001" controls-position="right" />
            <el-input-number v-model="poiForm.latitude" :precision="6" :step="0.000001" controls-position="right" />
          </div>
        </el-form-item>
        <el-form-item label="平面图坐标">
          <div class="inline-fields">
            <el-input-number v-model="poiForm.mapX" :min="0" controls-position="right" />
            <el-input-number v-model="poiForm.mapY" :min="0" controls-position="right" />
          </div>
        </el-form-item>
        <el-form-item label="评分/热度">
          <div class="inline-fields">
            <el-input-number v-model="poiForm.rating" :precision="1" :step="0.1" :min="0" :max="5" controls-position="right" />
            <el-input-number v-model="poiForm.hotness" :min="0" :step="10" controls-position="right" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="poiForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="介绍">
          <el-input v-model="poiForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="poiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPoi" @click="savePoi">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import {
  createAdminSpot,
  createAdminPoi,
  getAdminPoiCategories,
  getAdminSpotTags,
  getAdminSpotPois,
  getAdminSpots,
  getAdminTags,
  updateAdminPoi,
  updateAdminPoiStatus,
  updateAdminSpot,
  updateAdminSpotTags,
  updateAdminSpotStatus
} from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const spotLoading = ref(false)
const spots = ref([])
const spotTotal = ref(0)
const categories = ref([])
const availableTags = ref([])
const spotTagNames = ref([])
const selectedSpot = ref(null)

const qualityIssueOptions = [
  { label: '缺少封面', value: 'missingCover' },
  { label: '缺少经纬度', value: 'missingGeo' },
  { label: '缺少简称', value: 'missingShortName' },
  { label: '缺少介绍', value: 'missingDescription' },
  { label: '缺少地址', value: 'missingAddress' },
  { label: '缺少标签', value: 'missingTags' },
  { label: '缺少分类', value: 'missingCategory' },
  { label: '缺少代表 POI', value: 'missingRepresentativePoi' },
  { label: '代表 POI 停用', value: 'representativePoiDisabled' },
  { label: '没有可见 POI', value: 'missingVisiblePoi' },
  { label: '校区未配置路网', value: 'campusRouteMissing' }
]

const poiQualityIssueOptions = [
  { label: '缺少平面图坐标', value: 'missingMapPoint' },
  { label: '缺少经纬度', value: 'missingGeo' },
  { label: '未接入路网', value: 'notConnected' },
  { label: '缺少图片', value: 'missingImage' }
]

const spotQuery = reactive({
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : '',
  status: 1,
  routeGraphStatus: '',
  qualityIssue: typeof route.query.qualityIssue === 'string' ? route.query.qualityIssue : '',
  page: 1,
  pageSize: 10
})

const spotDialogVisible = ref(false)
const savingSpot = ref(false)
const spotForm = reactive(emptySpotForm())

const poiDrawerVisible = ref(false)
const poiLoading = ref(false)
const pois = ref([])
const poiTotal = ref(0)
const poiQuery = reactive({
  keyword: '',
  status: 1,
  qualityIssue: '',
  page: 1,
  pageSize: 20
})

const poiDialogVisible = ref(false)
const savingPoi = ref(false)
const poiForm = reactive(emptyPoiForm())

const poiDrawerTitle = computed(() => selectedSpot.value ? `${selectedSpot.value.name} POI 管理` : 'POI 管理')

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入景点管理')
    router.push('/')
    return
  }
  await Promise.all([loadCategories(), loadTags(), loadSpots()])
})

async function loadCategories() {
  const res = await getAdminPoiCategories()
  categories.value = res.data || []
}

async function loadTags() {
  const res = await getAdminTags({ tagType: 'poi' })
  availableTags.value = res.data || []
}

async function loadSpots() {
  spotLoading.value = true
  try {
    const res = await getAdminSpots({
      keyword: spotQuery.keyword || undefined,
      status: spotQuery.status,
      routeGraphStatus: spotQuery.routeGraphStatus || undefined,
      qualityIssue: spotQuery.qualityIssue || undefined,
      page: spotQuery.page,
      pageSize: spotQuery.pageSize
    })
    spots.value = res.data?.list || []
    spotTotal.value = res.data?.total || 0
  } finally {
    spotLoading.value = false
  }
}

async function loadSpotTags(spotId) {
  if (!spotId) {
    spotTagNames.value = []
    return
  }
  const res = await getAdminSpotTags(spotId)
  spotTagNames.value = (res.data || []).map(tag => tag.name).filter(Boolean)
}

function applyQualityIssue(value) {
  spotQuery.qualityIssue = spotQuery.qualityIssue === value ? '' : value
  spotQuery.page = 1
  loadSpots()
}

function clearQualityFilters() {
  spotQuery.qualityIssue = ''
  spotQuery.routeGraphStatus = ''
  spotQuery.page = 1
  loadSpots()
}

function spotInlineIssues(row) {
  const issues = []
  if (!row.coverImage) issues.push('缺封面')
  if (row.longitude == null || row.latitude == null) issues.push('缺坐标')
  if (!row.shortName) issues.push('缺简称')
  if (!row.description) issues.push('缺介绍')
  if (!row.address) issues.push('缺地址')
  if (!Number(row.tagCount || 0)) issues.push('缺标签')
  if (!row.categoryId) issues.push('缺分类')
  if (!row.representativePoiId) issues.push('缺代表 POI')
  if (!Number(row.poiCount || 0)) issues.push('无 POI')
  if (['campus', 'university'].includes(row.spotType) && row.routeGraphStatus === 'none') {
    issues.push('缺路网')
  }
  return issues.slice(0, 4)
}

async function openSpotDialog(row) {
  if (row) {
    Object.assign(spotForm, {
      id: row.id,
      categoryId: row.categoryId,
      name: row.name,
      shortName: row.shortName,
      spotType: row.spotType || 'other',
      province: row.province,
      city: row.city,
      district: row.district,
      address: row.address,
      description: row.description,
      coverImage: row.coverImage,
      longitude: decimalNumber(row.longitude),
      latitude: decimalNumber(row.latitude),
      locationRadiusM: row.locationRadiusM || 500,
      rating: decimalNumber(row.rating) ?? 5,
      hotness: row.hotness || 0,
      status: row.status ?? 1,
      routeGraphStatus: row.routeGraphStatus || 'none'
    })
    await loadSpotTags(row.id)
  } else {
    Object.assign(spotForm, emptySpotForm(), {
      categoryId: categories.value.find(item => item.code === 'scenic')?.id || categories.value[0]?.id || null
    })
    spotTagNames.value = []
  }
  spotDialogVisible.value = true
}

async function saveSpot() {
  savingSpot.value = true
  try {
    if (spotForm.id) {
      await updateAdminSpot(spotForm.id, payloadWithoutId(spotForm))
      await updateAdminSpotTags(spotForm.id, spotTagNames.value)
      ElMessage.success('景点信息已保存')
    } else {
      const res = await createAdminSpot(payloadWithoutId(spotForm))
      const createdId = res.data?.id
      if (createdId) {
        await updateAdminSpotTags(createdId, spotTagNames.value)
      }
      ElMessage.success('景点已新增')
    }
    spotDialogVisible.value = false
    await loadTags()
    await loadSpots()
  } finally {
    savingSpot.value = false
  }
}

async function toggleSpotStatus(row) {
  await updateAdminSpotStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '景点已停用' : '景点已启用')
  await loadSpots()
}

function handleSpotImageUploaded(row, imageUrl) {
  row.coverImage = imageUrl || row.coverImage
  loadSpots()
}

function handleSpotFormImageUploaded(imageUrl) {
  spotForm.coverImage = imageUrl || spotForm.coverImage
  loadSpots()
}

async function openPois(row) {
  selectedSpot.value = row
  poiQuery.page = 1
  poiQuery.qualityIssue = ''
  poiDrawerVisible.value = true
  await loadPois()
}

async function loadPois() {
  if (!selectedSpot.value) return
  poiLoading.value = true
  try {
    const res = await getAdminSpotPois(selectedSpot.value.id, {
      keyword: poiQuery.keyword || undefined,
      status: poiQuery.status,
      qualityIssue: poiQuery.qualityIssue || undefined,
      page: poiQuery.page,
      pageSize: poiQuery.pageSize
    })
    pois.value = res.data?.list || []
    poiTotal.value = res.data?.total || 0
  } finally {
    poiLoading.value = false
  }
}

function openPoiDialog(row) {
  if (row) {
    Object.assign(poiForm, {
      id: row.id,
      categoryId: row.categoryId,
      name: row.name,
      scene: row.scene || 'campus',
      areaCode: row.areaCode,
      areaName: row.areaName,
      province: row.province,
      city: row.city,
      address: row.address,
      locationText: row.locationText,
      description: row.description,
      imageUrl: row.imageUrl,
      longitude: decimalNumber(row.longitude),
      latitude: decimalNumber(row.latitude),
      mapX: row.mapX,
      mapY: row.mapY,
      rating: decimalNumber(row.rating) ?? 5,
      hotness: row.hotness || 0,
      status: row.status ?? 1
    })
  } else {
    Object.assign(poiForm, emptyPoiForm(), {
      categoryId: categories.value[0]?.id || null,
      province: selectedSpot.value?.province,
      city: selectedSpot.value?.city
    })
  }
  poiDialogVisible.value = true
}

async function savePoi() {
  if (!selectedSpot.value) return
  savingPoi.value = true
  try {
    if (poiForm.id) {
      await updateAdminPoi(poiForm.id, payloadWithoutId(poiForm))
      ElMessage.success('POI 已保存')
    } else {
      await createAdminPoi(selectedSpot.value.id, payloadWithoutId(poiForm))
      ElMessage.success('POI 已新增')
    }
    poiDialogVisible.value = false
    await loadPois()
    await loadSpots()
  } finally {
    savingPoi.value = false
  }
}

async function togglePoiStatus(row) {
  await updateAdminPoiStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? 'POI 已停用' : 'POI 已启用')
  await loadPois()
  await loadSpots()
}

function handlePoiImageUploaded(row, imageUrl) {
  row.imageUrl = imageUrl || row.imageUrl
  loadPois()
  loadSpots()
}

function handlePoiFormImageUploaded(imageUrl) {
  poiForm.imageUrl = imageUrl || poiForm.imageUrl
  loadPois()
  loadSpots()
}

function goRouteGraph(row) {
  router.push({ path: '/admin/route-graph', query: { placeGroupId: row.placeGroupId } })
}

function emptySpotForm() {
  return {
    id: null,
    categoryId: null,
    name: '',
    shortName: '',
    spotType: 'other',
    province: '',
    city: '',
    district: '',
    address: '',
    description: '',
    coverImage: '',
    longitude: null,
    latitude: null,
    locationRadiusM: 500,
    rating: 5,
    hotness: 0,
    status: 1,
    routeGraphStatus: 'none'
  }
}

function emptyPoiForm() {
  return {
    id: null,
    categoryId: null,
    name: '',
    scene: 'campus',
    areaCode: '',
    areaName: '',
    province: '',
    city: '',
    address: '',
    locationText: '',
    description: '',
    imageUrl: '',
    longitude: null,
    latitude: null,
    mapX: null,
    mapY: null,
    rating: 5,
    hotness: 0,
    status: 1
  }
}

function payloadWithoutId(form) {
  const result = { ...form }
  delete result.id
  return result
}

function decimalNumber(value) {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

function routeStatusText(status) {
  if (status === 'verified') return '已校准'
  if (status === 'draft') return '草稿'
  return '未配置'
}

function routeStatusType(status) {
  if (status === 'verified') return 'success'
  if (status === 'draft') return 'warning'
  return 'info'
}

function poiIssueTags(row) {
  const issues = []
  if (row.mapX === null || row.mapX === undefined || row.mapY === null || row.mapY === undefined) {
    issues.push('缺平面图坐标')
  }
  if (row.longitude === null || row.longitude === undefined || row.latitude === null || row.latitude === undefined) {
    issues.push('缺经纬度')
  }
  if (Number(row.routeEdgeCount || 0) === 0) {
    issues.push('未接入路网')
  }
  if (!row.imageUrl) {
    issues.push('缺图片')
  }
  return issues
}
</script>

<style lang="scss" scoped>
.admin-spot-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

.page-head,
.panel {
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding: 26px;

  h1 {
    margin: 6px 0 10px;
    color: #1f2a3d;
    font-size: 28px;
  }

  p {
    margin: 0;
    color: #6b7280;
    line-height: 1.7;
  }
}

.eyebrow {
  color: #4f7cff;
  font-size: 13px;
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.panel {
  margin-top: 18px;
  padding: 18px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-input {
  max-width: 320px;
}

.toolbar-select {
  width: 150px;
}

.issue-select {
  width: 180px;
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: -4px 0 16px;
  padding: 10px 12px;
  border: 1px solid #e6ebf4;
  border-radius: 6px;
  background: #f8fbff;
}

.quick-filter-label {
  margin-right: 4px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
}

.name-cell {
  color: #263243;
  font-weight: 700;
}

.muted {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.spot-warning-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 8px;
}

.issue-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.no-issue {
  color: #9aa3b2;
}

.image-cell {
  display: grid;
  gap: 8px;
  justify-items: center;
}

.admin-thumb,
.image-placeholder {
  width: 92px;
  height: 58px;
  border-radius: 6px;
}

.admin-thumb {
  display: block;
  border: 1px solid #e5e7eb;
}

.image-placeholder {
  display: grid;
  place-items: center;
  color: #9aa3b2;
  font-size: 12px;
  background: #f2f4f7;
  border: 1px dashed #d9dee8;
}

.image-editor-row {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  gap: 14px;
  width: 100%;
}

.form-preview {
  width: 160px;
  height: 104px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;

  &.empty {
    display: grid;
    place-items: center;
    color: #9aa3b2;
    background: #f2f4f7;
    border-style: dashed;
  }
}

.image-editor-main {
  display: grid;
  align-content: start;
  gap: 10px;
}

.tag-editor {
  display: grid;
  gap: 10px;
  width: 100%;

  :deep(.el-select) {
    width: 100%;
  }
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.edit-form {
  :deep(.el-select),
  :deep(.el-input-number) {
    width: 100%;
  }
}

.inline-fields {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
}

.drawer-panel {
  padding: 0 4px 24px;
}

@media (max-width: 960px) {
  .admin-main {
    padding: 18px 14px 32px;
  }

  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-input,
  .toolbar-select {
    max-width: none;
    width: 100%;
  }

  .inline-fields {
    grid-template-columns: 1fr;
  }

  .image-editor-row {
    grid-template-columns: 1fr;
  }
}
</style>
