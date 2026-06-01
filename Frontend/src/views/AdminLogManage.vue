<template>
  <div class="admin-log-page">
    <AppHeader />

    <main class="admin-main">
      <section class="admin-hero">
        <div>
          <p class="eyebrow">Content Governance</p>
          <h1>日志治理</h1>
          <p class="hero-copy">
            主动查看景点、圈子和用户发布的日志。这里删除日志采用业务软删除，并会同步刷新关联景点评分。
          </p>
        </div>
        <div class="hero-actions">
          <el-button @click="router.push('/admin')">返回后台</el-button>
          <el-button @click="router.push('/admin/reports')">举报审核</el-button>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadLogs">
            刷新
          </el-button>
        </div>
      </section>

      <section class="log-panel">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索标题、内容或景点"
            class="keyword-input"
            @keyup.enter="resetAndLoad"
            @clear="resetAndLoad"
          />
          <el-input-number
            v-model="query.spotId"
            :min="1"
            controls-position="right"
            placeholder="景点/POI ID"
            class="id-input"
            @change="resetAndLoad"
          />
          <el-input-number
            v-model="query.circleId"
            :min="1"
            controls-position="right"
            placeholder="圈子 ID"
            class="id-input"
            @change="resetAndLoad"
          />
          <el-input-number
            v-model="query.userId"
            :min="1"
            controls-position="right"
            placeholder="用户 ID"
            class="id-input"
            @change="resetAndLoad"
          />
          <el-select v-model="query.tab" class="toolbar-select" @change="resetAndLoad">
            <el-option label="最新优先" value="all" />
            <el-option label="热度优先" value="hot" />
          </el-select>
          <el-button :icon="Search" @click="resetAndLoad">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="logs" border class="log-table" empty-text="暂无日志">
          <el-table-column label="日志内容" min-width="360">
            <template #default="{ row }">
              <div class="log-title-row">
                <span class="log-title">{{ row.title || `日志 #${row.id}` }}</span>
                <el-tag v-if="row.rating" size="small" type="warning">{{ row.rating }} 分</el-tag>
              </div>
              <p class="log-content">{{ row.content || '暂无内容' }}</p>
              <div v-if="row.images?.length" class="image-strip">
                <el-image
                  v-for="image in row.images.slice(0, 4)"
                  :key="image"
                  class="log-image"
                  :src="image"
                  fit="cover"
                  :preview-src-list="row.images"
                  preview-teleported
                />
                <span v-if="row.images.length > 4" class="more-image">+{{ row.images.length - 4 }}</span>
              </div>
              <div v-if="row.tags?.length" class="tag-list">
                <el-tag v-for="tag in row.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发布人" min-width="150">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="34" :src="row.avatar || defaultAvatar" />
                <div>
                  <div>{{ row.username || `用户 ${row.userId}` }}</div>
                  <div class="meta-line">ID {{ row.userId }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="关联对象" min-width="180">
            <template #default="{ row }">
              <div>{{ row.location || '未关联景点' }}</div>
              <div class="meta-line">景点/POI：{{ row.spotId || '-' }}</div>
              <div class="meta-line">圈子：{{ row.circleId || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="数据" width="150">
            <template #default="{ row }">
              <div>热度：{{ row.hotness || 0 }}</div>
              <div class="meta-line">浏览 {{ row.viewCount || 0 }} · 点赞 {{ row.likeCount || 0 }}</div>
              <div class="meta-line">评论 {{ row.commentCount || 0 }}</div>
            </template>
          </el-table-column>
          <el-table-column label="发布时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/log/${row.id}`)">
                查看
              </el-button>
              <el-button text @click="openComments(row)">
                评论
              </el-button>
              <el-button text type="danger" @click="deleteLog(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :page-sizes="[10, 20, 30, 50]"
            @current-change="loadLogs"
            @size-change="resetAndLoad"
          />
        </div>
      </section>
    </main>

    <el-drawer
      v-model="commentDrawerVisible"
      :title="commentDrawerTitle"
      size="520px"
      append-to-body
    >
      <div v-loading="commentsLoading" class="comment-drawer">
        <el-empty v-if="!comments.length && !commentsLoading" description="暂无评论" />
        <div v-for="comment in comments" :key="comment.id" class="comment-block">
          <div class="comment-main">
            <el-avatar :size="34" :src="comment.avatar || defaultAvatar" />
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-user">{{ comment.username || `用户 ${comment.userId}` }}</span>
                <span class="meta-line">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
              <el-button text type="danger" size="small" @click="deleteComment(comment)">
                删除评论
              </el-button>
            </div>
          </div>

          <div v-if="comment.replies?.length" class="reply-list">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <el-avatar :size="28" :src="reply.avatar || defaultAvatar" />
              <div class="comment-body">
                <div class="comment-head">
                  <span class="comment-user">{{ reply.username || `用户 ${reply.userId}` }}</span>
                  <span class="meta-line">{{ formatDate(reply.createdAt) }}</span>
                </div>
                <p class="comment-content">{{ reply.content }}</p>
                <el-button text type="danger" size="small" @click="deleteComment(reply)">
                  删除回复
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { deleteAdminComment, deleteAdminLog, getAdminLogComments, getAdminLogs } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const query = reactive(defaultQuery(true))
const commentDrawerVisible = ref(false)
const commentsLoading = ref(false)
const selectedLog = ref(null)
const comments = ref([])
const commentDrawerTitle = computed(() => selectedLog.value ? `评论治理：${selectedLog.value.title || `日志 #${selectedLog.value.id}`}` : '评论治理')

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入日志治理')
    router.push('/')
    return
  }
  await loadLogs()
})

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getAdminLogs({
      keyword: query.keyword || undefined,
      spotId: query.spotId || undefined,
      circleId: query.circleId || undefined,
      userId: query.userId || undefined,
      tab: query.tab,
      page: query.page,
      pageSize: query.pageSize
    })
    logs.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  query.page = 1
  loadLogs()
}

