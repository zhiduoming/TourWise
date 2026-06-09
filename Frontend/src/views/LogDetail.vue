<template>
  <div class="log-detail-page">
    <AppHeader />

    <el-button text class="back-button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="16">
        <el-card class="detail-card" shadow="never" v-loading="loading">
          <el-empty v-if="!loading && !log" description="日志不存在或已被删除" />

          <article v-else-if="log" class="log-article">
            <header class="article-header">
              <div class="author-block" @click="openAuthor">
                <el-avatar :size="46" :src="log.avatar || defaultAvatar" />
                <div>
                  <strong>{{ log.username || '用户' }}</strong>
                  <span>{{ formatDate(log.createdAt || log.created_at) }}</span>
                </div>
              </div>
              <el-tag v-if="log.isTop || log.is_top" type="danger">置顶</el-tag>
            </header>

            <h1>{{ log.title || '无标题日志' }}</h1>
            <p class="location-line" v-if="log.location">
              <el-icon><Location /></el-icon>
              {{ log.location }}
            </p>

            <section v-if="hasRating" class="rating-panel">
              <div>
                <span>综合评分</span>
                <el-rate :model-value="numberValue(log.rating)" disabled allow-half />
              </div>
              <div class="dimension-grid">
                <span v-for="item in dimensionRatings" :key="item.label">
                  {{ item.label }}：{{ item.value || '-' }}
                </span>
              </div>
            </section>

            <p class="content-text">{{ log.content }}</p>

            <div v-if="log.images?.length" class="image-grid">
              <el-image
                v-for="(image, index) in log.images"
                :key="`${log.id}-${index}`"
                :src="image"
                class="log-image"
                fit="cover"
                :preview-src-list="log.images"
                :initial-index="index"
                preview-teleported
              />
            </div>

            <section
              v-if="hasPhoto && (isOwner || animationStatus === 'success')"
              class="animation-panel"
            >
              <header>
                <h3>
                  <el-icon><MagicStick /></el-icon>
                  AIGC 旅游动画
                </h3>
                <el-tag v-if="animationStatus === 'processing'" type="warning">生成中</el-tag>
                <el-tag v-else-if="animationStatus === 'success'" type="success">已生成</el-tag>
                <el-tag v-else-if="animationStatus === 'failed'" type="danger">生成失败</el-tag>
                <el-tag v-else type="info">未生成</el-tag>
              </header>

              <div v-if="animationStatus === 'success' && animationUrl" class="animation-player">
                <video
                  :src="animationUrl"
                  :poster="animationCover || log.images?.[0]"
                  controls
                  playsinline
                  preload="metadata"
                />
              </div>

              <div v-else-if="animationStatus === 'processing'" class="animation-hint">
                <el-icon class="is-loading"><Refresh /></el-icon>
                正在调用智谱 CogVideoX 生成视频，通常需要 30 秒到几分钟，离开页面也不影响生成。
              </div>

              <div v-else-if="animationStatus === 'failed'" class="animation-hint failed">
                上次生成失败：{{ animationError || '未知原因' }}，可重试。
              </div>

              <div v-else-if="isOwner" class="animation-hint">
                把你的日记首张照片用 AIGC 动起来，作为旅行 vlog 片段。
              </div>

              <div v-if="isOwner" class="animation-actions">
                <el-button
                  v-if="animationStatus !== 'success'"
                  type="primary"
                  :icon="MagicStick"
                  :loading="animationSubmitting"
                  :disabled="!canGenerateAnimation"
                  @click="handleGenerateAnimation(false)"
                >
                  {{ animationStatus === 'failed' ? '重新生成' : '生成动画' }}
                </el-button>
                <el-button
                  v-else
                  :icon="Refresh"
                  :loading="animationSubmitting"
                  @click="handleGenerateAnimation(true)"
                >
                  重新生成
                </el-button>
              </div>
            </section>

            <div
              v-if="log.itineraryPlanId"
              class="itinerary-card"
              @click="openItineraryPlan"
            >
              <div>
                <span>关联行程</span>
                <h3>{{ log.itineraryPlanTitle || '旅行计划' }}</h3>
                <p>{{ log.itineraryPlanSummary || '打开查看完整行程安排。' }}</p>
              </div>
              <el-button type="primary" plain>查看行程</el-button>
            </div>

            <div v-if="log.tags?.length" class="tag-row">
              <el-tag v-for="tag in log.tags" :key="tag" size="small">{{ tag }}</el-tag>
            </div>

            <footer class="article-actions">
              <el-button
                :type="isLiked ? 'danger' : 'default'"
                plain
                :loading="likeLoading"
                @click="handleLike"
              >
                <HeartIcon class="like-icon" />
                {{ isLiked ? '已点赞' : '点赞' }} {{ log.likeCount ?? log.like_count ?? 0 }}
              </el-button>
              <el-button plain :icon="Warning" @click="openReport('log', log.id)">
                举报
              </el-button>
              <span>
                <el-icon><ChatDotRound /></el-icon>
                {{ log.commentCount ?? log.comment_count ?? 0 }} 条评论
              </span>
              <span>
                <el-icon><View /></el-icon>
                {{ log.viewCount ?? log.view_count ?? 0 }} 次浏览
              </span>
            </footer>
          </article>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="comment-card" shadow="never">
          <template #header>
            <div class="comment-header">
              <h3>
                <el-icon><ChatDotRound /></el-icon>
                评论
              </h3>
              <el-button text type="primary" @click="loadComments">刷新</el-button>
            </div>
          </template>

          <el-empty v-if="!log" description="日志不存在，无法查看评论" :image-size="90" />

          <template v-else>
          <div class="comment-editor">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="写下你的评论..."
            />
            <el-button type="primary" :loading="commentSubmitting" @click="submitComment">
              发表评论
            </el-button>
          </div>

          <div v-loading="commentLoading" class="comment-list">
            <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="90" />
            <article v-for="comment in comments" v-else :key="comment.id" class="comment-item">
              <div class="comment-author">
                <el-avatar :size="30" :src="comment.avatar || defaultAvatar" />
                <div>
                  <strong>{{ comment.username || '用户' }}</strong>
                  <span>{{ formatDate(comment.createdAt || comment.created_at) }}</span>
                </div>
              </div>
              <p>{{ comment.content }}</p>
              <div class="comment-actions">
                <el-button text type="primary" size="small" @click="startReply(comment)">
                  回复
                </el-button>
                <el-button text type="danger" size="small" @click="openReport('comment', comment.id)">
                  举报
                </el-button>
              </div>
              <div v-if="replyingTo === comment.id" class="reply-editor">
                <el-input v-model="replyContent" size="small" placeholder="写下回复..." />
                <el-button size="small" type="primary" @click="submitReply(comment.id)">发送</el-button>
              </div>
              <div v-if="comment.replies?.length" class="reply-list">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <div>
                    <strong>{{ reply.username || '用户' }}：</strong>
                    <span>{{ reply.content }}</span>
                  </div>
                  <el-button text type="danger" size="small" @click="openReport('comment', reply.id)">
                    举报
                  </el-button>
                </div>
              </div>
            </article>
          </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import HeartIcon from '@/components/HeartIcon.vue'
