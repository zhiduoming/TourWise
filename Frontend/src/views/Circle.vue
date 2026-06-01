<template>
  <div class="circle-container" v-loading="loading">
    <AppHeader />

    <div class="content">
      <!-- 上部分：我加入的圈子 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <h2 class="section-title">
              <el-icon><Star /></el-icon>
              我加入的圈子
            </h2>
          </div>
        </template>

        <div class="circle-list">
          <el-empty v-if="joinedCircles.length === 0" description="你还没有加入任何圈子~" />

          <el-row v-else :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="circle in joinedCircles" :key="circle.id">
              <el-card class="circle-item" shadow="hover" @click="enterCircle(circle.id)">
                <div class="circle-cover" :style="circleCoverStyle(circle)">
                  <img
                    v-if="hasCircleCover(circle)"
                    :src="circle.cover"
                    :alt="circle.name"
                    @error="handleCircleCoverError(circle.cover)"
                  />
                  <div v-else class="circle-fallback">
                    <span>{{ circleCoverText(circle) }}</span>
                  </div>
                  <AdminImageUpload
                    class="cover-upload"
                    target-type="circle"
                    :target-id="circle.id"
                    label="换封面"
                    @success="url => handleCircleCoverUploaded(circle, url)"
                  />
                  <div class="circle-overlay">
                    <el-button size="small" type="primary" plain @click.stop="enterCircle(circle.id)">进入圈子</el-button>
                  </div>
                </div>
                <div class="circle-info">
                  <h4 class="circle-name">{{ circle.name }}</h4>
                  <p class="circle-desc">{{ circle.description }}</p>
                  <div class="circle-meta">
                    <span class="circle-members">
                      <el-icon><User /></el-icon>
                      {{ circle.members || 0 }}
                    </span>
                    <span class="circle-posts">
                      <el-icon><Document /></el-icon>
                      {{ circle.posts || 0 }}
                    </span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 下部分：其他圈子 -->
      <el-card class="section-card" shadow="hover" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <h2 class="section-title">
              <el-icon><Search /></el-icon>
              其他圈子
            </h2>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索圈子..."
              prefix-icon="Search"
              style="width: 200px"
              clearable
              @clear="handleSearch"
              @input="handleSearch"
            />
          </div>
        </template>

        <div class="circle-list">
          <el-empty v-if="otherCircles.length === 0" description="暂无其他圈子" />

          <el-row v-else :gutter="20">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="circle in otherCircles" :key="circle.id">
              <el-card class="circle-item" shadow="hover">
                <div class="circle-cover" :style="circleCoverStyle(circle)">
                  <img
                    v-if="hasCircleCover(circle)"
                    :src="circle.cover"
                    :alt="circle.name"
                    @error="handleCircleCoverError(circle.cover)"
                  />
                  <div v-else class="circle-fallback">
                    <span>{{ circleCoverText(circle) }}</span>
                  </div>
                  <AdminImageUpload
                    class="cover-upload"
                    target-type="circle"
                    :target-id="circle.id"
                    label="换封面"
                    @success="url => handleCircleCoverUploaded(circle, url)"
                  />
                  <div class="circle-overlay">
                    <el-button size="small" type="primary" plain @click.stop="handleJoinCircle(circle)">加入圈子</el-button>
                  </div>
                </div>
                <div class="circle-info">
                  <h4 class="circle-name">{{ circle.name }}</h4>
                  <p class="circle-desc">{{ circle.description }}</p>
                  <div class="circle-meta">
                    <span class="circle-members">
                      <el-icon><User /></el-icon>
                      {{ circle.members || 0 }}
                    </span>
                    <span class="circle-posts">
                      <el-icon><Document /></el-icon>
                      {{ circle.posts || 0 }}
                    </span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 底部创建圈子按钮 -->
    <div class="bottom-action">
      <el-button type="primary" size="large" round @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建圈子
      </el-button>
    </div>

    <!-- 创建圈子对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建新圈子" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="圈子名称">
          <el-input v-model="createForm.name" placeholder="请输入圈子名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="圈子描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="简单介绍一下这个圈子..."
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ scene: 'circle-cover' }"
            :show-file-list="false"
            :on-success="handleCoverUploadSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="createForm.cover" :src="createForm.cover" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCircle">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { Star, Search, User, Document, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCircleList, createCircle, joinCircle } from '@/api/circle'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const searchKeyword = ref('')
const showCreateDialog = ref(false)

const joinedCircles = ref([])
const otherCircles = ref([])
const failedCoverUrls = ref(new Set())

