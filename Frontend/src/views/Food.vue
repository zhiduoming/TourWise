<template>
  <div class="food-container">
    <AppHeader />
    <!-- 美食推荐横幅 -->
    <el-card class="banner-card">
      <div class="banner-content">
        <h1 class="banner-title">
          <el-icon><Food /></el-icon>
          美食推荐
        </h1>
        <p class="banner-subtitle">发现景点周边、商圈和校园附近的美味</p>
      </div>
    </el-card>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="菜系">
          <el-select v-model="filterForm.cuisine" placeholder="全部" clearable>
            <el-option label="川菜" value="sichuan" />
            <el-option label="粤菜" value="cantonese" />
            <el-option label="湘菜" value="hunan" />
            <el-option label="快餐" value="fastfood" />
            <el-option label="小吃" value="snack" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-select v-model="filterForm.price" placeholder="全部" clearable>
            <el-option label="¥ 实惠" value="1" />
            <el-option label="¥¥ 适中" value="2" />
            <el-option label="¥¥¥ 较高" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filterForm.sort" placeholder="默认排序">
            <el-option label="评分最高" value="score" />
            <el-option label="距离最近" value="distance" />
            <el-option label="人气最旺" value="popular" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 美食列表 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in foodList" :key="item.id">
        <el-card class="food-card" shadow="hover">
          <div class="image-admin-shell">
            <el-image
              class="food-image"
              :src="item.image || '/food-placeholder.jpg'"
              fit="cover"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <AdminImageUpload
              class="image-upload-overlay"
              target-type="food"
              :target-id="item.id"
              @success="url => handleFoodImageUploaded(item, url)"
            />
          </div>
          <div class="food-info">
            <h4 class="food-name">{{ item.name }}</h4>
            <div class="food-rating">
              <el-rate v-model="item.score" disabled size="small" />
              <span class="rating-text">{{ item.score }}</span>
            </div>
            <p class="food-desc">{{ item.description }}</p>
            <div class="food-meta">
              <span class="food-price">
                <el-icon><Money /></el-icon>
                {{ '¥'.repeat(item.priceLevel) }}
              </span>
              <span class="food-distance">
                <el-icon><Location /></el-icon>
                {{ item.distance }}m
              </span>
            </div>
            <div class="food-tags">
              <el-tag size="small" v-if="item.cuisine">{{ item.cuisineName }}</el-tag>
              <el-tag size="small" type="success" v-if="item.recommend">推荐</el-tag>
            </div>
            <div class="food-actions">
              <el-button size="small" @click="handleViewDetail(item)">详情</el-button>
              <el-button size="small" type="primary" @click="handleNavigate(item)">导航</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="美食详情" width="700px">
      <div v-if="currentDetail" class="detail-content">
        <div class="image-admin-shell">
          <el-image :src="currentDetail.image || '/food-placeholder.jpg'" fit="cover" class="detail-image" />
          <AdminImageUpload
            class="image-upload-overlay"
            target-type="food"
            :target-id="currentDetail.id"
            @success="url => handleFoodImageUploaded(currentDetail, url)"
          />
        </div>
        <h3>{{ currentDetail.name }}</h3>
        <div class="detail-rating">
          <el-rate v-model="currentDetail.score" disabled />
          <span>{{ currentDetail.score }}分</span>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="菜系">{{ currentDetail.cuisineName }}</el-descriptions-item>
          <el-descriptions-item label="人均">¥{{ currentDetail.avgPrice }}</el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">
            <el-icon><Location /></el-icon>
            {{ currentDetail.address }}
          </el-descriptions-item>
          <el-descriptions-item label="营业时间" :span="2">
            {{ currentDetail.openTime }} - {{ currentDetail.closeTime }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话" :span="2">
            {{ currentDetail.phone }}
          </el-descriptions-item>
        </el-descriptions>
        <h4>用户评价</h4>
        <el-input
          v-model="reviewContent"
          type="textarea"
          :rows="3"
          placeholder="写下你的评价..."
          style="margin-top: 12px"
        />
        <el-button type="primary" style="margin-top: 12px" @click="handleSubmitReview">
          提交评价
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getFoodList, getFoodRecommendations, submitFoodReview } from '@/api/food'
import { ElMessage } from 'element-plus'

const router = useRouter()

const filterForm = reactive({
  cuisine: '',
  price: '',
  sort: 'score'
})

const foodList = ref([])
const detailVisible = ref(false)
const currentDetail = ref(null)
const reviewContent = ref('')

const handleSearch = async () => {
  try {
    const res = await getFoodList(filterForm)
    foodList.value = res.data || []
  } catch (error) {
    console.error('获取美食列表失败:', error)
    // 模拟数据
    foodList.value = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      name: `美食${i + 1}`,
      description: '美味可口，值得一试',
      score: 4 + Math.random(),
      priceLevel: Math.floor(Math.random() * 3) + 1,
      distance: Math.floor(Math.random() * 2000),
      cuisine: ['sichuan', 'cantonese'][i % 2],
      cuisineName: ['川菜', '粤菜'][i % 2],
      recommend: i % 3 === 0
    }))
  }
}

const handleViewDetail = (item) => {
  console.log('点击详情，item:', item)
  if (!item.id) {
    ElMessage.warning('缺少美食 ID，无法跳转')
    return
  }
  router.push({ path: `/food/${item.id}` })
}

const handleNavigate = (item) => {
  ElMessage.success(`开始导航到 ${item.name}`)
}

const handleFoodImageUploaded = (item, imageUrl) => {
  if (imageUrl) {
    item.image = imageUrl
  }
}

const handleSubmitReview = async () => {
  if (!reviewContent.value.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }

  try {
    await submitFoodReview({
      foodId: currentDetail.value.id,
      content: reviewContent.value
    })
    ElMessage.success('评价提交成功')
    reviewContent.value = ''
    detailVisible.value = false
  } catch (error) {
    console.error('提交评价失败:', error)
  }
}

onMounted(() => {
  handleSearch()
})
</script>

<style lang="scss" scoped>
.food-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.banner-card {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  margin-bottom: 20px;
}

.banner-content {
  text-align: center;
  padding: 40px 20px;
}

.banner-title {
  font-size: 36px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.banner-subtitle {
  font-size: 18px;
  opacity: 0.9;
}

.filter-card {
  margin-bottom: 20px;
}

.food-card {
  margin-bottom: 20px;
  
  &:hover {
    .food-image {
      transform: scale(1.05);
    }
  }
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

.food-image {
  width: 100%;
  height: 200px;
  transition: transform 0.3s;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 48px;
}

.food-info {
  padding: 16px;
}

.food-name {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}

.food-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rating-text {
  color: #f7ba2a;
  font-weight: bold;
}

.food-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.food-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}

.food-price, .food-distance {
  display: flex;
  align-items: center;
  gap: 4px;
}

.food-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.food-actions {
  display: flex;
  gap: 8px;
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

  .detail-rating {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  h4 {
    margin-top: 20px;
    margin-bottom: 12px;
    color: #303133;
  }
}
</style>
