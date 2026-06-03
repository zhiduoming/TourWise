<template>
  <div class="route-plan-container">
    <AppHeader />
    <el-row :gutter="20">
      <!-- 左侧控制面板 -->
      <el-col :xs="24" :lg="8">
        <el-card class="control-card">
          <template #header>
            <h3 class="card-title">
              <el-icon><MapLocation /></el-icon>
              路线规划
            </h3>
          </template>

          <el-form :model="routeForm" label-width="80px">
            <el-form-item label="规划类型">
              <el-radio-group v-model="routeSource" @change="handleRouteSourceChange">
                <el-radio label="amap" :disabled="!amapConfig.enabled">景点之间</el-radio>
                <el-radio label="local">景点内部</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-alert
              v-if="routeSource === 'amap' && !amapConfig.enabled"
              :title="amapConfig.message || '高德地图未配置，当前仅支持本地路线'"
              type="warning"
              :closable="false"
              show-icon
              class="route-alert"
            />
            <el-alert
              v-else-if="routeSource === 'amap'"
              title="景点之间使用高德真实道路路线，起点和终点请选择大景点；当前位置只作为起点和附近景点判断。"
              type="info"
              :closable="false"
              show-icon
              class="route-alert"
            />
            <el-alert
              v-else
              title="景点内部使用平面图和本地路网，只能选择已校准的校区/景区内部 POI。"
              type="info"
              :closable="false"
              show-icon
              class="route-alert"
            />

            <el-form-item v-if="routeSource === 'local'" label="内部景点">
              <el-select
                v-model="routeScope.placeGroupId"
                placeholder="请选择具体景点或校区"
                filterable
                style="width: 100%"
                @change="handleScopeChange"
              >
                <el-option
                  v-for="scope in routeScopes"
                  :key="scope.placeGroupId"
                  :label="`${scope.name}（${scope.poiCount} 个 POI）`"
                  :value="scope.placeGroupId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="精确定位">
              <div class="location-row">
                <el-button type="primary" plain :loading="locating" @click="handleUseCurrentLocation">
                  <el-icon><Aim /></el-icon>
                  精确定位
                </el-button>
                <el-tag v-if="currentLocation?.accuracy" :type="currentLocation.accuracy > 200 ? 'warning' : 'success'" effect="plain">
                  精度 {{ Math.round(currentLocation.accuracy) }}m
                </el-tag>
              </div>
              <div v-if="locationResolve" class="location-tip">
                <template v-if="routeSource === 'amap' && locationResolve.matchedSpot">
                  最近景点：{{ locationResolve.matchedSpot.name }}，
                  距离 {{ locationResolve.matchedSpot.distance }}m，
                  置信度 {{ confidenceText(locationResolve.matchedSpot.confidence) }}
                </template>
                <template v-else-if="routeSource === 'local' && locationResolve.matchedPoi">
                  最近 POI：{{ locationResolve.matchedPoi.name }}，
                  距离 {{ locationResolve.matchedPoi.distance }}m，
                  置信度 {{ confidenceText(locationResolve.matchedPoi.confidence) }}
                </template>
              </div>
            </el-form-item>

            <el-form-item v-if="isAdmin && routeScope.placeGroupId" label="地图底图">
              <div class="map-upload-row">
                <el-upload
                  :show-file-list="false"
                  accept="image/*"
                  :http-request="handleRouteMapUpload"
                >
                  <el-button type="primary" plain :loading="mapUploading">
                    <el-icon><Upload /></el-icon>
                    上传该景点地图
                  </el-button>
                </el-upload>
                <el-tag v-if="routeMap?.imageUrl" type="success" effect="plain">已配置</el-tag>
              </div>
            </el-form-item>

            <el-form-item label="起点">
              <el-select
                v-model="routeForm.start"
                placeholder="请选择起点"
                filterable
                clearable
                :disabled="routeSource === 'local' ? !allPOIs.length : !routeSpots.length"
                style="width: 100%"
              >
                <el-option
                  v-if="routeSource === 'amap' && currentLocation"
                  :label="currentLocationLabel"
                  value="__current__"
                />
                <el-option
                  v-for="item in routeStartOptions"
                  :key="`start-${item.value}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="poi-option">
                    <span>{{ item.label }}</span>
                    <el-tag v-if="item.tag" size="small">{{ item.tag }}</el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="终点">
              <el-select
                v-model="routeForm.end"
                placeholder="请选择终点"
                filterable
                clearable
                :disabled="routeSource === 'local' ? !allPOIs.length : !routeSpots.length"
                style="width: 100%"
              >
                <el-option
                  v-for="item in routeEndOptions"
                  :key="`end-${item.value}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="poi-option">
                    <span>{{ item.label }}</span>
                    <el-tag v-if="item.tag" size="small">{{ item.tag }}</el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item v-if="routeSource === 'amap'" label="途经景点">
              <div class="waypoints-container">
                <div
                  v-for="(point, index) in routeForm.waypoints"
                  :key="`amap-waypoint-${index}`"
                  class="waypoint-item"
                >
                  <el-select
                    v-model="routeForm.waypoints[index]"
                    placeholder="请选择途经景点"
                    filterable
                    clearable
                    size="small"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in routeEndOptions"
                      :key="`amap-waypoint-option-${index}-${item.value}`"
                      :label="item.label"
                      :value="item.value"
                    >
                      <div class="poi-option">
                        <span>{{ item.label }}</span>
                        <el-tag v-if="item.tag" size="small">{{ item.tag }}</el-tag>
                      </div>
                    </el-option>
                  </el-select>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    @click="removeWaypoint(index)"
                  />
                </div>
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :disabled="!routeSpots.length"
                  @click="addWaypoint"
                  style="width: 100%; margin-top: 8px"
                >
                  <el-icon><Plus /></el-icon>
                  添加途经景点
                </el-button>
              </div>
            </el-form-item>

            <el-form-item v-if="routeSource === 'local'" label="途经点">
              <div class="waypoints-container">
                <div
                  v-for="(point, index) in routeForm.waypoints"
                  :key="index"
                  class="waypoint-item"
                >
                  <el-select
                    v-model="routeForm.waypoints[index]"
                    placeholder="请选择途经点"
                    filterable
                    clearable
                    size="small"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="poi in allPOIs"
                      :key="`waypoint-${index}-${poi.id}`"
                      :label="poi.name"
                      :value="poi.name"
                    >
                      <div class="poi-option">
                        <span>{{ poi.name }}</span>
                        <el-tag size="small">{{ poi.category }}</el-tag>
                      </div>
                    </el-option>
                  </el-select>
                  <el-button
                    type="danger"
                    :icon="Delete"
                    size="small"
                    @click="removeWaypoint(index)"
                  />
                </div>
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :disabled="!allPOIs.length"
                  @click="addWaypoint"
                  style="width: 100%; margin-top: 8px"
                >
                  <el-icon><Plus /></el-icon>
                  添加途经点
                </el-button>
              </div>
            </el-form-item>

            <el-form-item v-if="routeSource === 'amap'" label="高德模式">
              <el-radio-group v-model="routeForm.amapMode">
                <el-radio label="walking">步行</el-radio>
                <el-radio label="driving">驾车</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="routeSource === 'local'" label="规划模式">
              <el-radio-group v-model="routeForm.mode">
                <el-radio label="shortest">最短路径</el-radio>
                <el-radio label="optimal">最优路线</el-radio>
                <el-radio label="indoor">楼宇内</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="routeSource === 'local'" label="偏好">
              <el-checkbox-group v-model="routeForm.preferences">
                <el-checkbox label="avoid_crowd">避开拥挤</el-checkbox>
                <el-checkbox label="scenic">风景好</el-checkbox>
                <el-checkbox label="fastest">时间最短</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" style="width: 100%" @click="handlePlanRoute">
                <el-icon><Navigation /></el-icon>
                开始规划
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button plain style="width: 100%" @click="openRouteHistory">
                路线历史
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 路线信息 -->
          <div v-if="routeResult" class="route-result">
            <el-divider content-position="left">规划结果</el-divider>
            <div class="route-stats">
              <div class="stat-item">
                <el-icon><Timer /></el-icon>
                <span>预计时间</span>
                <strong>{{ routeResult.duration }}分钟</strong>
              </div>
              <div class="stat-item">
                <el-icon><ScaleToOriginal /></el-icon>
                <span>总距离</span>
                <strong>{{ routeResult.distance }}米</strong>
              </div>
            </div>
            <el-timeline>
              <el-timeline-item
                v-for="(point, index) in routeResult.points"
                :key="index"
                :timestamp="point.name"
                placement="top"
              >
                <el-card>
                  <p>{{ point.description }}</p>
                  <span class="point-distance">距离：{{ point.distance }}m</span>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <div class="route-result-actions">
              <el-button type="primary" plain :loading="savingRecord" @click="handleSaveRouteRecord">
                保存路线
              </el-button>
              <el-button type="success" @click="handleNavigate">
                <el-icon><Guide /></el-icon>
                开始导航
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 景点选择器 -->
        <el-card v-if="routeSource === 'local'" class="poi-card" style="margin-top: 20px">
          <template #header>
            <h3 class="card-title">
              <el-icon><Collection /></el-icon>
              {{ routeScope.name ? `${routeScope.name} POI` : '选择景点' }}
            </h3>
          </template>
          <el-input
            v-model="poiSearch"
            placeholder="搜索景点"
            prefix-icon="Search"
            clearable
          />
          <el-checkbox-group v-model="selectedPOIs" class="poi-list">
            <div
              v-for="poi in filteredPOIs"
              :key="poi.id"
              class="poi-item"
            >
              <div class="poi-main">
                <el-checkbox :label="poi.id">
                  {{ poi.name }}
                </el-checkbox>
                <el-tag size="small">{{ poi.category }}</el-tag>
              </div>
              <div class="poi-actions">
                <el-button size="small" text @click.stop="setRoutePoint('start', poi)">起点</el-button>
                <el-button size="small" text @click.stop="setRoutePoint('end', poi)">终点</el-button>
              </div>
            </div>
          </el-checkbox-group>
          <el-empty
            v-if="!filteredPOIs.length"
            :description="routeScope.name ? '当前地点暂无可规划 POI' : '暂无可规划 POI'"
            :image-size="80"
          />
          <el-button
            type="primary"
            plain
            style="width: 100%; margin-top: 12px"
            :disabled="!selectedPOIs.length"
            @click="handleAddSelectedPOIs"
          >
            添加到路线
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧地图 -->
      <el-col :xs="24" :lg="16">
        <el-card class="map-card">
          <div
            v-show="routeSource === 'amap'"
            ref="amapContainer"
            class="map-container amap-container"
          >
            <div v-if="!amapConfig.enabled" class="amap-disabled">
              {{ amapConfig.message || '高德地图未配置' }}
            </div>
          </div>
          <div
            v-show="routeSource === 'local'"
            id="route-map"
            class="map-container"
            :class="{ 'has-map-image': routeMap?.imageUrl }"
          >
            <svg class="route-svg" :viewBox="mapViewBox" preserveAspectRatio="xMidYMid meet">
              <defs>
                <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
                  <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#d7dde8" stroke-width="1" />
                </pattern>
              </defs>
              <image
                v-if="routeMap?.imageUrl"
                :href="routeMap.imageUrl"
                x="0"
                y="0"
                :width="mapWidth"
                :height="mapHeight"
                preserveAspectRatio="none"
              />
              <rect v-if="!routeMap?.imageUrl" :width="mapWidth" :height="mapHeight" fill="#f7f9fc" />
              <rect v-if="!routeMap?.imageUrl" :width="mapWidth" :height="mapHeight" fill="url(#grid)" />
              <polyline
                v-if="routePolyline"
                :points="routePolyline"
                fill="none"
                stroke="#409eff"
                stroke-width="8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <g v-for="poi in visibleMapPOIs" :key="`poi-${poi.id}`">
                <circle
                  :cx="poi.x"
                  :cy="poi.y"
                  :r="poi.inRoute ? 9 : 4"
                  :fill="poi.inRoute ? '#67c23a' : '#9aa7bd'"
                  :stroke="poi.inRoute ? '#2f8f46' : '#ffffff'"
                  stroke-width="3"
                />
              </g>
              <g v-for="(point, index) in routeMapPoints" :key="`route-${point.id}-${index}`">
                <circle
                  :cx="point.x"
                  :cy="point.y"
                  r="14"
                  :fill="index === 0 ? '#409eff' : index === routeMapPoints.length - 1 ? '#f56c6c' : '#67c23a'"
                  stroke="#ffffff"
                  stroke-width="4"
                />
                <text
                  :x="point.labelX"
                  :y="point.labelY"
                  :text-anchor="point.labelAnchor"
                  class="map-label"
                >
                  {{ point.name }}
                </text>
              </g>
            </svg>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="historyDrawerVisible" title="我的路线历史" size="520px" @open="loadRouteHistory">
      <div class="history-drawer">
        <el-skeleton v-if="historyLoading" :rows="5" animated />
        <template v-else>
          <el-empty v-if="!routeRecords.length" description="暂无路线记录" />
          <div v-for="record in routeRecords" :key="record.id" class="history-item">
            <div class="history-head">
              <div>
                <h4>{{ record.routeName }}</h4>
                <p>{{ record.createdAt }}</p>
              </div>
              <el-tag :type="record.metadata?.provider === 'amap' ? 'success' : 'info'">
                {{ record.metadata?.provider === 'amap' ? '高德路线' : '本地路线' }}
              </el-tag>
            </div>
            <div class="history-stats">
              <span>{{ record.totalDurationMin }} 分钟</span>
              <span>{{ record.totalDistanceM }} 米</span>
              <span>{{ algorithmText(record.metadata?.algorithm || record.mode) }}</span>
            </div>
            <div v-if="record.metadata?.sourcePlanTitle" class="history-source">
              来源行程：{{ record.metadata.sourcePlanTitle }}
            </div>
            <div class="history-points">
              <span
                v-for="point in record.points"
                :key="point.id"
              >
                {{ point.pointName }}
              </span>
            </div>
            <div class="history-actions">
              <el-button
                v-if="record.metadata?.sourcePlanId"
                text
                type="primary"
                @click="openSourcePlan(record)"
              >
                查看来源行程
              </el-button>
              <el-button text type="danger" @click="removeRouteRecord(record)">删除</el-button>
            </div>
          </div>
          <div class="pagination-row">
            <el-pagination
              small
              background
              layout="prev, pager, next"
              :total="routeRecordTotal"
              v-model:current-page="routeRecordQuery.page"
              :page-size="routeRecordQuery.pageSize"
              @current-change="loadRouteHistory"
            />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AMapLoader from '@amap/amap-jsapi-loader'
