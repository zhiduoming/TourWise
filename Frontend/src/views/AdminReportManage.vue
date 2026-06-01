<template>
  <div class="admin-report-page">
    <AppHeader />

    <main class="admin-main">
      <section class="admin-hero">
        <div>
          <p class="eyebrow">Content Moderation</p>
          <h1>内容审核</h1>
          <p class="hero-copy">
            处理用户对日志和评论的举报。这里的删除都是业务软删除，不会物理移除数据库记录和文件。
          </p>
        </div>
        <div class="hero-actions">
          <el-button @click="router.push('/admin')">返回后台</el-button>
          <el-button @click="router.push('/admin/logs')">日志治理</el-button>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadReports">
            刷新
          </el-button>
        </div>
      </section>

      <section class="report-panel">
        <div class="toolbar">
          <el-select v-model="query.status" class="toolbar-select" @change="resetAndLoad">
            <el-option label="待处理" value="pending" />
            <el-option label="已处理" value="handled" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="全部" value="all" />
          </el-select>
          <el-select v-model="query.targetType" clearable placeholder="内容类型" class="toolbar-select" @change="resetAndLoad">
            <el-option label="日志" value="log" />
            <el-option label="评论" value="comment" />
          </el-select>
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索举报原因、内容、用户"
            class="keyword-input"
            @keyup.enter="resetAndLoad"
            @clear="resetAndLoad"
          />
          <el-button :icon="Search" @click="resetAndLoad">查询</el-button>
        </div>

        <el-table v-loading="loading" :data="reports" border class="report-table" empty-text="暂无举报记录">
          <el-table-column label="举报对象" min-width="260">
            <template #default="{ row }">
              <div class="target-title">
                <el-tag size="small" :type="row.targetType === 'log' ? 'primary' : 'warning'">
                  {{ targetTypeText(row.targetType) }}
                </el-tag>
                <span>{{ row.targetTitle || `#${row.targetId}` }}</span>
              </div>
              <p class="content-snippet">{{ row.targetContent || '暂无内容摘要' }}</p>
              <el-button
                v-if="row.targetType === 'log'"
                text
                type="primary"
                size="small"
                @click="router.push(`/log/${row.targetId}`)"
              >
                查看日志
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="举报信息" min-width="260">
            <template #default="{ row }">
              <div class="reason-line">{{ row.reason }}</div>
              <p class="content-snippet">{{ row.detail || '未填写补充说明' }}</p>
              <div class="meta-line">
                举报人：{{ row.reporterName || `用户 ${row.reporterId}` }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="106">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">
              <div>{{ formatDate(row.createdAt) }}</div>
              <div v-if="row.handledAt" class="meta-line">处理：{{ formatDate(row.handledAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="处理结果" min-width="180">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <span class="muted">等待处理</span>
              </template>
              <template v-else>
                <div>{{ row.handlerName || '管理员' }}</div>
                <p class="content-snippet">{{ row.handleNote || '无处理备注' }}</p>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button text type="danger" @click="handleReport(row, 'handled', true)">
                  删除内容
                </el-button>
                <el-button text type="primary" @click="handleReport(row, 'handled', false)">
                  已处理
                </el-button>
                <el-button text @click="handleReport(row, 'rejected', false)">
                  驳回
                </el-button>
              </template>
              <span v-else class="muted">已完成</span>
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
            @current-change="loadReports"
            @size-change="resetAndLoad"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import { getContentReports, handleContentReport } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const reports = ref([])
const total = ref(0)
const query = reactive({
  status: 'pending',
  targetType: '',
  keyword: '',
  page: 1,
  pageSize: 10
})

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入内容审核')
    router.push('/')
    return
  }
  await loadReports()
})

const loadReports = async () => {
  loading.value = true
  try {
    const res = await getContentReports({
      status: query.status,
      targetType: query.targetType || undefined,
      keyword: query.keyword || undefined,
      page: query.page,
      pageSize: query.pageSize
    })
    reports.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  query.page = 1
  loadReports()
}

const handleReport = async (row, status, deleteTarget) => {
  const title = deleteTarget ? '处理并删除内容' : status === 'rejected' ? '驳回举报' : '标记已处理'
  const defaultNote = deleteTarget ? '内容违规，已由管理员删除。' : status === 'rejected' ? '未发现明显违规。' : '已完成处理。'
  try {
    const { value } = await ElMessageBox.prompt('请输入处理备注', title, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValue: defaultNote,
      inputType: 'textarea',
      inputValidator: value => (value || '').trim().length > 0 || '处理备注不能为空'
    })
    await handleContentReport(row.id, {
      status,
      deleteTarget,
      handleNote: value.trim()
    })
    ElMessage.success('举报已处理')
    await loadReports()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('处理举报失败:', error)
    }
  }
}

const targetTypeText = (type) => {
  if (type === 'log') return '日志'
  if (type === 'comment') return '评论'
  return '内容'
}

const statusText = (status) => {
  if (status === 'handled') return '已处理'
  if (status === 'rejected') return '已驳回'
  return '待处理'
}

const statusType = (status) => {
  if (status === 'handled') return 'success'
  if (status === 'rejected') return 'info'
  return 'danger'
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
</script>

<style lang="scss" scoped>
.admin-report-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

.admin-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(30, 45, 70, 0.06);

  h1 {
    margin: 6px 0 10px;
    font-size: 30px;
    line-height: 1.25;
    color: #1f2a3d;
  }
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  color: #4f7cff;
  font-weight: 700;
}

.hero-copy {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

.hero-actions,
.toolbar,
.target-title {
  display: flex;
  align-items: center;
}

.hero-actions {
  gap: 12px;
  flex-shrink: 0;
}

.report-panel {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #e3e8f3;
  border-radius: 8px;
  overflow: hidden;
}

.toolbar {
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid #e6ebf4;
}

.toolbar-select {
  width: 140px;
}

.keyword-input {
  width: 320px;
}

.report-table {
  width: 100%;
}

.target-title {
  gap: 8px;
  font-weight: 700;
  color: #263243;
}

.reason-line {
  font-weight: 700;
  color: #1f2a3d;
}

.content-snippet {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: #6b7280;
  line-height: 1.55;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta-line,
.muted {
  color: #98a2b3;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
}

@media (max-width: 960px) {
  .admin-main {
    padding: 18px 14px 32px;
  }

  .admin-hero,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-select,
  .keyword-input {
    width: 100%;
  }
}
</style>
