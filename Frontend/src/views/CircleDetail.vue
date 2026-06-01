<template>
  <div class="circle-detail-container">
    <AppHeader />

    <div class="content" v-loading="loading">
      <!-- 圈子头部信息 -->
      <el-card class="circle-header" shadow="hover" v-if="circle">
        <div class="header-content">
          <div class="circle-cover" :style="{ backgroundImage: `url(${circle.cover || '/circle-placeholder.jpg'})` }">
            <AdminImageUpload
              class="circle-cover-upload"
              target-type="circle"
              :target-id="circle.id"
              label="更换封面"
              @success="handleCircleCoverUploaded"
            />
            <div class="cover-overlay"></div>
          </div>
          <div class="circle-info">
            <h1 class="circle-name">{{ circle.name }}</h1>
            <p class="circle-description">{{ circle.description }}</p>
            <div class="circle-meta">
              <span><el-icon><User /></el-icon> {{ circle.members }} 成员</span>
              <span><el-icon><Document /></el-icon> {{ circle.logs }} 日志</span>
              <span><el-icon><Calendar /></el-icon> 创建于 {{ formatDate(circle.created_at) }}</span>
            </div>
            <div class="circle-actions">
              <el-button 
                v-if="!circle.is_member" 
                type="primary" 
                size="large"
                @click="handleJoinCircle"
              >
                <el-icon><Plus /></el-icon> 加入圈子
              </el-button>
              <el-button 
                v-else 
                type="danger" 
                size="large"
                plain
                @click="handleLeaveCircle"
              >
                <el-icon><Close /></el-icon> 退出圈子
              </el-button>
              <el-button size="large" @click="showLogDialog = true">
                <el-icon><Edit /></el-icon> 写日志
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 成员列表 -->
      <el-card class="section-card" shadow="hover" v-if="circle && circle.member_list?.length">
        <template #header>
          <div class="card-header">
            <h2 class="section-title">
              <el-icon><User /></el-icon>
              圈子成员
            </h2>
          </div>
        </template>
        <div class="member-list">
          <el-tag 
            v-for="member in circle.member_list" 
            :key="member.id"
            :type="member.role === 3 ? 'danger' : member.role === 2 ? 'warning' : 'info'"
            size="large"
            effect="plain"
          >
            {{ member.username }}
            <span class="role-label" v-if="member.role === 3">圈主</span>
            <span class="role-label" v-else-if="member.role === 2">管理</span>
          </el-tag>
        </div>
      </el-card>

      <!-- 日志列表 -->
      <el-card class="section-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <h2 class="section-title">
              <el-icon><Document /></el-icon>
              圈子日志
            </h2>
          </div>
        </template>

        <div class="log-list">
          <el-empty v-if="!logs.length" description="暂无日志，快来发第一篇吧~" />
          
          <div v-else>
            <div 
              class="log-item" 
              v-for="log in logs" 
              :key="log.id"
              @click="viewLogDetail(log)"
            >
              <div class="log-header">
                <div class="log-author">
                  <el-avatar :size="40" :icon="User" />
                  <div class="author-info">
                    <span class="author-name">{{ log.username }}</span>
                    <span class="log-time">{{ formatDate(log.created_at) }}</span>
                  </div>
                </div>
                <el-tag v-if="log.is_top" type="danger" size="small">置顶</el-tag>
                <el-button
                  v-if="canDeleteLog(log)"
                  size="small"
                  type="danger"
                  plain
                  @click.stop="handleDeleteLog(log)"
                >
                  删除
                </el-button>
              </div>
              
              <h3 class="log-title">{{ log.title || '无标题' }}</h3>
              <p class="log-content">{{ truncateContent(log.content) }}</p>

              <div
                v-if="log.itineraryPlanId"
                class="itinerary-card"
                @click.stop="openItineraryPlan(log)"
              >
                <div>
                  <span class="itinerary-label">行程计划</span>
                  <h4>{{ log.itineraryPlanTitle || '旅行计划' }}</h4>
                  <p>{{ log.itineraryPlanSummary || '打开查看完整行程安排和路线规划。' }}</p>
                </div>
                <div class="itinerary-meta">
                  <span>{{ log.itineraryPlanCity || '目的地' }}</span>
                  <span>{{ durationText(log.itineraryPlanDuration) }}</span>
                  <span>{{ log.itineraryPlanSpotCount || 0 }} 个景点</span>
                  <el-button size="small" type="primary" plain @click.stop="copySharedPlan(log)">
                    复制行程
                  </el-button>
                </div>
              </div>
              
              <div class="log-images" v-if="log.images?.length">
                <el-image
                  v-for="(img, idx) in log.images"
                  :key="`${log.id}-${idx}`"
                  :src="img"
                  class="log-image"
                  fit="cover"
                  :preview-src-list="log.images"
                  :initial-index="idx"
                  preview-teleported
                  @click.stop
                />
              </div>

              <div class="log-footer">
                <span
                  class="log-stat like-stat"
                  :class="{ liked: isLogLiked(log) }"
                  @click.stop="handleLike(log)"
                >
                  <HeartIcon class="like-icon" /> {{ log.like_count }}
                </span>
                <span class="log-stat" @click.stop="toggleComments(log)">
                  <el-icon><ChatDotRound /></el-icon> {{ log.comment_count }}
                </span>
                <span class="log-stat">
                  <el-icon><View /></el-icon> {{ log.view_count }}
                </span>
              </div>

              <!-- 评论区域 -->
              <div class="log-comments" v-if="log.showComments">
                <div class="comment-list" v-loading="log.loadingComments">
                  <div 
                    class="comment-item" 
                    v-for="comment in log.comments" 
                    :key="comment.id"
                  >
                    <div class="comment-header">
                      <span class="comment-author">{{ comment.username }}</span>
                      <span class="comment-time">{{ formatDate(comment.created_at) }}</span>
                    </div>
                    <p class="comment-content">{{ comment.content }}</p>
                    <div class="comment-reply" @click.stop="showReplyInput(comment)">
                      <el-icon><ChatLineRound /></el-icon> 回复
                    </div>
                    <!-- 回复输入框 -->
                    <el-input
                      v-if="log.replyingTo === comment.id"
                      v-model="log.replyContent"
                      placeholder="写下你的回复..."
                      size="small"
                      @blur="cancelReply(log)"
                      @keyup.enter="handleReply(log, comment.id)"
                    >
                      <template #append>
                        <el-button @click="handleReply(log, comment.id)">发送</el-button>
                      </template>
                    </el-input>
                    <!-- 回复列表 -->
                    <div class="reply-list" v-if="comment.replies?.length">
                      <div 
                        class="reply-item" 
                        v-for="reply in comment.replies" 
                        :key="reply.id"
                      >
                        <span class="reply-author">{{ reply.username }}:</span>
                        <span class="reply-content">{{ reply.content }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 发表评论 -->
                <div class="comment-input">
                  <el-input
                    v-model="log.newComment"
                    placeholder="写下你的评论..."
                    size="small"
                    @keyup.enter="handleComment(log)"
                  >
                    <template #append>
                      <el-button @click="handleComment(log)">评论</el-button>
                    </template>
                  </el-input>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination" v-if="total > pageSize">
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadLogs"
              />
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 写日志对话框 -->
    <el-dialog v-model="showLogDialog" title="发布日志" width="600px">
      <el-form :model="logForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="logForm.title" placeholder="可选" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="logForm.content"
            type="textarea"
            :rows="6"
            placeholder="分享你的想法..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关联景点">
          <el-select 
            v-model="logForm.spotId" 
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
        <el-form-item label="评分" v-if="logForm.spotId">
          <el-rate v-model="logForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            v-model:file-list="logForm.fileList"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="{ scene: 'circle-log' }"
            :on-success="handleImageUploadSuccess"
            :on-remove="handleImageRemove"
            :before-upload="beforeImageUpload"
            multiple
            :limit="9"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLogDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateLog">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import HeartIcon from '@/components/HeartIcon.vue'
import { 
  User, Document, Calendar, Plus, Close, Edit, 
  ChatDotRound, View, ChatLineRound
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCircleDetail, joinCircle, leaveCircle, getCircleLogs, createLog, toggleLike, getComments, createComment } from '@/api/circle'
import { deleteLog } from '@/api/log'
import { copyItineraryPlan } from '@/api/itinerary'
import { searchFacilities } from '@/api/search'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const circle = ref(null)
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

const showLogDialog = ref(false)
const spotList = ref([])
const loadingSpots = ref(false)
const logForm = reactive({
  title: '',
  content: '',
  spotId: null,
  rating: 0,
  fileList: [],
  images: []
})
const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/upload`
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const handleImageUploadSuccess = (response, file) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '图片上传失败')
    return
  }
  const url = response.data?.url
  file.url = url
  if (url && !logForm.images.includes(url)) {
    logForm.images.push(url)
  }
}

const handleImageRemove = (file) => {
  const url = file.response?.data?.url || file.url
  logForm.images = logForm.images.filter(item => item !== url)
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

const handleCreateLog = async () => {
  if (!logForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  try {
    await createLog(route.params.id, {
      title: logForm.title,
      content: logForm.content,
      spotId: logForm.spotId,
      rating: logForm.rating,
      images: logForm.images
    })
    ElMessage.success('发布日志成功')
    showLogDialog.value = false
    logForm.title = ''
    logForm.content = ''
    logForm.spotId = null
    logForm.rating = 0
    logForm.images = []
    logForm.fileList = []
    loadLogs()
    loadCircleDetail()
  } catch (error) {
    console.error('发布日志失败:', error)
  }
}

const canDeleteLog = (log) => {
  return isAdmin.value || (currentUserId.value && Number(log.user_id || log.userId) === Number(currentUserId.value))
}

const handleDeleteLog = async (log) => {
  try {
    await ElMessageBox.confirm('确定删除这篇日志吗？删除后景点详情页、圈子和个人主页都会同步消失。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteLog(log.id)
    ElMessage.success('删除成功')
    await loadLogs(currentPage.value)
    await loadCircleDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除日志失败:', error)
    }
  }
}

const isLogLiked = (log) => {
  return Number(log?.is_liked ?? log?.isLiked ?? 0) === 1
}

const handleCircleCoverUploaded = (url) => {
  if (url && circle.value) {
    circle.value.cover = url
  }
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

const loadCircleDetail = async () => {
  try {
    loading.value = true
    const res = await getCircleDetail(route.params.id)
    circle.value = res.data
  } catch (error) {
    console.error('获取圈子详情失败:', error)
    ElMessage.error('圈子不存在或已被删除')
    router.push('/circle')
  } finally {
    loading.value = false
  }
}

const loadLogs = async (page = 1) => {
  try {
    const res = await getCircleLogs(route.params.id, page, pageSize.value)
    logs.value = res.data.list.map(log => ({
      ...log,
      showComments: false,
      loadingComments: false,
      comments: [],
      replyingTo: null,
      replyContent: '',
      newComment: ''
    }))
    total.value = res.data.total
    currentPage.value = page
  } catch (error) {
    console.error('获取日志失败:', error)
  }
}

// 点赞日志
const handleLike = async (log) => {
  try {
    const res = await toggleLike(log.id)
    if (res.data?.liked) {
      log.like_count++
      log.is_liked = 1
      log.isLiked = 1
      ElMessage.success('点赞成功')
    } else {
      log.like_count = Math.max(0, log.like_count - 1)
      log.is_liked = 0
      log.isLiked = 0
      ElMessage.info('已取消点赞')
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 切换评论显示
const toggleComments = async (log) => {
  if (log.showComments) {
    log.showComments = false
    return
  }

  log.showComments = true
  if (log.comments.length === 0 && !log.loadingComments) {
    log.loadingComments = true
    try {
      const res = await getComments(log.id)
      log.comments = res.data.list
    } catch (error) {
      console.error('获取评论失败:', error)
    } finally {
      log.loadingComments = false
    }
  }
}

// 发表评论
const handleComment = async (log) => {
  if (!log.newComment || !log.newComment.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    await createComment(log.id, {
      content: log.newComment.trim(),
      parentId: 0
    })
    ElMessage.success('评论成功')
    log.newComment = ''
    // 重新加载评论
    const res = await getComments(log.id)
    log.comments = res.data.list
    log.comment_count++
  } catch (error) {
    console.error('评论失败:', error)
  }
}

// 显示回复输入框
const showReplyInput = (comment) => {
  // 找到对应的日志
  const log = logs.value.find(l => l.comments?.includes(comment))
  if (log) {
    log.replyingTo = comment.id
    log.replyContent = ''
  }
}

// 取消回复
const cancelReply = (log) => {
  log.replyingTo = null
  log.replyContent = ''
}

// 发表回复
const handleReply = async (log, parentId) => {
  if (!log.replyContent || !log.replyContent.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    await createComment(log.id, {
      content: log.replyContent.trim(),
      parentId: parentId
    })
    ElMessage.success('回复成功')
    log.replyContent = ''
    log.replyingTo = null
    // 重新加载评论
    const res = await getComments(log.id)
    log.comments = res.data.list
  } catch (error) {
    console.error('回复失败:', error)
  }
}

const handleJoinCircle = async () => {
  try {
    await joinCircle(route.params.id)
    ElMessage.success('加入圈子成功')
    loadCircleDetail()
  } catch (error) {
    console.error('加入圈子失败:', error)
  }
}

const handleLeaveCircle = async () => {
  try {
    await ElMessageBox.confirm('确定要退出这个圈子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await leaveCircle(route.params.id)
    ElMessage.success('退出圈子成功')
    loadCircleDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出圈子失败:', error)
    }
  }
}

const viewLogDetail = (log) => {
  if (log?.id) {
    router.push(`/log/${log.id}`)
  }
}

const openItineraryPlan = (log) => {
  if (log?.itineraryPlanId) {
    router.push({ path: '/itinerary', query: { sharedPlanId: log.itineraryPlanId } })
  }
}

const copySharedPlan = async (log) => {
  if (!log?.itineraryPlanId) {
    return
  }
  try {
    const res = await copyItineraryPlan(log.itineraryPlanId)
    ElMessage.success('已复制到我的行程')
    router.push({ path: '/itinerary', query: { planId: res.data?.id } })
  } catch (error) {
    console.error('复制行程失败:', error)
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

const truncateContent = (content, maxLength = 200) => {
  if (!content) return ''
  if (content.length <= maxLength) return content
  return content.substring(0, maxLength) + '...'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.getUserInfoAction().catch(() => userStore.clearLoginState())
  }
  loadCircleDetail()
  loadLogs()
})

// 监听对话框打开，加载景点列表
watch(showLogDialog, async (newVal) => {
  if (newVal && spotList.value.length === 0) {
    await loadSpotList()
  }
})
</script>

<style lang="scss" scoped>
.circle-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 80px);
}

.content {
  margin-bottom: 40px;
}

.circle-header {
  margin-bottom: 20px;

  .header-content {
    display: flex;
    gap: 20px;
    align-items: stretch;
  }

  .circle-cover {
    width: 240px;
    height: 240px;
    flex-shrink: 0;
    background-size: cover;
    background-position: center;
    background-color: #f0f2f5;
    border-radius: 8px;
    position: relative;
    overflow: hidden;

    .circle-cover-upload {
      position: absolute;
      right: 10px;
      top: 10px;
      z-index: 2;
    }

    .cover-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(to bottom, transparent, rgba(0,0,0,0.1));
    }
  }

  .circle-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 12px;

    .circle-name {
      font-size: 28px;
      color: #303133;
      margin: 0;
      font-weight: 600;
    }

    .circle-description {
      color: #606266;
      font-size: 16px;
      margin: 0;
      line-height: 1.6;
    }

    .circle-meta {
      display: flex;
      gap: 20px;
      color: #909399;
      font-size: 14px;

      span {
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }

    .circle-actions {
      display: flex;
      gap: 12px;
      margin-top: 8px;
    }
  }
}

.section-card {
  margin-bottom: 20px;

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

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .el-tag {
    padding: 8px 16px;
    font-size: 14px;

    .role-label {
      margin-left: 6px;
      font-size: 12px;
      opacity: 0.8;
    }
  }
}

.log-list {
  min-height: 200px;
}

.log-item {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background-color: #f5f7fa;
  }

  .log-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .log-author {
      display: flex;
      align-items: center;
      gap: 12px;

      .author-info {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .author-name {
          font-size: 15px;
          color: #303133;
          font-weight: 500;
        }

        .log-time {
          font-size: 13px;
          color: #909399;
        }
      }
    }
  }

  .log-title {
    font-size: 18px;
    color: #303133;
    margin: 0 0 12px 0;
    font-weight: 600;
  }

  .log-content {
    color: #606266;
    font-size: 14px;
    margin: 0 0 16px 0;
    line-height: 1.8;
  }

  .itinerary-card {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 16px;
    padding: 14px;
    margin: 12px 0 16px;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    background: #f8fbff;
    cursor: pointer;

    &:hover {
      border-color: #409eff;
      background: #eef6ff;
    }

    .itinerary-label {
      color: #2563eb;
      font-size: 12px;
      font-weight: 600;
    }

    h4 {
      margin: 4px 0 6px;
      color: #263243;
      font-size: 16px;
    }

    p {
      margin: 0;
      color: #606266;
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .itinerary-meta {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      justify-content: center;
      gap: 6px;
      color: #4f6f9f;
      font-size: 13px;
      white-space: nowrap;
    }
  }

  .log-images {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
    margin-bottom: 16px;

    .log-image {
      width: 100%;
      height: 120px;
      border-radius: 8px;
      cursor: pointer;
      transition: transform 0.3s;

      &:hover {
        transform: scale(1.05);
      }
    }
  }

  .log-footer {
    display: flex;
    gap: 20px;
    color: #909399;
    font-size: 14px;

    .log-stat {
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #409eff;
      }

      &.like-stat:hover,
      &.like-stat.liked {
        color: #f56c6c;
      }

      &.like-stat.liked .like-icon {
        fill: currentColor;
      }

      .like-icon {
        width: 16px;
        height: 16px;
        flex: 0 0 auto;
      }
    }
  }

  .log-comments {
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #f0f0f0;

    .comment-list {
      margin-bottom: 16px;
      max-height: 400px;
      overflow-y: auto;
    }

    .comment-item {
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .comment-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;

        .comment-author {
          font-weight: 500;
          color: #303133;
        }

        .comment-time {
          font-size: 13px;
          color: #909399;
        }
      }

      .comment-content {
        color: #606266;
        margin-bottom: 8px;
        line-height: 1.6;
      }

      .comment-reply {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #409eff;
        font-size: 13px;
        cursor: pointer;

        &:hover {
          color: #66b1ff;
        }
      }

      .reply-list {
        margin-top: 12px;
        padding-left: 16px;
        background-color: #f5f7fa;
        border-radius: 4px;
        padding: 12px;

        .reply-item {
          padding: 8px 0;

          &:not(:last-child) {
            border-bottom: 1px solid #e4e7ed;
          }

          .reply-author {
            font-weight: 500;
            color: #303133;
            margin-right: 8px;
          }

          .reply-content {
            color: #606266;
          }
        }
      }
    }

    .comment-input {
      margin-top: 16px;
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .circle-header {
    .header-content {
      flex-direction: column;
    }

    .circle-cover {
      width: 100%;
      height: 200px;
    }
  }
}
</style>