import AppHeader from '@/components/AppHeader.vue'
import {
  getShortestPath,
  getOptimalRoute,
  getIndoorPath,
  getPOIs,
  getRouteScopes,
  getRouteSpots,
  getRouteMap,
  uploadRouteMap,
  getAmapConfig,
  planAmapRoute,
  resolveLocation,
  saveRouteRecord,
  getRouteRecords,
  deleteRouteRecord
} from '@/api/route'
import { useUserStore } from '@/stores/user'
import { Delete, Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const routeForm = reactive({
  start: '',
  end: '',
  waypoints: [],
  mode: 'shortest',
  amapMode: 'walking',
  preferences: []
})

const poiSearch = ref('')
const routeSource = ref('local')
const selectedPOIs = ref([])
const allPOIs = ref([])
const routeScopes = ref([])
const routeSpots = ref([])
const routeResult = ref(null)
const routeMap = ref(null)
const mapUploading = ref(false)
const savingRecord = ref(false)
const currentLocation = ref(null)
const locationResolve = ref(null)
const locating = ref(false)
const historyDrawerVisible = ref(false)
const historyLoading = ref(false)
const routeRecords = ref([])
const routeRecordTotal = ref(0)
const routeRecordQuery = reactive({
  page: 1,
  pageSize: 6
})
const amapConfig = reactive({
  enabled: false,
  jsKey: '',
  securityCode: '',
  message: ''
})
const amapContainer = ref(null)
const amapApi = ref(null)
const amapInstance = ref(null)
const amapOverlays = ref([])
const routeScope = reactive({
  spotId: null,
  placeGroupId: null,
  name: ''
})
const sourcePlan = reactive({
  id: null,
  title: ''
})

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
const mapWidth = computed(() => routeMap.value?.mapWidth || 1000)
const mapHeight = computed(() => routeMap.value?.mapHeight || 680)
const mapViewBox = computed(() => `0 0 ${mapWidth.value} ${mapHeight.value}`)

const hasPlanPoint = (point) => {
  return point?.mapX != null && point?.mapY != null
}

const hasGeoPoint = (point) => {
  return point?.longitude != null && point?.latitude != null
}

const filteredPOIs = computed(() => {
  if (!poiSearch.value) return allPOIs.value
  return allPOIs.value.filter(poi =>
    poi.name.toLowerCase().includes(poiSearch.value.toLowerCase())
  )
})

const currentLocationLabel = computed(() => {
  if (!currentLocation.value) return '当前位置'
  const match = locationResolve.value?.matchedSpot || locationResolve.value?.matchedPoi
  const accuracy = currentLocation.value.accuracy ? `，精度 ${Math.round(currentLocation.value.accuracy)}m` : ''
  return match ? `当前位置（近 ${match.name}${accuracy}）` : `当前位置${accuracy}`
})

const routeStartOptions = computed(() => {
  if (routeSource.value === 'amap') {
    return routeSpots.value.map(spot => ({
      label: spot.name,
      value: `spot:${spot.id}`,
      tag: [spot.city, spot.spotType].filter(Boolean).join(' / ')
    }))
  }
  return allPOIs.value.map(poi => ({
    label: poi.name,
    value: poi.name,
    tag: poi.category
  }))
})

const routeEndOptions = computed(() => {
  if (routeSource.value === 'amap') {
    return routeSpots.value.map(spot => ({
      label: spot.name,
      value: `spot:${spot.id}`,
      tag: [spot.city, spot.spotType].filter(Boolean).join(' / ')
    }))
  }
  return routeStartOptions.value
})

const mapSourcePoints = computed(() => {
  const points = routeResult.value?.points?.length ? routeResult.value.points : allPOIs.value.slice(0, 80)
  return points.filter(p => hasPlanPoint(p) || hasGeoPoint(p))
})

const mapBounds = computed(() => {
  const points = mapSourcePoints.value.filter(hasGeoPoint)
  if (!points.length) return null
  const lngs = points.map(p => Number(p.longitude))
  const lats = points.map(p => Number(p.latitude))
  const minLng = Math.min(...lngs)
  const maxLng = Math.max(...lngs)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  return {
    minLng,
    maxLng: maxLng === minLng ? maxLng + 0.001 : maxLng,
    minLat,
    maxLat: maxLat === minLat ? maxLat + 0.001 : maxLat
  }
})

const projectPoint = (point) => {
  if (hasPlanPoint(point)) {
    const x = Number(point.mapX)
    const y = Number(point.mapY)
    return {
      ...point,
      x,
      y,
      labelX: x > mapWidth.value - 180 ? x - 24 : x + 24,
      labelY: y < 80 ? y + 36 : y - 14,
      labelAnchor: x > mapWidth.value - 180 ? 'end' : 'start'
    }
  }
  const bounds = mapBounds.value
  if (!bounds) return { ...point, x: mapWidth.value / 2, y: mapHeight.value / 2 }
  const padding = 56
  const x = padding + ((Number(point.longitude) - bounds.minLng) / (bounds.maxLng - bounds.minLng)) * (mapWidth.value - padding * 2)
  const y = mapHeight.value - padding - ((Number(point.latitude) - bounds.minLat) / (bounds.maxLat - bounds.minLat)) * (mapHeight.value - padding * 2)
  return {
    ...point,
    x,
    y,
    labelX: x > mapWidth.value - 180 ? x - 18 : x + 18,
    labelY: y < 80 ? y + 30 : y - 10,
    labelAnchor: x > mapWidth.value - 180 ? 'end' : 'start'
  }
}

const routeMapPoints = computed(() => {
  return (routeResult.value?.points || [])
    .filter(p => hasPlanPoint(p) || hasGeoPoint(p))
    .map(projectPoint)
})

const routePolyline = computed(() => {
  if (routeMapPoints.value.length < 2) return ''
  return routeMapPoints.value.map(p => `${p.x},${p.y}`).join(' ')
})

const visibleMapPOIs = computed(() => {
  const routeIds = new Set((routeResult.value?.points || []).map(p => p.id))
  const pois = routeResult.value?.points?.length ? routeResult.value.points : allPOIs.value.slice(0, 80)
  return pois
    .filter(p => hasPlanPoint(p) || hasGeoPoint(p))
    .map(p => ({ ...projectPoint(p), inRoute: routeIds.has(p.id) }))
})

const addWaypoint = () => {
  routeForm.waypoints.push('')
}

const removeWaypoint = (index) => {
  routeForm.waypoints.splice(index, 1)
}

const setRoutePoint = (field, poi) => {
  routeForm[field] = poi.name
}

const findPoiByRouteValue = (value) => {
  return allPOIs.value.find(item => item.name === value || String(item.id) === String(value))
}

const handleScopeChange = async (placeGroupId) => {
  const scope = routeScopes.value.find(item => item.placeGroupId === placeGroupId)
  routeScope.spotId = null
  routeScope.name = scope?.name || ''
  routeForm.start = ''
  routeForm.end = ''
  routeForm.waypoints = []
  selectedPOIs.value = []
  routeResult.value = null
  locationResolve.value = null
  await loadRoutePOIs()
  await loadRouteMap()
  if (routeSource.value === 'amap') {
    await initAmap()
  }
}

const handleUseCurrentLocation = async () => {
  if (routeSource.value === 'local' && !routeScope.placeGroupId) {
    ElMessage.warning('请先选择支持内部路线的景点或校区')
    return
  }
  locating.value = true
  try {
    let location
    try {
      location = await locateByAmap()
    } catch (error) {
      location = await locateByBrowser()
    }
    currentLocation.value = location
    const res = await resolveLocation({
      mode: routeSource.value === 'amap' ? 'between' : 'internal',
      placeGroupId: routeSource.value === 'local' ? routeScope.placeGroupId : null,
      longitude: location.longitude,
      latitude: location.latitude,
      accuracy: Math.round(location.accuracy || 200),
      provider: location.provider,
      coordinateSystem: location.coordinateSystem
    })
    locationResolve.value = res.data
    if (res.data?.coordinate) {
      currentLocation.value = {
        ...currentLocation.value,
        longitude: Number(res.data.coordinate.longitude),
        latitude: Number(res.data.coordinate.latitude),
        coordinateSystem: res.data.coordinate.coordinateSystem || 'gcj02'
      }
    }

    if (routeSource.value === 'amap') {
      routeForm.start = '__current__'
      if (locationResolve.value?.matchedSpot?.confidence === 'low') {
        ElMessage.warning('已获取当前位置，但离最近景点较远，建议手动确认起点')
      } else {
        ElMessage.success('已使用当前位置作为景点之间路线起点')
      }
      await initAmap()
      drawCurrentLocationMarker()
    } else {
      const match = locationResolve.value?.matchedPoi
      if (match && match.confidence !== 'low') {
        routeForm.start = match.name
        ElMessage.success(`已匹配最近 POI：${match.name}`)
      } else {
        ElMessage.warning('定位精度不足或距离 POI 较远，请手动选择内部起点')
      }
    }
    if ((currentLocation.value.accuracy || 0) > 200) {
      ElMessage.warning('当前定位精度较低，电脑端定位只能作为辅助')
    }
  } catch (error) {
    console.error('定位失败:', error)
    ElMessage.warning('无法获取当前位置，请检查浏览器定位权限')
  } finally {
    locating.value = false
  }
}

const locateByAmap = async () => {
  if (!amapConfig.enabled || !amapConfig.jsKey) {
    throw new Error('amap disabled')
  }
  const AMap = await loadAmapApi()
  return new Promise((resolve, reject) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
      convert: true,
      showButton: false,
      showMarker: false,
      showCircle: false
    })
    geolocation.getCurrentPosition((status, result) => {
      if (status === 'complete' && result?.position) {
        resolve({
          name: '当前位置',
          longitude: Number(result.position.lng),
          latitude: Number(result.position.lat),
          accuracy: Number(result.accuracy || result.position?.accuracy || 200),
          provider: 'amap',
          coordinateSystem: 'gcj02'
        })
      } else {
        reject(new Error(result?.message || 'amap location failed'))
      }
    })
  })
}

