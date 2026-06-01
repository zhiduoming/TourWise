<template>
  <div class="search-container">
    <AppHeader />
    <el-card class="search-card">
      <h3 class="card-title">
        <el-icon><Search /></el-icon>
        景点查询
      </h3>
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索景点名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="省份">
          <el-input
            v-model="searchForm.province"
            placeholder="如 北京、浙江"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="城市">
          <el-input
            v-model="searchForm.city"
            placeholder="如 北京、上海"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="searchForm.tag" placeholder="全部" clearable>
            <el-option
              v-for="tag in searchTags"
              :key="tag.value"
              :label="tag.label"
              :value="tag.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简称">
          <el-input
            v-model="searchForm.shortName"
            placeholder="如 北邮、北航"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="result-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><List /></el-icon>
            查询结果
          </h3>
          <span class="result-count">共 {{ total }} 个景点</span>
        </div>
      </template>

      <el-row v-loading="loading" :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in facilityList" :key="item.id">
          <el-card class="facility-card" shadow="hover">
            <div class="facility-header">
              <el-icon class="facility-icon" :size="32">
                <component :is="getIconByType(item.type)" />
              </el-icon>
              <div class="facility-info">
                <h4 class="facility-name">{{ item.name }}</h4>
                <el-tag size="small" :type="getTypeTag(item.type)">
                  {{ item.typeName }}
                </el-tag>
              </div>
            </div>
            <p class="facility-desc">{{ item.description }}</p>
            <div class="facility-meta">
              <span class="facility-location">
                <el-icon><Location /></el-icon>
                {{ item.city || item.placeGroupName || item.location || item.address || '暂无位置' }}
              </span>
              <span class="facility-distance">
                <el-icon><Star /></el-icon>
                {{ item.rating || item.score || '暂无评分' }}
              </span>
            </div>
            <div class="facility-actions">
              <el-button size="small" @click="handleViewDetail(item)">
                详情
              </el-button>
              <el-button size="small" type="primary" @click="handleNavigate(item)">
                路线规划
              </el-button>
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
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">{{ currentDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="标签">{{ currentDetail.typeName }}</el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">
            <el-icon><Location /></el-icon>
            {{ currentDetail.location }}
          </el-descriptions-item>
          <el-descriptions-item label="开放时间" :span="2">
            {{ currentDetail.openTime }} - {{ currentDetail.closeTime }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话" :span="2">
            {{ currentDetail.phone || '暂无' }}
          </el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">
            {{ currentDetail.description }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { searchFacilities, getSearchTags } from '@/api/search'

const router = useRouter()
const route = useRoute()

const searchForm = reactive({
  keyword: '',
  province: '',
  city: '',
  tag: '',
  shortName: ''
})

const loading = ref(false)
const facilityList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const detailVisible = ref(false)
const currentDetail = ref(null)
const searchTags = ref([
  { value: 'scenery', label: '风景' },
  { value: 'culture', label: '文化' },
  { value: 'museum', label: '博物馆' },
  { value: 'university', label: '高校' }
])

const getIconByType = (type) => {
  const iconMap = {
    teaching: 'OfficeBuilding',
    library: 'Reading',
    cafeteria: 'Food',
    dormitory: 'House',
    sports: 'Basketball',
    shop: 'ShoppingCart',
    hospital: 'FirstAidKit',
    office: 'Monitor'
  }
  return iconMap[type] || 'Location'
}

const getTypeTag = (type) => {
  const tagMap = {
    teaching: '',
    library: 'success',
    cafeteria: 'warning',
    dormitory: 'info',
    sports: 'primary',
    shop: 'success',
    hospital: 'danger',
    office: ''
  }
  return tagMap[type] || ''
}

const handleSearch = async () => {
  loading.value = true
  try {
    const res = await searchFacilities({
      ...searchForm,
      spotOnly: true,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    facilityList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('搜索失败:', error)
    // 模拟数据
    facilityList.value = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      name: `景点${i + 1}`,
      type: ['university', 'natural', 'museum'][i % 3],
      typeName: ['高校', '自然风景', '博物馆'][i % 3],
      description: '这是景点描述信息',
      location: 'XX 市',
      distance: Math.floor(Math.random() * 1000)
    }))
    total.value = 50
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.province = ''
  searchForm.city = ''
  searchForm.tag = ''
  searchForm.shortName = ''
  currentPage.value = 1
  handleSearch()
}

const handleViewDetail = (item) => {
  console.log('点击详情，item:', item)
  if (!item.id) {
    ElMessage.warning('缺少景点 ID，无法跳转')
    return
  }
  // 跳转到详情页
  ElMessage.success(`完成跳转${item.id}`)
  router.push(`/spot/${item.id}`)
}

const handleNavigate = (item) => {
  router.push({
    path: '/route-plan',
    query: {
      spotId: item.id,
      placeGroupId: item.placeGroupId,
      scopeName: item.name,
      endName: item.name
    }
  })
}

onMounted(async () => {
  if (route.query.keyword) {
    searchForm.keyword = String(route.query.keyword)
  }
  if (route.query.province) {
    searchForm.province = String(route.query.province)
  }
  if (route.query.city) {
    searchForm.city = String(route.query.city)
  }
  if (route.query.tag) {
    searchForm.tag = String(route.query.tag)
  }
  if (route.query.shortName) {
    searchForm.shortName = String(route.query.shortName)
  }
  try {
    const res = await getSearchTags()
    searchTags.value = res.data || searchTags.value
  } catch (error) {
    console.error('获取景点标签失败:', error)
  }
  handleSearch()
})
</script>

<style lang="scss" scoped>
.search-container {
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

.search-form {
  .el-form-item {
    margin-bottom: 0;
    margin-right: 24px;
  }
}

.result-card {
  margin-top: 20px;
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

.facility-card {
  margin-bottom: 20px;
}

.facility-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.facility-icon {
  color: #409eff;
}

.facility-info {
  flex: 1;
}

.facility-name {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
}

.facility-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.facility-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.facility-location, .facility-distance {
  display: flex;
  align-items: center;
  gap: 4px;
}

.facility-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.detail-content {
  .el-descriptions {
    margin-top: 16px;
  }
}
</style>