import {
  createReport,
  getLogDetail,
  queryLogAnimation,
  submitLogAnimation,
  toggleLike
} from '@/api/log'
import { createComment, getComments } from '@/api/circle'
import { ArrowLeft, ChatDotRound, Location, MagicStick, Refresh, View, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const log = ref(null)
const comments = ref([])
const commentLoading = ref(false)
const newComment = ref('')
const commentSubmitting = ref(false)
const likeLoading = ref(false)
const replyingTo = ref(null)
const replyContent = ref('')

const animationSubmitting = ref(false)
const animationPolling = ref(false)
let animationTimer = null

const animationStatus = computed(() => log.value?.animationStatus ?? log.value?.animation_status ?? null)
const animationUrl = computed(() => log.value?.animationUrl ?? log.value?.animation_url ?? '')
const animationCover = computed(() => log.value?.animationCoverUrl ?? log.value?.animation_cover_url ?? '')
const animationError = computed(() => log.value?.animationError ?? log.value?.animation_error ?? '')
const ownerId = computed(() => log.value?.userId ?? log.value?.user_id ?? null)
const isOwner = computed(() => {
  const myId = userStore.userInfo?.id
  return myId && ownerId.value && Number(myId) === Number(ownerId.value)
})
const hasPhoto = computed(() => (log.value?.images?.length || 0) > 0)
const canGenerateAnimation = computed(() =>
  isOwner.value && hasPhoto.value && animationStatus.value !== 'processing'
)

const isLiked = computed(() => Number(log.value?.isLiked ?? log.value?.is_liked ?? 0) === 1)
const hasRating = computed(() => Number(log.value?.rating || 0) > 0 || dimensionRatings.value.some(item => item.value))
const dimensionRatings = computed(() => [
  { label: '景色', value: log.value?.sceneryRating ?? log.value?.scenery_rating },
  { label: '设施', value: log.value?.facilityRating ?? log.value?.facility_rating },
  { label: '服务', value: log.value?.serviceRating ?? log.value?.service_rating },
  { label: '交通', value: log.value?.trafficRating ?? log.value?.traffic_rating },
  { label: '性价比', value: log.value?.valueRating ?? log.value?.value_rating }
])

const loadLog = async () => {
  loading.value = true
  try {
    const res = await getLogDetail(route.params.id)
    log.value = res.data
    if (animationStatus.value === 'processing') {
      startAnimationPolling()
    }
  } catch (error) {
    console.error('加载日志详情失败:', error)
    log.value = null
  } finally {
    loading.value = false
  }
}

const applyAnimationState = (state) => {
  if (!log.value || !state) return
  log.value.animationStatus = state.status ?? state.animation_status
  log.value.animation_status = log.value.animationStatus
  if (state.animation_url !== undefined) {
    log.value.animationUrl = state.animation_url
    log.value.animation_url = state.animation_url
  }
  if (state.animation_cover_url !== undefined) {
    log.value.animationCoverUrl = state.animation_cover_url
    log.value.animation_cover_url = state.animation_cover_url
  }
  if (state.animation_error !== undefined) {
    log.value.animationError = state.animation_error
    log.value.animation_error = state.animation_error
  }
}

const stopAnimationPolling = () => {
  if (animationTimer) {
    clearTimeout(animationTimer)
    animationTimer = null
  }
  animationPolling.value = false
}

const pollAnimationOnce = async () => {
  try {
    const res = await queryLogAnimation(route.params.id)
    applyAnimationState(res.data || {})
    if (animationStatus.value === 'processing') {
      animationTimer = setTimeout(pollAnimationOnce, 6000)
    } else {
      stopAnimationPolling()
      if (animationStatus.value === 'success') {
        ElMessage.success('AIGC 动画生成完成')
      } else if (animationStatus.value === 'failed') {
        ElMessage.error(`动画生成失败：${animationError.value || '未知原因'}`)
      }
    }
  } catch (error) {
    console.error('轮询动画状态失败:', error)
    animationTimer = setTimeout(pollAnimationOnce, 10000)
  }
}

const startAnimationPolling = () => {
  if (animationPolling.value) return
  animationPolling.value = true
  animationTimer = setTimeout(pollAnimationOnce, 4000)
}

const handleGenerateAnimation = async (force = false) => {
  if (!ensureLogin()) return
  if (!hasPhoto.value) {
    ElMessage.warning('请先在日记中上传至少一张照片')
    return
  }
  animationSubmitting.value = true
  try {
    const res = await submitLogAnimation(route.params.id, force)
    applyAnimationState({ status: 'processing', ...(res.data || {}) })
    ElMessage.success('已提交生成任务，请稍候')
    startAnimationPolling()
  } catch (error) {
    console.error('提交动画生成失败:', error)
  } finally {
    animationSubmitting.value = false
  }
}

const loadComments = async () => {
  commentLoading.value = true
  try {
    const res = await getComments(route.params.id)
    comments.value = res.data?.list || []
  } catch (error) {
    console.error('加载评论失败:', error)
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

const ensureLogin = () => {
  if (localStorage.getItem('token')) {
    return true
  }
  ElMessage.warning('请先登录')
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
  return false
}

const handleLike = async () => {
  if (!log.value || !ensureLogin()) return
  likeLoading.value = true
  try {
    const res = await toggleLike(log.value.id)
    const liked = Boolean(res.data?.liked)
    log.value.isLiked = liked ? 1 : 0
    log.value.is_liked = liked ? 1 : 0
    const current = Number(log.value.likeCount ?? log.value.like_count ?? 0)
    log.value.likeCount = liked ? current + 1 : Math.max(0, current - 1)
    log.value.like_count = log.value.likeCount
    ElMessage.success(liked ? '点赞成功' : '已取消点赞')
  } catch (error) {
    console.error('点赞失败:', error)
  } finally {
    likeLoading.value = false
  }
}

const submitComment = async () => {
  if (!ensureLogin()) return
  const content = newComment.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await createComment(route.params.id, { content, parentId: 0 })
    newComment.value = ''
    if (log.value) {
      const current = Number(log.value.commentCount ?? log.value.comment_count ?? 0)
      log.value.commentCount = current + 1
      log.value.comment_count = log.value.commentCount
    }
    await loadComments()
    ElMessage.success('评论成功')
  } catch (error) {
    console.error('评论失败:', error)
  } finally {
    commentSubmitting.value = false
  }
}

const startReply = (comment) => {
  replyingTo.value = comment.id
  replyContent.value = ''
}

const submitReply = async (parentId) => {
  if (!ensureLogin()) return
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await createComment(route.params.id, { content, parentId })
    replyingTo.value = null
    replyContent.value = ''
    await loadComments()
    ElMessage.success('回复成功')
  } catch (error) {
    console.error('回复失败:', error)
  }
}

const openReport = async (targetType, targetId) => {
  if (!ensureLogin()) return
  try {
    const { value } = await ElMessageBox.prompt('请说明举报原因，管理员会在后台审核处理。', '举报内容', {
      confirmButtonText: '提交举报',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：广告、辱骂、人身攻击、虚假内容、无关内容等',
      inputValidator: value => (value || '').trim().length > 0 || '举报原因不能为空'
    })
    const text = value.trim()
    await createReport({
      targetType,
      targetId,
      reason: text.slice(0, 100),
      detail: text.slice(0, 1000)
    })
    ElMessage.success('举报已提交，等待管理员处理')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('提交举报失败:', error)
    }
  }
}