const locateByBrowser = () => {
  if (!navigator.geolocation) {
    return Promise.reject(new Error('geolocation unsupported'))
  }
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      position => {
        resolve({
          name: '当前位置',
          longitude: Number(position.coords.longitude),
          latitude: Number(position.coords.latitude),
          accuracy: Number(position.coords.accuracy || 200),
          provider: 'browser',
          coordinateSystem: 'wgs84'
        })
      },
      reject,
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

const confidenceText = (value) => {
  if (value === 'high') return '高'
  if (value === 'medium') return '中'
  return '低'
}

const handlePlanRoute = async () => {
  if (routeSource.value === 'local' && !routeScope.placeGroupId) {
    ElMessage.warning('请先选择支持内部路线的景点或校区')
    return
  }
  if (!routeForm.start || !routeForm.end) {
    ElMessage.warning('请选择起点和终点')
    return
  }

  try {
    let res
    if (routeSource.value === 'amap') {
      if (!amapConfig.enabled) {
        ElMessage.warning('高德地图未配置，当前仅支持本地路线')
        return
      }
      const origin = resolveAmapPoint(routeForm.start)
      const destination = resolveAmapPoint(routeForm.end)
      const waypoints = routeForm.waypoints
        .filter(Boolean)
        .map(resolveAmapPoint)
        .filter(Boolean)
      if (!origin || !destination) {
        ElMessage.warning('景点之间路线需要起点和终点都有经纬度')
        return
      }
      res = await planAmapRoute({
        origin,
        destination,
        waypoints,
        mode: routeForm.amapMode
      })
    } else {
      res = await planLocalRoute()
    }

    routeResult.value = res.data
    if (routeSource.value === 'amap') {
      await drawAmapRoute()
    }
    if (routeResult.value?.message) {
      ElMessage.info(routeResult.value.message)
    }
  } catch (error) {
    console.error('路线规划失败:', error)
    ElMessage.error('路线规划失败，请稍后重试')
  }
}

const planLocalRoute = () => {
  const commonPayload = {
    start: routeForm.start,
    end: routeForm.end,
    waypoints: routeForm.waypoints.filter(w => w),
    preferences: routeForm.preferences,
    spotId: routeScope.spotId,
    placeGroupId: routeScope.placeGroupId
  }
  if (routeForm.mode === 'optimal') {
    return getOptimalRoute({
      points: [routeForm.start, ...routeForm.waypoints.filter(w => w), routeForm.end],
      preferences: routeForm.preferences,
      spotId: routeScope.spotId,
      placeGroupId: routeScope.placeGroupId
    })
  }
  if (routeForm.mode === 'indoor') {
    return getIndoorPath(commonPayload)
  }
  return getShortestPath(commonPayload)
}

const isScopedInternalAmapRoute = () => {
  if (!routeScope.placeGroupId || routeForm.start === '__current__') {
    return false
  }
  const values = [routeForm.start, ...routeForm.waypoints.filter(Boolean), routeForm.end]
  return values.length >= 2 && values.every(value => {
    const poi = findPoiByRouteValue(value)
    return poi && Number(poi.placeGroupId) === Number(routeScope.placeGroupId)
  })
}

const handleNavigate = () => {
  if (routeResult.value?.provider === 'amap') {
    const origin = resolveAmapPoint(routeForm.start)
    const destination = resolveAmapPoint(routeForm.end)
    if (origin && destination) {
      const url = `https://uri.amap.com/navigation?from=${origin.longitude},${origin.latitude},起点&to=${destination.longitude},${destination.latitude},终点&mode=${routeForm.amapMode === 'driving' ? 'car' : 'walk'}&policy=1&src=tourwise`
      window.open(url, '_blank')
      return
    }
  }
  ElMessage.success('开始导航')
}

const openRouteHistory = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后查看路线历史')
    return
  }
  historyDrawerVisible.value = true
}

