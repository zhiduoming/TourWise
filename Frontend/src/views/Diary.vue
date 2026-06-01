<template>
  <div class="diary-container">
    <AppHeader />
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">
            <el-icon><Document /></el-icon>
            写日记
          </h3>
          <el-button type="primary" @click="handleSave">
            <el-icon><Check /></el-icon>
            发布
          </el-button>
        </div>
      </template>

      <el-form :model="diaryForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="diaryForm.title" placeholder="输入日记标题" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="diaryForm.date"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="关联景点">
          <el-select 
            v-model="diaryForm.spotId" 
            placeholder="选择景点（可选）"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="spot in spotList"
              :key="spot.id"
              :label="spot.name"
              :value="spot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="同步圈子">
          <el-select
            v-model="diaryForm.circleId"
            placeholder="选择已加入圈子（可选）"
            filterable
            clearable
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
        <el-form-item label="心情">
          <el-rate v-model="diaryForm.mood" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="diaryForm.content"
            type="textarea"
            :rows="8"
            placeholder="记录今天的经历..."
          />
        </el-form-item>
        <el-form-item label="配图">
          <el-upload
            class="uploader"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ scene: 'log' }"
            list-type="picture-card"
            :file-list="fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-success="handleUploadSuccess"
            :before-upload="beforeImageUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="标签">
          <el-tag
            v-for="tag in diaryForm.tags"
            :key="tag"
            closable
            style="margin-right: 8px"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="inputVisible"
            ref="inputRef"
            v-model="inputValue"
            size="small"
            style="width: 120px"
            @blur="handleInputConfirm"
            @keyup.enter="handleInputConfirm"
          />
          <el-button v-else size="small" @click="showInput">
            + 添加标签
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive, nextTick, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { createLog } from '@/api/log'
import { searchFacilities } from '@/api/search'
import { getCircleList } from '@/api/circle'
import { Plus, Document, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const diaryForm = reactive({
  title: '',
  date: new Date(),
  spotId: null,
  circleId: null,
  mood: 3,
  content: '',
  tags: []
})

const fileList = ref([])
const spotList = ref([])
const joinedCircles = ref([])
const loadingSpots = ref(false)
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref(null)
const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/upload`
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value && !diaryForm.tags.includes(inputValue.value)) {
    diaryForm.tags.push(inputValue.value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

const removeTag = (tag) => {
  diaryForm.tags = diaryForm.tags.filter(t => t !== tag)
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
}

const handleUploadSuccess = (response, file, files) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '图片上传失败')
    return
  }
  file.url = response.data?.url
  fileList.value = files
}

const beforeImageUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    return false
  }
  return true
}

const uploadedImages = () => {
  return fileList.value
    .map(file => file.response?.data?.url || file.url)
    .filter(Boolean)
}

const handleSave = async () => {
  // 验证必填字段
  if (!diaryForm.title || !diaryForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  
  if (!diaryForm.content || !diaryForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  try {
    await createLog({
      title: diaryForm.title.trim(),
      content: diaryForm.content.trim(),
      spotId: diaryForm.spotId,
      circleId: diaryForm.circleId,
      rating: diaryForm.mood,
      images: uploadedImages(),
      tags: diaryForm.tags
    })
    ElMessage.success('发布成功')
    handleReset()
    // 可以跳转到日志列表或个人中心
    setTimeout(() => {
      // router.push('/profile')  // 如果需要跳转
    }, 1000)
  } catch (error) {
    console.error('发布失败:', error)
  }
}

const handleReset = () => {
  diaryForm.title = ''
  diaryForm.date = new Date()
  diaryForm.spotId = null
  diaryForm.circleId = null
  diaryForm.mood = 3
  diaryForm.content = ''
  diaryForm.tags = []
  fileList.value = []
}

// 加载景点列表
const loadSpotList = async () => {
  try {
    loadingSpots.value = true
    const res = await searchFacilities({
      spotOnly: true,
      page: 1,
      pageSize: 100
    })
    spotList.value = res.data?.list || []
  } catch (error) {
    console.error('获取景点列表失败:', error)
  } finally {
    loadingSpots.value = false
  }
}

const loadJoinedCircles = async () => {
  try {
    const res = await getCircleList('')
    joinedCircles.value = res.data?.joinedCircles || []
  } catch (error) {
    console.error('获取已加入圈子失败:', error)
    joinedCircles.value = []
  }
}

onMounted(() => {
  loadSpotList()
  loadJoinedCircles()
})
</script>

<style lang="scss" scoped>
.diary-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: #303133;
}

.uploader {
  width: 100%;
}
</style>