const openAuthor = () => {
  const userId = log.value?.userId || log.value?.user_id
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

const openItineraryPlan = () => {
  if (log.value?.itineraryPlanId) {
    router.push({ path: '/itinerary', query: { sharedPlanId: log.value.itineraryPlanId } })
  }
}

const numberValue = value => Number(value || 0)

const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(async () => {
  await loadLog()
  if (log.value) {
    await loadComments()
  }
})

onBeforeUnmount(() => {
  stopAnimationPolling()
})
</script>

<style lang="scss" scoped>
.log-detail-page {
  max-width: 1300px;
  margin: 0 auto;
  padding: 20px;
  background: #f6f8fb;
  min-height: 100vh;
}

.back-button {
  margin: 14px 0;
}

.detail-card,
.comment-card {
  border-radius: 8px;
}

.log-article {
  h1 {
    margin: 22px 0 10px;
    color: #111827;
    font-size: 30px;
    line-height: 1.35;
  }
}

.article-header,
.article-actions,
.comment-header,
.comment-author,
.notification-title-row {
  display: flex;
  align-items: center;
}

.article-header {
  justify-content: space-between;
}

.author-block {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 4px;
    color: #909399;
    font-size: 13px;
  }
}

.location-line {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.rating-panel {
  padding: 16px;
  margin: 20px 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fbff;
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
}

.content-text {
  white-space: pre-wrap;
  color: #374151;
  line-height: 1.9;
  font-size: 16px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.log-image {
  width: 100%;
  height: 150px;
  border-radius: 8px;
  background: #f5f7fa;
}

.animation-panel {
  margin-top: 22px;
  padding: 18px;
  border: 1px solid #e0e7ff;
  border-radius: 10px;
  background: linear-gradient(135deg, #f5f7ff 0%, #fdf4ff 100%);

  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    h3 {
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0;
      color: #4338ca;
      font-size: 16px;
    }
  }
}

.animation-player video {
  width: 100%;
  max-height: 420px;
  border-radius: 8px;
  background: #000;
}

.animation-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;

  &.failed {
    color: #b91c1c;
  }
}

.animation-actions {
  margin-top: 12px;
}

.itinerary-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 16px;
  margin-top: 22px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  cursor: pointer;

  span {
    color: #2563eb;
    font-size: 12px;
    font-weight: 700;
  }

  h3 {
    margin: 6px 0;
  }

  p {
    margin: 0;
    color: #606266;
    line-height: 1.6;
  }
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

.article-actions {
  gap: 16px;
  padding-top: 22px;
  margin-top: 22px;
  border-top: 1px solid #ebeef5;
  color: #606266;

  span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
  }
}

.like-icon {
  width: 16px;
  height: 16px;
  margin-right: 4px;
}

.comment-header {
  justify-content: space-between;

  h3 {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0;
  }
}

.comment-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.comment-editor {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.comment-list {
  min-height: 180px;
}

.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  p {
    margin: 10px 0;
    color: #374151;
    line-height: 1.7;
  }
}

.comment-author {
  gap: 10px;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 3px;
    color: #909399;
    font-size: 12px;
  }
}

.reply-editor {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  margin-top: 8px;
}

.reply-list {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #f5f7fa;
}

.reply-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #606266;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .dimension-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .itinerary-card,
  .article-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