const loadRouteHistory = async () => {
  if (!historyDrawerVisible.value && routeRecords.value.length) {
    return
  }
  historyLoading.value = true
  try {
    const res = await getRouteRecords({
      page: routeRecordQuery.page,
      pageSize: routeRecordQuery.pageSize
    })
    routeRecords.value = res.data?.list || []
    routeRecordTotal.value = res.data?.total || 0
  } finally {
    historyLoading.value = false
  }
}

const handleSaveRouteRecord = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后保存路线')
    return
  }
  if (!routeResult.value || !routeResult.value.points?.length) {
    ElMessage.warning('请先完成路线规划')
    return
  }
  const defaultName = sourcePlan.title
    ? `${sourcePlan.title}路线`
    : `${displayRouteValue(routeForm.start)} 到 ${displayRouteValue(routeForm.end)}`
  try {
    const { value } = await ElMessageBox.prompt('给这条路线起个名字', '保存路线', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: defaultName,
      inputValidator: text => Boolean(text && text.trim()) || '路线名称不能为空'
    })
    savingRecord.value = true
    await saveRouteRecord(buildRouteRecordPayload(value))
    ElMessage.success('路线已保存')
    if (historyDrawerVisible.value) {
      await loadRouteHistory()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存路线失败:', error)
    }
  } finally {
    savingRecord.value = false
  }
}

