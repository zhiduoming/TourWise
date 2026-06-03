<template>
  <div class="spot-foods-page">
    <AppHeader />

    <section class="page-hero">
      <div class="hero-text">
        <span class="eyebrow">Nearby Foods</span>
        <h1>{{ spotName ? `${spotName} · 周边美食` : '周边美食' }}</h1>
        <p>覆盖校区周边餐厅、咖啡、快餐与火锅等多种品类，支持菜系筛选和评分/人气排序。</p>
      </div>
      <el-button size="large" plain @click="goBack">返回景点详情</el-button>
    </section>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="菜系">
          <el-select v-model="filters.cuisine" placeholder="全部菜系" clearable style="width: 160px" @change="resetAndLoad">
            <el-option v-for="opt in cuisineOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="价位">
          <el-select v-model="filters.price" placeholder="全部价位" clearable style="width: 140px" @change="resetAndLoad">
            <el-option label="实惠 (人均≤30)" value="1" />
            <el-option label="适中 (30-80)" value="2" />
            <el-option label="较高 (≥80)" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-radio-group v-model="filters.sort" @change="resetAndLoad">
            <el-radio-button label="score">评分</el-radio-button>
            <el-radio-button label="popular">人气</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetFilters">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row v-loading="loading" :gutter="20" class="food-grid">
      <el-empty v-if="!loading && foods.length === 0" description="没有匹配的美食" style="width: 100%" />
      <el-col v-for="food in foods" :key="food.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="food-card" shadow="hover" @click="viewDetail(food.id)">
          <el-image :src="food.image || '/food-placeholder.jpg'" fit="cover" class="food-cover">
            <template #error>
              <div class="cover-fallback">暂无图片</div>
            </template>
          </el-image>
          <div class="food-body">
            <div class="food-title">
              <h3>{{ food.name }}</h3>
              <el-tag size="small" :type="priceTagType(food.priceLevel)">{{ priceLabel(food.priceLevel) }}</el-tag>
            </div>
            <p class="food-cuisine">{{ food.cuisine_type || food.cuisineName || '餐饮' }}</p>
            <p class="food-address">{{ food.address || '地址未知' }}</p>
            <div class="food-meta">
              <span><el-icon><Star /></el-icon> {{ food.rating || '暂无评分' }}</span>
              <span v-if="food.hotness"><el-icon><View /></el-icon> {{ food.hotness }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      class="pagination"
      @size-change="onPageSizeChange"
      @current-change="loadFoods"
    />
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import { getFoodPagedList } from '@/api/food'
import { Star, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const spotId = computed(() => route.params.id)

const filters = reactive({ cuisine: '', price: '', sort: 'score' })
const foods = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const spotName = ref('')

const cuisineOptions = [
  { label: '中餐厅', value: '050100' },
  { label: '快餐厅', value: '050300' },
  { label: '咖啡厅', value: '050500' },
  { label: '茶餐厅', value: '050600' },
  { label: '火锅店', value: '050200' },
  { label: '甜品店', value: '050900' }
]

const priceLabel = (lvl) => ({ 1: '实惠', 2: '适中', 3: '较高' }[lvl] || '适中')
const priceTagType = (lvl) => ({ 1: 'success', 2: '', 3: 'warning' }[lvl] || '')

const loadFoods = async () => {
  loading.value = true
  try {
    const res = await getFoodPagedList({
      spotId: spotId.value,
      cuisine: filters.cuisine || undefined,
      price: filters.price || undefined,
      sort: filters.sort,
      page: page.value,
      pageSize: pageSize.value
    })
    foods.value = res.data?.list || []
    total.value = res.data?.total || 0
    if (foods.value.length && !spotName.value) {
      spotName.value = foods.value[0].spot_name || ''
    }
  } catch (error) {
    console.error('加载美食列表失败:', error)
    ElMessage.error('加载失败，稍后重试')
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  page.value = 1
  loadFoods()
}

const resetFilters = () => {
  filters.cuisine = ''
  filters.price = ''
  filters.sort = 'score'
  resetAndLoad()
}

const onPageSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
  loadFoods()
}

const viewDetail = (id) => {
  router.push(`/food/${id}`)
}

const goBack = () => {
  router.push(`/spot/${spotId.value}`)
}

onMounted(() => {
  loadFoods()
})
</script>

<style lang="scss" scoped>
.spot-foods-page {
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
  padding: 32px;
  margin-bottom: 20px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #d946ef, #f59e0b);

  h1 { margin: 8px 0; font-size: 32px; }
  p  { margin: 0; color: rgba(255,255,255,0.85); }
}

.eyebrow { font-size: 13px; opacity: 0.85; }

.filter-card { border-radius: 8px; margin-bottom: 20px; }
.filter-form { display: flex; flex-wrap: wrap; gap: 12px; }

.food-grid { row-gap: 18px; margin: 0; }

.food-card {
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.food-card:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(0,0,0,0.08); }

.food-cover { width: 100%; height: 160px; border-radius: 8px; background: #f5f7fa; }

.cover-fallback {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #909399;
}

.food-body { padding: 12px 4px 4px; }

.food-title {
  display: flex; justify-content: space-between; align-items: center; gap: 8px;
  h3 { margin: 0; font-size: 16px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.food-cuisine { margin: 6px 0 4px; color: #606266; font-size: 13px; }
.food-address {
  margin: 0 0 8px;
  color: #909399;
  font-size: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.food-meta {
  display: flex; gap: 12px; color: #909399; font-size: 13px;
  span { display: inline-flex; align-items: center; gap: 4px; }
}

.pagination { justify-content: center; margin-top: 24px; }
</style>