const resetQuery = () => {
  Object.assign(query, defaultQuery())
  loadLogs()
}

const deleteLog = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.title || `日志 #${row.id}`}」吗？删除后用户个人主页、景点详情和圈子中都会同步不可见。`,
      '删除日志',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await deleteAdminLog(row.id)
    ElMessage.success('日志已删除')
    await loadLogs()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除日志失败:', error)
    }
  }
}

const openComments = async (row) => {
  selectedLog.value = row
  commentDrawerVisible.value = true
  await loadComments()
}

const loadComments = async () => {
  if (!selectedLog.value?.id) return
  commentsLoading.value = true
  try {
    const res = await getAdminLogComments(selectedLog.value.id)
    comments.value = res.data?.list || []
  } finally {
    commentsLoading.value = false
  }
}

const deleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm(
      `确定删除这条评论吗？评论内容：${comment.content || `#${comment.id}`}`,
      '删除评论',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await deleteAdminComment(comment.id)
    ElMessage.success('评论已删除')
    await loadComments()
    await loadLogs()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除评论失败:', error)
    }
  }
}

const formatDate = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function defaultQuery(useRouteQuery = false) {
  return {
    keyword: '',
    spotId: useRouteQuery ? positiveNumber(route.query.spotId) : null,
    circleId: useRouteQuery ? positiveNumber(route.query.circleId) : null,
    userId: useRouteQuery ? positiveNumber(route.query.userId) : null,
    tab: 'all',
    page: 1,
    pageSize: 10
  }
}

function positiveNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}
</script>

<style lang="scss" scoped>
.admin-log-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

.admin-hero,
.log-panel {
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
}

.admin-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  box-shadow: 0 8px 24px rgba(30, 45, 70, 0.06);

  h1 {
    margin: 6px 0 10px;
    font-size: 30px;
    color: #1f2a3d;
  }
}

.eyebrow {
  margin: 0;
  color: #4f7cff;
  font-size: 13px;
  font-weight: 700;
}

.hero-copy {
  max-width: 720px;
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.log-panel {
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

.keyword-input {
  width: 280px;
}

.id-input {
  width: 140px;
}

.toolbar-select {
  width: 120px;
}

.log-table {
  width: 100%;
}

.log-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-title {
  color: #263243;
  font-weight: 700;
}

.log-content {
  display: -webkit-box;
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.image-strip,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.log-image {
  width: 64px;
  height: 44px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.more-image {
  display: grid;
  place-items: center;
  width: 42px;
  height: 44px;
  border-radius: 6px;
  color: #64748b;
  background: #f1f5f9;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-line {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.comment-drawer {
  min-height: 240px;
}

.comment-block {
  padding: 14px 0;
  border-bottom: 1px solid #edf0f5;
}

.comment-main,
.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comment-body {
  min-width: 0;
  flex: 1;
}

.comment-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.comment-user {
  color: #263243;
  font-weight: 700;
}

.comment-content {
  margin: 6px 0 2px;
  color: #475569;
  line-height: 1.6;
  word-break: break-word;
}

.reply-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  margin-left: 44px;
  padding: 12px;
  border-radius: 6px;
  background: #f8fafc;
}

@media (max-width: 960px) {
  .admin-main {
    padding: 18px 14px 32px;
  }

  .admin-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .keyword-input,
  .id-input,
  .toolbar-select {
    width: 100%;
  }
}
</style>