const buildRouteRecordPayload = (routeName) => {
  const points = (routeResult.value.points || []).map(point => ({
    poiId: point.id || null,
    pointName: point.name || '路线点',
    distanceFromStartM: point.distance ?? null,
    description: point.description || ''
  }))
  return {
    routeName,
    routeType: routeSource.value === 'amap' ? 'between' : 'internal',
    provider: routeResult.value.provider || (routeSource.value === 'amap' ? 'amap' : 'local'),
    algorithm: routeResult.value.algorithm || '',
    mode: routeSource.value === 'amap' ? 'shortest' : routeForm.mode,
    placeGroupId: routeScope.placeGroupId,
    spotId: routeScope.spotId,
    sourcePlanId: sourcePlan.id,
    sourcePlanTitle: sourcePlan.title,
    startName: displayRouteValue(routeForm.start),
    endName: displayRouteValue(routeForm.end),
    totalDistanceM: routeResult.value.distance || 0,
    totalDurationMin: routeResult.value.duration || 0,
    preferences: routeForm.preferences || [],
    points
  }
}

const removeRouteRecord = async (record) => {
  try {
    await ElMessageBox.confirm(`确定删除路线记录「${record.routeName}」吗？`, '删除路线记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteRouteRecord(record.id)
    ElMessage.success('路线记录已删除')
    await loadRouteHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除路线记录失败:', error)
    }
  }
}