const createForm = reactive({
  name: '',
  description: '',
  cover: ''
})
const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/upload`
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const handleCoverUploadSuccess = (response) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '封面上传失败')
    return
  }
  createForm.cover = response.data?.url || ''
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

const hasCircleCover = (circle) => {
  const cover = circle?.cover
  return !!cover && !failedCoverUrls.value.has(cover)
}

const handleCircleCoverError = (cover) => {
  if (!cover) return
  failedCoverUrls.value = new Set([...failedCoverUrls.value, cover])
}

const circleCoverText = (circle) => {
  return (circle?.name || '圈子').replace(/圈$/, '').slice(0, 4)
}

const circleCoverStyle = (circle) => {
  const themes = [
    'linear-gradient(135deg, #2563eb 0%, #14b8a6 100%)',
    'linear-gradient(135deg, #7c3aed 0%, #ec4899 100%)',
    'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
    'linear-gradient(135deg, #0f766e 0%, #84cc16 100%)'
  ]
  const index = Number(circle?.id || 0) % themes.length
  return { background: themes[index] }
}

const handleCircleCoverUploaded = (circle, url) => {
  if (!url) return
  circle.cover = url
}

const handleCreateCircle = async () => {
  // 验证名称
  if (!createForm.name || !createForm.name.trim()) {
    ElMessage.warning('请输入圈子名称')
    return
  }
  
  const nameLength = createForm.name.trim().length
  if (nameLength > 50) {
    ElMessage.warning('圈子名称不能超过 50 个字符')
    return
  }
  
  // 验证描述
  if (!createForm.description || !createForm.description.trim()) {
    ElMessage.warning('请输入圈子描述')
    return
  }
  
  try {
    await createCircle({
      name: createForm.name.trim(),
      description: createForm.description.trim(),
      cover: createForm.cover
    })
    ElMessage.success('圈子创建成功')
    showCreateDialog.value = false
    // 重置表单
    createForm.name = ''
    createForm.description = ''
    createForm.cover = ''
    // 重新加载列表
    loadCircles()
  } catch (error) {
    console.error('创建圈子失败:', error)
    // 错误信息已在 API 拦截器中处理
  }
}

const handleJoinCircle = async (circle) => {
  try {
    await joinCircle(circle.id)
    ElMessage.success('加入圈子成功')
    loadCircles()
  } catch (error) {
    console.error('加入圈子失败:', error)
  }
}

const enterCircle = (circleId) => {
  router.push(`/circle/${circleId}`)
}

const loadCircles = async () => {
  try {
    loading.value = true
    const res = await getCircleList(searchKeyword.value)
    joinedCircles.value = res.data.joinedCircles || []
    otherCircles.value = res.data.otherCircles || []
  } catch (error) {
    console.error('获取圈子列表失败:', error)
    ElMessage.error('加载失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 搜索防抖
let searchTimer = null
const handleSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadCircles()
  }, 500)
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后访问圈子')
    router.push({ name: 'Login', query: { redirect: '/circle' } })
    return
  }

  loadCircles()
})
</script>

<style lang="scss" scoped>
.circle-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 80px);
  position: relative;
}

.content {
  margin-bottom: 80px;
}

.section-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .section-title {
    font-size: 20px;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.circle-list {
  min-height: 200px;
}

.circle-item {
  margin-bottom: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);

    .circle-cover {
      .circle-overlay {
        opacity: 1;
      }
    }
  }

  .circle-cover {
    height: 160px;
    background-color: #f0f2f5;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .circle-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: 0;
      background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.18), transparent 42%),
        radial-gradient(circle at 78% 20%, rgba(255, 255, 255, 0.28), transparent 22%);

      span {
        padding: 8px 14px;
        border-radius: 8px;
        background: rgba(15, 23, 42, 0.28);
        backdrop-filter: blur(8px);
      }
    }

    .circle-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s;
    }

    .cover-upload {
      position: absolute;
      right: 10px;
      top: 10px;
      z-index: 2;
    }
  }

  .circle-info {
    padding: 16px;

    .circle-name {
      font-size: 16px;
      color: #303133;
      margin: 0 0 8px 0;
      font-weight: 600;
    }

    .circle-desc {
      color: #909399;
      font-size: 14px;
      margin: 0 0 12px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .circle-meta {
      display: flex;
      gap: 16px;
      color: #909399;
      font-size: 13px;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.bottom-action {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;

  .el-button {
    padding: 16px 40px;
    font-size: 18px;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }
}

.cover-uploader {
  width: 100%;

  .cover-preview {
    width: 100%;
    max-height: 200px;
    object-fit: cover;
    border-radius: 8px;
  }

  .cover-uploader-icon {
    font-size: 48px;
    color: #8c939d;
    width: 100%;
    height: 150px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
  }
}

.content {
  margin-bottom: 80px;
}

.section-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .section-title {
    font-size: 20px;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.circle-list {
  min-height: 200px;
}

.circle-item {
  margin-bottom: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);

    .circle-cover {
      .circle-overlay {
        opacity: 1;
      }
    }
  }

  .circle-cover {
    height: 160px;
    background-color: #f0f2f5;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .circle-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: 0;
      background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.18), transparent 42%),
        radial-gradient(circle at 78% 20%, rgba(255, 255, 255, 0.28), transparent 22%);

      span {
        padding: 8px 14px;
        border-radius: 8px;
        background: rgba(15, 23, 42, 0.28);
        backdrop-filter: blur(8px);
      }
    }

    .circle-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s;
    }

    .cover-upload {
      position: absolute;
      right: 10px;
      top: 10px;
      z-index: 2;
    }
  }

  .circle-info {
    padding: 16px;

    .circle-name {
      font-size: 16px;
      color: #303133;
      margin: 0 0 8px 0;
      font-weight: 600;
    }

    .circle-desc {
      color: #909399;
      font-size: 14px;
      margin: 0 0 12px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .circle-meta {
      display: flex;
      gap: 16px;
      color: #909399;
      font-size: 13px;

      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.bottom-action {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;

  .el-button {
    padding: 16px 40px;
    font-size: 18px;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }
}

.cover-uploader {
  width: 100%;

  .cover-preview {
    width: 100%;
    max-height: 200px;
    object-fit: cover;
    border-radius: 8px;
  }

  .cover-uploader-icon {
    font-size: 48px;
    color: #8c939d;
    width: 100%;
    height: 150px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px dashed #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
  }
}
</style>
