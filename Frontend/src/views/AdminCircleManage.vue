<template>
  <div class="admin-circle-page">
    <AppHeader />

    <main class="admin-main">
      <section class="admin-hero">
        <div>
          <p class="eyebrow">Community Operation</p>
          <h1>圈子管理</h1>
          <p class="hero-copy">
            维护圈子封面、基础信息和启用状态。圈子是旅行分享和用户互动入口，不能只依赖前台用户自维护。
          </p>
        </div>
        <div class="hero-actions">
          <el-button @click="router.push('/admin')">返回后台</el-button>
          <el-button @click="router.push('/admin/logs')">日志治理</el-button>
          <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadCircles">刷新</el-button>
        </div>
      </section>

      <section class="circle-panel">
        <div class="toolbar">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索圈子、描述或圈主"
            class="keyword-input"
            @keyup.enter="resetAndLoad"
            @clear="resetAndLoad"
          />
          <el-select v-model="query.status" clearable placeholder="状态" class="status-select" @change="resetAndLoad">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-button :icon="Search" @click="resetAndLoad">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>

        <el-table v-loading="loading" :data="circles" border empty-text="暂无圈子">
          <el-table-column label="封面" width="142">
            <template #default="{ row }">
              <div class="cover-cell">
                <el-image
                  v-if="row.cover"
                  class="circle-cover"
                  :src="row.cover"
                  fit="cover"
                  :preview-src-list="[row.cover]"
                  preview-teleported
                />
                <div v-else class="cover-placeholder">暂无封面</div>
                <AdminImageUpload
                  v-if="row.status === 1"
                  target-type="circle"
                  :target-id="row.id"
                  label="上传"
                  @success="url => handleCoverUploaded(row, url)"
                />
                <el-tag v-else size="small" type="info">停用后不可上传</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="圈子" min-width="260">
            <template #default="{ row }">
              <div class="circle-name">{{ row.name }}</div>
              <p class="circle-desc">{{ row.description || '暂无描述' }}</p>
              <div class="meta-line">ID {{ row.id }} · 圈主：{{ row.ownerName || `用户 ${row.ownerId}` }}</div>
            </template>
          </el-table-column>
          <el-table-column label="数据" width="150">
            <template #default="{ row }">
              <div>成员 {{ row.members || 0 }}</div>
              <div class="meta-line">日志 {{ row.logs || row.posts || 0 }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button text @click="goCircleLogs(row)">日志</el-button>
              <el-button
                text
                :type="row.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '停用' : '启用' }}
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
            :page-sizes="[10, 20, 50]"
            @current-change="loadCircles"
            @size-change="resetAndLoad"
          />
        </div>
      </section>
    </main>

    <el-dialog v-model="editDialogVisible" title="编辑圈子" width="640px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="圈子名称">
          <el-input v-model="editForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-editor">
            <el-image
              v-if="editForm.cover"
              class="form-cover"
              :src="editForm.cover"
              fit="cover"
              :preview-src-list="[editForm.cover]"
              preview-teleported
            />
            <div v-else class="form-cover empty">暂无封面</div>
            <div class="cover-editor-main">
              <el-input v-model="editForm.cover" placeholder="可直接填写图片 URL" />
              <AdminImageUpload
                v-if="editForm.id && editForm.status === 1"
                target-type="circle"
                :target-id="editForm.id"
                label="上传封面"
                @success="handleEditCoverUploaded"
              />
              <el-alert
                v-else
                type="info"
                :closable="false"
                title="停用圈子需要先启用，才能通过上传接口更换封面；也可以直接填写图片 URL 后保存。"
              />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCircle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AdminImageUpload from '@/components/AdminImageUpload.vue'
import { getAdminCircles, updateAdminCircle, updateAdminCircleStatus } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const circles = ref([])
const total = ref(0)
const query = reactive(defaultQuery())
const editDialogVisible = ref(false)
const editForm = reactive(emptyEditForm())

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  if (userStore.userInfo?.role !== 'admin') {
    ElMessage.warning('只有管理员可以进入圈子管理')
    router.push('/')
    return
  }
  await loadCircles()
})

const loadCircles = async () => {
  loading.value = true
  try {
    const res = await getAdminCircles({
      keyword: query.keyword || undefined,
      status: query.status ?? undefined,
      page: query.page,
      pageSize: query.pageSize
    })
    circles.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  query.page = 1
  loadCircles()
}

const resetQuery = () => {
  Object.assign(query, defaultQuery())
  loadCircles()
}

const openEditDialog = (row) => {
  Object.assign(editForm, {
    id: row.id,
    name: row.name,
    description: row.description,
    cover: row.cover,
    status: row.status
  })
  editDialogVisible.value = true
}

const saveCircle = async () => {
  saving.value = true
  try {
    await updateAdminCircle(editForm.id, {
      name: editForm.name,
      description: editForm.description,
      cover: editForm.cover
    })
    ElMessage.success('圈子信息已保存')
    editDialogVisible.value = false
    await loadCircles()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}「${row.name}」吗？停用后普通用户将无法在圈子列表和详情中看到该圈子。`,
      `${actionText}圈子`,
      {
        type: 'warning',
        confirmButtonText: actionText,
        cancelButtonText: '取消'
      }
    )
    await updateAdminCircleStatus(row.id, nextStatus)
    ElMessage.success(`圈子已${actionText}`)
    await loadCircles()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('更新圈子状态失败:', error)
    }
  }
}

const handleCoverUploaded = (row, imageUrl) => {
  row.cover = imageUrl || row.cover
  loadCircles()
}

const handleEditCoverUploaded = (imageUrl) => {
  editForm.cover = imageUrl || editForm.cover
  loadCircles()
}

const goCircleLogs = (row) => {
  router.push({ path: '/admin/logs', query: { circleId: row.id } })
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

function defaultQuery() {
  return {
    keyword: '',
    status: null,
    page: 1,
    pageSize: 10
  }
}

function emptyEditForm() {
  return {
    id: null,
    name: '',
    description: '',
    cover: '',
    status: 1
  }
}
</script>

<style lang="scss" scoped>
.admin-circle-page {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

.admin-hero,
.circle-panel {
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

.hero-actions,
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.circle-panel {
  margin-top: 18px;
  padding: 18px;
}

.toolbar {
  margin-bottom: 16px;
}

.keyword-input {
  width: 300px;
}

.status-select {
  width: 140px;
}

.cover-cell {
  display: grid;
  gap: 8px;
  justify-items: center;
}

.circle-cover,
.cover-placeholder {
  width: 96px;
  height: 62px;
  border-radius: 6px;
}

.circle-cover {
  border: 1px solid #e5e7eb;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  color: #9aa3b2;
  font-size: 12px;
  background: #f2f4f7;
  border: 1px dashed #d9dee8;
}

.circle-name {
  color: #263243;
  font-weight: 700;
}

.circle-desc {
  display: -webkit-box;
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
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

.cover-editor {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 14px;
  width: 100%;
}

.form-cover {
  width: 170px;
  height: 110px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;

  &.empty {
    display: grid;
    place-items: center;
    color: #9aa3b2;
    background: #f2f4f7;
    border-style: dashed;
  }
}

.cover-editor-main {
  display: grid;
  align-content: start;
  gap: 10px;
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
  .status-select {
    width: 100%;
  }

  .cover-editor {
    grid-template-columns: 1fr;
  }
}
</style>