const openSourcePlan = (record) => {
  const planId = record?.metadata?.sourcePlanId
  if (!planId) {
    return
  }
  router.push({ path: '/itinerary', query: { planId } })
}

const displayRouteValue = (value) => {
  if (value === '__current__') {
    return currentLocationLabel.value || '当前位置'
  }
  if (typeof value === 'string' && value.startsWith('spot:')) {
    const spotId = value.replace('spot:', '')
    return routeSpots.value.find(item => String(item.id) === String(spotId))?.name || value
  }
  return value || '路线点'
}

const algorithmText = (value) => {
  if (value === 'astar') return 'A*'
  if (value === 'dijkstra') return 'Dijkstra'
  if (value === 'tsp_dp') return '多点 DP'
  if (value === 'nearest_2opt') return '最近邻 + 2-opt'
  if (value === 'amap') return '高德'
  if (value === 'optimal') return '最优路线'
  if (value === 'indoor') return '楼宇内'
  return '最短路径'
}

const handleAddSelectedPOIs = () => {
  const selected = allPOIs.value.filter(poi => selectedPOIs.value.includes(poi.id))
  selected.forEach(poi => {
    if (!routeForm.waypoints.includes(poi.name)) {
      routeForm.waypoints.push(poi.name)
    }
  })
  ElMessage.success(`已添加 ${selected.length} 个景点`)
}

const handleRouteMapUpload = async ({ file }) => {
  if (!routeScope.placeGroupId) {
    ElMessage.warning('请先选择具体景点或校区')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  mapUploading.value = true
  try {
    const res = await uploadRouteMap(routeScope.placeGroupId, formData)
    routeMap.value = res.data
    ElMessage.success('地图底图已上传')
  } catch (error) {
    console.error('地图底图上传失败:', error)
  } finally {
    mapUploading.value = false
  }
}

const loadRouteScopes = async () => {
  const res = await getRouteScopes()
  routeScopes.value = res.data || []
}

const loadRouteSpots = async () => {
  const res = await getRouteSpots()
  routeSpots.value = res.data || []
}

const loadRoutePOIs = async () => {
  try {
    const params = {}
    if (routeScope.spotId) {
      params.spotId = routeScope.spotId
    } else if (routeScope.placeGroupId) {
      params.placeGroupId = routeScope.placeGroupId
    } else {
      allPOIs.value = []
      return
    }
    const res = await getPOIs(params)
    allPOIs.value = res.data || []
    if (routeScope.spotId && allPOIs.value.length) {
      routeScope.placeGroupId = allPOIs.value[0].placeGroupId
    }
    const addPOI = route.query.addPOI
    if (addPOI) {
      const poi = allPOIs.value.find(item => String(item.id) === String(addPOI))
      if (poi && !routeForm.end) {
        routeForm.end = poi.name
      }
    }
  } catch (error) {
    console.error('加载路线 POI 失败:', error)
    allPOIs.value = []
    ElMessage.error('加载路线 POI 失败')
  }
}

const loadRouteMap = async () => {
  if (!routeScope.placeGroupId) {
    routeMap.value = null
    return
  }
  try {
    const res = await getRouteMap(routeScope.placeGroupId)
    routeMap.value = res.data || null
  } catch (error) {
    routeMap.value = null
  }
}

const loadAmapConfig = async () => {
  try {
    const res = await getAmapConfig()
    Object.assign(amapConfig, res.data || {})
  } catch (error) {
    console.error('加载高德配置失败:', error)
    amapConfig.enabled = false
    amapConfig.message = '高德地图配置加载失败'
  }
}

const handleRouteSourceChange = async (value) => {
  routeResult.value = null
  locationResolve.value = null
  routeForm.start = ''
  routeForm.end = ''
  routeForm.waypoints = []
  if (value === 'amap') {
    await initAmap()
  } else {
    clearAmapOverlays()
  }
}

const applyFoodRouteQuery = () => {
  const { startSpotId, endLng, endLat, endName } = route.query
  if (!startSpotId || !endLng || !endLat) {
    return false
  }
  if (!amapConfig.enabled) {
    ElMessage.warning('高德地图未配置，暂时无法规划到该美食店的路线')
    return false
  }
  const startSpot = routeSpots.value.find(s => String(s.id) === String(startSpotId))
  if (!startSpot || startSpot.longitude == null || startSpot.latitude == null) {
    ElMessage.warning('未找到起点景点的坐标')
    return false
  }
  const lngNum = Number(endLng)
  const latNum = Number(endLat)
  if (!Number.isFinite(lngNum) || !Number.isFinite(latNum)) {
    return false
  }
  // 把美食店作为一个临时景点插入 routeSpots，使其能被 spot:ID 引用机制解析
  const virtualId = `food-${Date.now()}`
  routeSpots.value.push({
    id: virtualId,
    name: String(endName || '美食目的地'),
    longitude: lngNum,
    latitude: latNum
  })
  routeSource.value = 'amap'
  routeForm.start = `spot:${startSpot.id}`
  routeForm.end = `spot:${virtualId}`
  routeForm.waypoints = []
  routeResult.value = null
  locationResolve.value = null
  return true
}

const applyItineraryRouteQuery = () => {
  if (route.query.routeType !== 'between' || !route.query.routeSpotIds) {
    return false
  }
  if (!amapConfig.enabled) {
    ElMessage.warning('高德地图未配置，暂时无法载入行程景点之间路线')
    return false
  }
  const ids = String(route.query.routeSpotIds)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
  const values = ids
    .map(id => routeSpots.value.find(spot => String(spot.id) === String(id)))
    .filter(spot => spot?.longitude != null && spot?.latitude != null)
    .map(spot => `spot:${spot.id}`)
  if (values.length < 2) {
    ElMessage.warning('行程中的可规划景点不足，无法自动生成景点之间路线')
    return false
  }
  routeSource.value = 'amap'
  routeForm.start = values[0]
  routeForm.end = values[values.length - 1]
  routeForm.waypoints = values.slice(1, -1)
  sourcePlan.id = route.query.sourcePlanId ? Number(route.query.sourcePlanId) : null
  sourcePlan.title = route.query.routeTitle ? String(route.query.routeTitle) : ''
  routeResult.value = null
  locationResolve.value = null
  return true
}

const resolveAmapPoint = (value) => {
  if (value === '__current__') {
    return currentLocation.value
  }
  if (typeof value === 'string' && value.startsWith('spot:')) {
    const spotId = value.replace('spot:', '')
    const spot = routeSpots.value.find(item => String(item.id) === String(spotId))
    if (spot?.longitude != null && spot?.latitude != null) {
      return {
        name: spot.name,
        longitude: Number(spot.longitude),
        latitude: Number(spot.latitude)
      }
    }
  }
  const poi = allPOIs.value.find(item => item.name === value || String(item.id) === String(value))
  if (poi?.longitude != null && poi?.latitude != null) {
    return {
      name: poi.name,
      longitude: Number(poi.longitude),
      latitude: Number(poi.latitude)
    }
  }
  if (typeof value === 'string' && value.includes(',')) {
    const parts = value.split(',').map(item => Number(item.trim()))
    if (parts.length === 2 && parts.every(Number.isFinite)) {
      return { name: '路线点', latitude: parts[0], longitude: parts[1] }
    }
  }
  return null
}

const initAmap = async () => {
  if (!amapConfig.enabled || !amapConfig.jsKey || !amapContainer.value) {
    return
  }
  await nextTick()
  if (amapInstance.value) {
    return
  }
  const AMap = await loadAmapApi()
  amapInstance.value = new AMap.Map(amapContainer.value, {
    zoom: 16,
    center: defaultAmapCenter()
  })
  amapInstance.value.addControl(new AMap.Scale())
  amapInstance.value.addControl(new AMap.ToolBar())
}

const loadAmapApi = async () => {
  if (amapApi.value) {
    return amapApi.value
  }
  window._AMapSecurityConfig = {
    securityJsCode: amapConfig.securityCode || ''
  }
  const AMap = await AMapLoader.load({
    key: amapConfig.jsKey,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geolocation']
  })
  amapApi.value = AMap
  return AMap
}

const defaultAmapCenter = () => {
  const current = currentLocation.value
  if (current?.longitude != null && current?.latitude != null) {
    return [Number(current.longitude), Number(current.latitude)]
  }
  const spot = routeSpots.value.find(item => item.longitude != null && item.latitude != null)
  if (spot) {
    return [Number(spot.longitude), Number(spot.latitude)]
  }
  const first = allPOIs.value.find(item => item.longitude != null && item.latitude != null)
  if (first) {
    return [Number(first.longitude), Number(first.latitude)]
  }
  return [116.397428, 39.90923]
}

const clearAmapOverlays = () => {
  if (amapInstance.value && amapOverlays.value.length) {
    amapInstance.value.remove(amapOverlays.value)
  }
  amapOverlays.value = []
}

const drawAmapRoute = async () => {
  await initAmap()
  if (!amapApi.value || !amapInstance.value || !routeResult.value) {
    return
  }
  clearAmapOverlays()
  const AMap = amapApi.value
  const path = (routeResult.value.polyline || [])
    .filter(point => point.longitude != null && point.latitude != null)
    .map(point => [Number(point.longitude), Number(point.latitude)])
  if (path.length >= 2) {
    const polyline = new AMap.Polyline({
      path,
      strokeColor: '#409eff',
      strokeWeight: 7,
      strokeOpacity: 0.9,
      lineJoin: 'round',
      lineCap: 'round'
    })
    amapOverlays.value.push(polyline)
  }
  const routePoints = routeResult.value.points || []
  routePoints.forEach((point, index) => {
    if (point.longitude == null || point.latitude == null) return
    const marker = new AMap.Marker({
      position: [Number(point.longitude), Number(point.latitude)],
      title: point.name,
      label: {
        content: index === 0 ? '起点' : index === routePoints.length - 1 ? '终点' : `途经${index}`,
        direction: 'top'
      }
    })
    amapOverlays.value.push(marker)
  })
  if (amapOverlays.value.length) {
    amapInstance.value.add(amapOverlays.value)
    amapInstance.value.setFitView(amapOverlays.value, false, [60, 60, 60, 60])
  }
}

const drawCurrentLocationMarker = () => {
  if (!amapApi.value || !amapInstance.value || !currentLocation.value) {
    return
  }
  const AMap = amapApi.value
  const marker = new AMap.Marker({
    position: [Number(currentLocation.value.longitude), Number(currentLocation.value.latitude)],
    title: '当前位置',
    label: {
      content: '当前位置',
      direction: 'top'
    }
  })
  amapInstance.value.add(marker)
  amapOverlays.value.push(marker)
  amapInstance.value.setCenter([Number(currentLocation.value.longitude), Number(currentLocation.value.latitude)])
}

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.getUserInfoAction()
    } catch (error) {
      userStore.clearLoginState()
    }
  }
  await loadAmapConfig()
  await loadRouteSpots()
  await loadRouteScopes()

  const appliedItineraryRoute = applyItineraryRouteQuery()
  const appliedFoodRoute = !appliedItineraryRoute && applyFoodRouteQuery()

  if (!appliedItineraryRoute && !appliedFoodRoute && route.query.placeGroupId) {
    routeSource.value = 'local'
    routeScope.spotId = null
    routeScope.placeGroupId = Number(route.query.placeGroupId)
    routeScope.name = route.query.scopeName || route.query.endName || ''
    if (route.query.endName) {
      routeForm.end = route.query.endName
    }
  } else if (!appliedItineraryRoute && !appliedFoodRoute && route.query.spotId) {
    routeSource.value = amapConfig.enabled ? 'amap' : 'local'
    routeForm.end = `spot:${route.query.spotId}`
  }
  if (!appliedItineraryRoute && !appliedFoodRoute && route.query.end && routeSource.value === 'local') {
    routeForm.end = route.query.end
  }

  await loadRoutePOIs()
  await loadRouteMap()
  if (routeSource.value === 'amap') {
    await initAmap()
  }
})

watch(routeSource, async value => {
  if (value === 'amap') {
    await initAmap()
  }
})

onBeforeUnmount(() => {
  if (amapInstance.value) {
    amapInstance.value.destroy()
  }
})
</script>

<style lang="scss" scoped>
.route-plan-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #303133;
}

.control-card, .poi-card, .map-card {
  height: fit-content;
}

.waypoints-container {
  width: 100%;
}

.waypoint-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.map-upload-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.location-tip {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.route-alert {
  margin-bottom: 16px;
}

.current-location-btn {
  margin-top: 8px;
}

.poi-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-result {
  margin-top: 20px;
}

.route-result-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.route-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  
  strong {
    color: #67c23a;
    font-size: 18px;
  }
}

.point-distance {
  color: #909399;
  font-size: 13px;
}

.poi-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 12px;
}

.poi-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.poi-main {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.poi-actions {
  display: flex;
  gap: 4px;
}

.map-container {
  position: relative;
  height: 700px;
  border-radius: 8px;
  overflow: hidden;
  background: #f7f9fc;
}

.amap-container {
  min-height: 700px;
}

.amap-disabled {
  height: 100%;
  min-height: 700px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f7f9fc;
}

.map-background-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.route-svg {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: block;
}

.has-map-image .route-svg {
  background: rgba(255, 255, 255, 0.08);
}

.map-label {
  font-size: 22px;
  fill: #303133;
  paint-order: stroke;
  stroke: #ffffff;
  stroke-width: 5px;
  stroke-linejoin: round;
}

.history-drawer {
  padding: 0 4px 24px;
}

.history-item {
  padding: 16px 0;
  border-bottom: 1px solid #edf1f7;
}

.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  h4 {
    margin: 0;
    color: #263243;
    font-size: 16px;
  }

  p {
    margin: 6px 0 0;
    color: #9aa3b2;
    font-size: 12px;
  }
}

.history-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
}

.history-source {
  margin-top: 8px;
  color: #4f6f9f;
  font-size: 13px;
}

.history-points {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;

  span {
    padding: 3px 8px;
    color: #4f6f9f;
    background: #eef4ff;
    border-radius: 4px;
    font-size: 12px;
  }
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
