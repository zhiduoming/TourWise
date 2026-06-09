<template>
  <div class="profile-container">
    <AppHeader />
    <el-row :gutter="20">
      <!-- 左侧个人信息卡片 -->
      <el-col :xs="24" :lg="8">
        <el-card class="profile-card">
          <div class="profile-header">
            <div class="avatar-section">
              <el-avatar :size="120" :src="profileForm.avatar || defaultAvatar" class="avatar" />
              <div class="avatar-overlay" @click="triggerAvatarUpload">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleAvatarChange"
              />
            </div>
            <h2 class="username">{{ profileForm.username || profileForm.nickname }}</h2>
            <p class="signature">
              {{ profileForm.signature || '这个人很懒，什么都没写~' }}
            </p>
          </div>

          <el-divider />

          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ diaryCount }}</div>
              <div class="stat-label">日记</div>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <div class="stat-value">{{ profileForm.visits || 0 }}</div>
              <div class="stat-label">访问</div>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <div class="stat-value">{{ profileForm.favorites || 0 }}</div>
              <div class="stat-label">收藏</div>
            </div>
          </div>

          <el-divider />

          <el-button type="primary" plain style="width: 100%" @click="showEditDialog = true">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
        </el-card>

        <!-- 快捷信息 -->
        <el-card class="info-card" style="margin-top: 20px">
          <template #header>
            <h3 class="card-title">个人信息</h3>
          </template>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="手机号">
              {{ profileForm.phone || '未绑定' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ profileForm.email || '未绑定' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ formatDate(profileForm.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ formatDate(profileForm.lastLoginAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧我的日记 -->
      <el-col :xs="24" :lg="16">
        <el-card ref="notificationCardRef" class="notification-card">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Bell /></el-icon>
                消息通知
              </h3>
              <div class="notification-header-actions">
                <el-switch
                  v-model="notificationOnlyUnread"
                  active-text="未读"
                  inactive-text="全部"
                  @change="handleNotificationFilterChange"
                />
                <el-button
                  text
                  type="primary"
                  :disabled="notificationUnreadCount === 0"
                  @click="handleMarkAllNotificationsRead"
                >
                  全部已读
                </el-button>
              </div>
            </div>
          </template>

          <div v-loading="notificationLoading" class="notification-list">
            <el-empty v-if="notifications.length === 0" description="暂无通知" :image-size="90" />
            <article
              v-for="item in notifications"
              v-else
              :key="item.id"
              class="notification-item"
              :class="{ unread: !item.read }"
              @click="openNotification(item)"
            >
              <div class="notification-icon">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="notification-main">
                <div class="notification-title-row">
                  <h4>{{ item.title }}</h4>
                  <el-tag v-if="!item.read" size="small" type="danger">未读</el-tag>
                </div>
                <p>{{ item.content }}</p>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <el-button
                v-if="!item.read"
                size="small"
                text
                type="primary"
                @click.stop="markSingleNotificationRead(item)"
              >
                标为已读
              </el-button>
            </article>

            <el-pagination
              v-if="notificationTotal > notificationPageSize"
              v-model:current-page="notificationPage"
              :page-size="notificationPageSize"
              :total="notificationTotal"
              layout="total, prev, pager, next"
              @current-change="loadNotifications"
              class="pagination"
            />
          </div>
        </el-card>

        <el-card class="fulltext-card">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Search /></el-icon>
                日记全文检索
                <el-tag size="small" type="info" effect="plain" style="margin-left: 8px;">倒排索引</el-tag>
              </h3>
              <el-button v-if="fullTextHasSearched" size="small" text @click="resetFullTextSearch">清空</el-button>
            </div>
          </template>

          <div class="fulltext-search-row">
            <el-input
              v-model="fullTextKeyword"
              placeholder="搜索全站日记（标题/正文，支持中英文混合）"
              clearable
              @keyup.enter="fullTextPage = 1; runFullTextSearch()"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" :loading="fullTextLoading" @click="fullTextPage = 1; runFullTextSearch()">
              检索
            </el-button>
          </div>
          <p class="fulltext-hint">
            该入口走 Java 端 bigram 倒排索引，O(命中数) 检索，区别于 SQL LIKE 的全表扫描。
          </p>

          <div v-loading="fullTextLoading" class="fulltext-results">
            <el-empty
              v-if="fullTextHasSearched && fullTextResults.length === 0 && !fullTextLoading"
              description="没有命中的日记"
              :image-size="80"
            />
            <article
              v-for="item in fullTextResults"
              :key="`ft-${item.id}`"
              class="fulltext-item"
              @click="openFullTextResult(item)"
            >
              <div class="fulltext-main">
                <h4>{{ item.title || '无标题日记' }}</h4>
                <p>{{ item.content }}</p>
                <div class="fulltext-meta">
                  <span><el-icon><Document /></el-icon> {{ item.username || '匿名' }}</span>
                  <span v-if="item.location"><el-icon><Location /></el-icon> {{ item.location }}</span>
                  <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }}</span>
                </div>
              </div>
            </article>
            <el-pagination
              v-if="fullTextTotal > fullTextPageSize"
              v-model:current-page="fullTextPage"
              :page-size="fullTextPageSize"
              :total="fullTextTotal"
              layout="total, prev, pager, next"
              @current-change="handleFullTextPageChange"
              class="pagination"
            />
          </div>
        </el-card>

        <el-card class="diary-card">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Document /></el-icon>
                我的日记
              </h3>
              <el-button type="primary" @click="$router.push('/diary')">
                <el-icon><Plus /></el-icon>
                写日记
              </el-button>
            </div>
          </template>

          <el-tabs v-model="diaryTab" @tab-change="handleDiaryTabChange">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane label="最近" name="recent" />
            <el-tab-pane label="精华" name="featured" />
          </el-tabs>

          <div v-loading="diaryLoading" class="diary-list">
            <div v-if="myDiaries.length === 0" class="empty-state">
              <el-empty description="还没有写日记哦~">
                <el-button type="primary" @click="$router.push('/diary')">
                  去写一篇
                </el-button>
              </el-empty>
            </div>

            <div v-else>
              <el-timeline>
                <el-timeline-item
                  v-for="item in myDiaries"
                  :key="item.id"
                  :timestamp="formatDate(item.date)"
                  placement="top"
                >
                  <el-card class="diary-item-card">
                    <div class="diary-item" @click="viewLogDetail(item)">
                      <div class="diary-item-header">
                        <h4 class="diary-item-title">{{ item.title }}</h4>
                        <div class="diary-item-actions">
                          <el-button size="small" @click.stop="handleEditDiary(item)">编辑</el-button>
                          <el-button size="small" type="danger" @click.stop="handleDeleteDiary(item.id)">删除</el-button>
                        </div>
                      </div>
                      <p class="diary-item-content">{{ item.content }}</p>
                      <div
                        v-if="item.itineraryPlanId"
                        class="diary-plan-card"
                        @click.stop="openItineraryPlan(item)"
                      >
                        <div>
                          <span>行程计划</span>
                          <h4>{{ item.itineraryPlanTitle || '旅行计划' }}</h4>
                          <p>{{ item.itineraryPlanSummary || '打开查看完整行程安排。' }}</p>
                        </div>
                        <el-button text type="primary">查看行程</el-button>
                      </div>
                      <div v-if="item.images?.length" class="diary-item-images">
                        <el-image
                          v-for="(img, index) in item.images"
                          :key="`${item.id}-${index}`"
                          :src="img"
                          class="diary-item-image"
                          fit="cover"
                          :preview-src-list="item.images"
                          :initial-index="index"
                          preview-teleported
                        />
                      </div>
                      <div class="diary-item-meta">
                        <span class="diary-item-location">
                          <el-icon><Location /></el-icon>
                          {{ item.location }}
                        </span>
                        <span class="diary-item-mood">
                          <el-rate v-model="item.mood" disabled size="small" />
                        </span>
                      </div>
                      <div class="diary-item-tags" v-if="item.tags?.length">
                        <el-tag
                          v-for="tag in item.tags"
                          :key="tag"
                          size="small"
                          style="margin-right: 8px"
                        >
                          {{ tag }}
                        </el-tag>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>

              <el-pagination
                v-model:current-page="diaryPage"
                v-model:page-size="diaryPageSize"
                :total="diaryTotal"
                layout="total, prev, pager, next"
                @current-change="loadMyDiaries"
                class="pagination"
              />
            </div>
          </div>
        </el-card>

        <el-card class="plan-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Calendar /></el-icon>
                我的行程
              </h3>
              <el-button text type="primary" @click="$router.push('/itinerary')">
                生成行程
              </el-button>
            </div>
          </template>

          <div v-loading="planLoading" class="plan-list">
            <el-empty v-if="savedPlans.length === 0" description="还没有保存行程" :image-size="90">
              <el-button type="primary" @click="$router.push('/itinerary')">
                去生成
              </el-button>
            </el-empty>
            <article
              v-for="item in savedPlans"
              v-else
              :key="item.id"
              class="plan-item"
              @click="openSavedPlan(item)"
            >
              <div class="plan-icon">
                <el-icon><Compass /></el-icon>
              </div>
              <div class="plan-info">
                <h4>{{ item.title || item.summary }}</h4>
                <p>{{ item.summary }}</p>
                <div class="plan-meta">
                  <span>{{ item.city || '未知城市' }}</span>
                  <span>{{ durationText(item.duration) }}</span>
                  <span>{{ item.spotCount || 0 }} 个景点</span>
                  <span>{{ formatDate(item.updatedAt || item.createdAt) }}</span>
                </div>
              </div>
            </article>

            <el-button
              v-if="planTotal > savedPlans.length"
              text
              type="primary"
              class="more-plan-button"
              @click="$router.push('/itinerary')"
            >
              查看全部行程
            </el-button>
          </div>
        </el-card>

        <el-card class="favorite-plan-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><Star /></el-icon>
                收藏行程
              </h3>
              <el-button text type="primary" @click="$router.push('/itinerary')">
                热门行程
              </el-button>
            </div>
          </template>

          <div v-loading="favoritePlanLoading" class="favorite-plan-list">
            <el-empty v-if="favoritePlans.length === 0" description="还没有收藏行程" :image-size="90">
              <el-button type="primary" @click="$router.push('/itinerary')">
                去看看
              </el-button>
            </el-empty>
            <article
              v-for="item in favoritePlans"
              v-else
              :key="item.id"
              class="favorite-plan-item"
              @click="openFavoritePlan(item)"
            >
              <div class="plan-icon favorite-plan-icon">
                <el-icon><StarFilled /></el-icon>
              </div>
              <div class="plan-info">
                <h4>{{ item.title || item.summary }}</h4>
                <p>{{ item.summary }}</p>
                <div class="plan-meta">
                  <span>{{ item.ownerName || '圈子用户' }}</span>
                  <span>{{ item.city || '未知城市' }}</span>
                  <span>{{ durationText(item.duration) }}</span>
                  <span>{{ item.spotCount || 0 }} 个景点</span>
                  <span>{{ item.copyCount || 0 }} 次复制</span>
                  <span>{{ item.favoriteCount || 0 }} 次收藏</span>
                </div>
              </div>
              <div class="favorite-plan-actions">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :loading="favoritePlanActionLoading === `copy-${item.id}`"
                  @click.stop="copyFavoritePlan(item)"
                >
                  复制
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  plain
                  :loading="favoritePlanActionLoading === `favorite-${item.id}`"
                  @click.stop="cancelFavoritePlan(item)"
                >
                  取消收藏
                </el-button>
              </div>
            </article>

            <el-pagination
              v-if="favoritePlanTotal > favoritePlanPageSize"
              v-model:current-page="favoritePlanPage"
              :page-size="favoritePlanPageSize"
              :total="favoritePlanTotal"
              layout="total, prev, pager, next"
              @current-change="loadFavoritePlans"
              class="pagination"
            />
          </div>
        </el-card>

        <el-card class="behavior-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">
                <el-icon><StarFilled /></el-icon>
                我的足迹
              </h3>
            </div>
          </template>

          <el-tabs v-model="behaviorTab" @tab-change="handleBehaviorTabChange">
            <el-tab-pane label="收藏" name="favorite" />
            <el-tab-pane label="想去" name="want" />
            <el-tab-pane label="去过" name="visited" />
            <el-tab-pane label="浏览历史" name="history" />
          </el-tabs>

          <div v-loading="behaviorLoading" class="behavior-list">
            <el-empty v-if="behaviorItems.length === 0" description="暂无记录" :image-size="90" />
            <div
              v-for="item in behaviorItems"
              v-else
              :key="`${behaviorTab}-${item.id}`"
              class="behavior-item"
              @click="openSpot(item)"
            >
              <el-image :src="item.image || '/spot-placeholder.jpg'" fit="cover" class="behavior-cover">
                <template #error>
                  <div class="behavior-cover-placeholder">暂无图片</div>
                </template>
              </el-image>
              <div class="behavior-info">
                <h4>{{ item.name }}</h4>
                <p>{{ item.description || item.location || '暂无简介' }}</p>
                <div class="behavior-meta">
                  <span>
                    <el-icon><Star /></el-icon>
                    {{ item.rating || item.score || '暂无评分' }}
                  </span>
                  <span>
                    <el-icon><View /></el-icon>
                    {{ item.hotness || 0 }}
                  </span>
                  <span v-if="item.city || item.district">
                    <el-icon><Location /></el-icon>
                    {{ item.city || item.district }}
                  </span>
                </div>
              </div>
            </div>

            <el-pagination
              v-if="behaviorTotal > behaviorPageSize"
              v-model:current-page="behaviorPage"
              :page-size="behaviorPageSize"
              :total="behaviorTotal"
              layout="total, prev, pager, next"
              @current-change="loadBehaviorItems"
              class="pagination"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="500px">
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="签名">
          <el-input
            v-model="profileForm.signature"
            type="textarea"
            :rows="3"
            placeholder="介绍一下自己吧..."
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.gender">
            <el-radio label="male">男</el-radio>
            <el-radio label="female">女</el-radio>
            <el-radio label="secret">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="profileForm.birthday"
            type="date"
            placeholder="选择生日"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'
import { getProfile, updateProfile, uploadAvatar } from '@/api/profile'
import { getMyLogList, deleteLog, searchLogs } from '@/api/log'
import { getSpotActionList } from '@/api/spotAction'
import { copyItineraryPlan, getFavoriteItineraryPlans, getItineraryPlans, toggleItineraryFavorite } from '@/api/itinerary'
import { getNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead } from '@/api/notification'
import { Bell, Camera, Calendar, Compass, Edit, Plus, Document, Location, Search, Star, StarFilled, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const notificationCardRef = ref(null)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const profileForm = reactive({
  username: '',
  nickname: '',
  signature: '',
  avatar: '',
  phone: '',
  email: '',
  gender: 'secret',
  birthday: null,
  createdAt: null,
  lastLoginAt: null,
  visits: 0,
  favorites: 0
})

const showEditDialog = ref(false)
const diaryTab = ref('all')
const diaryLoading = ref(false)
const myDiaries = ref([])
const diaryCount = ref(0)
const diaryPage = ref(1)
const diaryPageSize = ref(5)
const diaryTotal = ref(0)
const behaviorTab = ref('favorite')
const behaviorLoading = ref(false)
const behaviorItems = ref([])
const behaviorTotal = ref(0)
const behaviorPage = ref(1)
const behaviorPageSize = ref(6)
const planLoading = ref(false)
const savedPlans = ref([])
const planTotal = ref(0)
const favoritePlanLoading = ref(false)
const favoritePlans = ref([])
const favoritePlanTotal = ref(0)
const favoritePlanPage = ref(1)
const favoritePlanPageSize = ref(3)
const favoritePlanActionLoading = ref('')
const fullTextKeyword = ref('')
const fullTextLoading = ref(false)
const fullTextResults = ref([])
const fullTextTotal = ref(0)
const fullTextPage = ref(1)
const fullTextPageSize = 5
const fullTextHasSearched = ref(false)

const notificationLoading = ref(false)
const notifications = ref([])
const notificationTotal = ref(0)
const notificationUnreadCount = ref(0)
const notificationPage = ref(1)
const notificationPageSize = ref(5)
const notificationOnlyUnread = ref(false)

const avatarInput = ref(null)

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 5MB')
    return
  }

  const formData = new FormData()
  formData.append('avatar', file)

  try {
    const res = await uploadAvatar(formData)
    profileForm.avatar = res.data?.avatarUrl || URL.createObjectURL(file)
    ElMessage.success('头像更新成功')
  } catch (error) {
    console.error('上传头像失败:', error)
    // 本地预览
    profileForm.avatar = URL.createObjectURL(file)
    ElMessage.success('头像已更新（未保存至服务器）')
  }

  // 清空 input
  event.target.value = ''
}

const loadProfile = async () => {
  try {
    const res = await getProfile()
    Object.assign(profileForm, res.data)
  } catch (error) {
    console.error('获取个人资料失败:', error)
    // 使用用户 store 中的数据
    profileForm.username = userStore.userInfo?.username || '用户'
    profileForm.nickname = userStore.userInfo?.nickname || userStore.userInfo?.username
  }
}

const handleSaveProfile = async () => {
  try {
    await updateProfile({
      nickname: profileForm.nickname,
      signature: profileForm.signature,
      gender: profileForm.gender,
      birthday: profileForm.birthday
    })
    ElMessage.success('资料更新成功')
    showEditDialog.value = false
  } catch (error) {
    console.error('更新资料失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
}

const loadMyDiaries = async () => {
  diaryLoading.value = true
  try {
    const params = {
      page: diaryPage.value,
      pageSize: diaryPageSize.value,
      tab: diaryTab.value
    }
    const res = await getMyLogList(params)
    myDiaries.value = res.data?.list || []
    diaryTotal.value = res.data?.total || 0
    diaryCount.value = diaryTotal.value
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载日志失败：' + (error.message || '未知错误'))
    myDiaries.value = []
    diaryTotal.value = 0
    diaryCount.value = 0
  } finally {
    diaryLoading.value = false
  }
}

const handleDiaryTabChange = () => {
  diaryPage.value = 1
  loadMyDiaries()
}

const loadBehaviorItems = async () => {
  behaviorLoading.value = true
  try {
    const res = await getSpotActionList({
      type: behaviorTab.value,
      page: behaviorPage.value,
      pageSize: behaviorPageSize.value
    })
    behaviorItems.value = res.data?.list || []
    behaviorTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载足迹失败:', error)
    behaviorItems.value = []
    behaviorTotal.value = 0
  } finally {
    behaviorLoading.value = false
  }
}

const handleBehaviorTabChange = () => {
  behaviorPage.value = 1
  loadBehaviorItems()
}

const loadSavedPlans = async () => {
  planLoading.value = true
  try {
    const res = await getItineraryPlans({
      page: 1,
      pageSize: 3
    })
    savedPlans.value = res.data?.list || []
    planTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载保存行程失败:', error)
    savedPlans.value = []
    planTotal.value = 0
  } finally {
    planLoading.value = false
  }
}

const loadFavoritePlans = async () => {
  favoritePlanLoading.value = true
  try {
    const res = await getFavoriteItineraryPlans({
      page: favoritePlanPage.value,
      pageSize: favoritePlanPageSize.value
    })
    favoritePlans.value = res.data?.list || []
    favoritePlanTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('加载收藏行程失败:', error)
    favoritePlans.value = []
    favoritePlanTotal.value = 0
  } finally {
    favoritePlanLoading.value = false
  }
}

const runFullTextSearch = async () => {
  const q = fullTextKeyword.value.trim()
  if (!q) {
    ElMessage.info('请输入关键词')
    return
  }
  fullTextLoading.value = true
  fullTextHasSearched.value = true
  try {
    const res = await searchLogs(q, fullTextPage.value, fullTextPageSize)
    fullTextResults.value = res.data?.list || []
    fullTextTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('全文检索失败:', error)
    fullTextResults.value = []
    fullTextTotal.value = 0
  } finally {
    fullTextLoading.value = false
  }
}

const handleFullTextPageChange = (page) => {
  fullTextPage.value = page
  runFullTextSearch()
}

const resetFullTextSearch = () => {
  fullTextKeyword.value = ''
  fullTextResults.value = []
  fullTextTotal.value = 0
  fullTextPage.value = 1
  fullTextHasSearched.value = false
}

const openFullTextResult = (item) => {
  router.push(`/log/${item.id}`)
}

const loadNotifications = async () => {
  notificationLoading.value = true
  try {
    const res = await getNotifications({
      onlyUnread: notificationOnlyUnread.value || undefined,
      page: notificationPage.value,
      pageSize: notificationPageSize.value
    })
    notifications.value = res.data?.list || []
    notificationTotal.value = res.data?.total || 0
    await loadNotificationUnreadCount()
  } catch (error) {
    console.error('加载通知失败:', error)
    notifications.value = []
    notificationTotal.value = 0
  } finally {
    notificationLoading.value = false
  }
}

const loadNotificationUnreadCount = async () => {
  try {
    const res = await getUnreadNotificationCount()
    notificationUnreadCount.value = res.data?.count || 0
  } catch (error) {
    notificationUnreadCount.value = 0
  }
}

const handleNotificationFilterChange = () => {
  notificationPage.value = 1
  loadNotifications()
}

const markSingleNotificationRead = async (item) => {
  if (!item?.id || item.read) return
  try {
    await markNotificationRead(item.id)
    item.read = true
    notificationUnreadCount.value = Math.max(0, notificationUnreadCount.value - 1)
    if (notificationOnlyUnread.value) {
      await loadNotifications()
    }
  } catch (error) {
    console.error('标记通知已读失败:', error)
  }
}

const handleMarkAllNotificationsRead = async () => {
  try {
    await markAllNotificationsRead()
    notifications.value.forEach(item => {
      item.read = true
    })
    notificationUnreadCount.value = 0
    if (notificationOnlyUnread.value) {
      await loadNotifications()
    }
    ElMessage.success('已全部标为已读')
  } catch (error) {
    console.error('全部已读失败:', error)
  }
}

const openSpot = (item) => {
  if (item?.id) {
    router.push(`/spot/${item.id}`)
  }
}

const openSavedPlan = (item) => {
  if (item?.id) {
    router.push({ path: '/itinerary', query: { planId: item.id } })
  }
}

const openNotification = async (item) => {
  if (!item) return
  await markSingleNotificationRead(item)
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

const openFavoritePlan = (item) => {
  if (item?.id) {
    router.push({ path: '/itinerary', query: { sharedPlanId: item.id } })
  }
}

const copyFavoritePlan = async (item) => {
  if (!item?.id) return
  favoritePlanActionLoading.value = `copy-${item.id}`
  try {
    const res = await copyItineraryPlan(item.id)
    item.copyCount = (item.copyCount || 0) + 1
    ElMessage.success('已复制到我的行程')
    if (res.data?.id) {
      router.push({ path: '/itinerary', query: { planId: res.data.id } })
    }
  } catch (error) {
    console.error('复制收藏行程失败:', error)
  } finally {
    favoritePlanActionLoading.value = ''
  }
}

const cancelFavoritePlan = async (item) => {
  if (!item?.id) return
  favoritePlanActionLoading.value = `favorite-${item.id}`
  try {
    await toggleItineraryFavorite(item.id)
    ElMessage.success('已取消收藏')
    favoritePlans.value = favoritePlans.value.filter(plan => plan.id !== item.id)
    favoritePlanTotal.value = Math.max(0, favoritePlanTotal.value - 1)
    if (favoritePlans.value.length === 0 && favoritePlanPage.value > 1) {
      favoritePlanPage.value -= 1
    }
    await loadFavoritePlans()
  } catch (error) {
    console.error('取消收藏行程失败:', error)
  } finally {
    favoritePlanActionLoading.value = ''
  }
}

const openItineraryPlan = (item) => {
  if (item?.itineraryPlanId) {
    router.push({ path: '/itinerary', query: { planId: item.itineraryPlanId } })
  }
}

const viewLogDetail = (item) => {
  if (item?.id) {
    router.push(`/log/${item.id}`)
  }
}

const handleEditDiary = (item) => {
  router.push({ path: '/diary', query: { edit: item.id } })
}

const handleDeleteDiary = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇日志吗？', '提示', {
      type: 'warning'
    })
    await deleteLog(id)
    ElMessage.success('删除成功')
    loadMyDiaries()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString('zh-CN')
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

onMounted(() => {
  loadProfile()
  loadMyDiaries()
  loadBehaviorItems()
  loadSavedPlans()
  loadFavoritePlans()
  loadNotifications()
  if (route.query.section === 'notifications') {
    nextTick(() => {
      notificationCardRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  }
})
</script>

<style lang="scss" scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card-title {
  font-size: 18px;
  color: #303133;
  margin: 0;
}

.profile-card {
  .profile-header {
    text-align: center;
    padding: 20px 0;
  }

  .avatar-section {
    position: relative;
    display: inline-block;
    margin-bottom: 16px;

    .avatar {
      border: 4px solid #f0f2f5;
      transition: transform 0.3s;
    }

    .avatar-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: white;
      opacity: 0;
      transition: opacity 0.3s;
      cursor: pointer;

      .el-icon {
        font-size: 24px;
        margin-bottom: 4px;
      }

      span {
        font-size: 12px;
      }
    }

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .username {
    font-size: 24px;
    color: #303133;
    margin-bottom: 8px;
  }

  .signature {
    color: #909399;
    font-size: 14px;
    line-height: 1.6;
  }

  .profile-stats {
    display: flex;
    justify-content: space-around;
    padding: 20px 0;

    .stat-item {
      text-align: center;

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #667eea;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }

    .stat-divider {
      width: 1px;
      background: #ebeef5;
    }
  }
}

.info-card {
  .card-title {
    font-size: 16px;
    margin-bottom: 16px;
  }
}

.diary-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .diary-list {
    min-height: 400px;
  }

  .empty-state {
    padding: 60px 0;
  }

  .diary-item-card {
    margin-bottom: 16px;

    .diary-item {
      .diary-item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
      }

      .diary-item-title {
        font-size: 18px;
        color: #303133;
        margin: 0;
      }

      .diary-item-actions {
        display: flex;
        gap: 8px;
      }

      .diary-item-content {
        color: #606266;
        line-height: 1.8;
        margin-bottom: 12px;
        white-space: pre-wrap;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
      }

      .diary-item-images {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
        gap: 12px;
        margin-bottom: 12px;
      }

      .diary-plan-card {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        padding: 12px;
        margin-bottom: 12px;
        border: 1px solid #dbeafe;
        border-radius: 8px;
        background: #f8fbff;
        cursor: pointer;

        span {
          color: #2563eb;
          font-size: 12px;
          font-weight: 600;
        }

        h4 {
          margin: 4px 0 6px;
          color: #263243;
        }

        p {
          margin: 0;
          color: #606266;
          line-height: 1.6;
        }
      }

      .diary-item-image {
        width: 100%;
        height: 120px;
        border-radius: 8px;
        cursor: pointer;
        background: #f5f7fa;
      }

      .diary-item-meta {
        display: flex;
        justify-content: space-between;
        color: #909399;
        font-size: 14px;
        margin-bottom: 12px;
      }

      .diary-item-location {
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .diary-item-tags {
        display: flex;
        flex-wrap: wrap;
      }
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 24px;
  }
}

.notification-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 14px;
  }
}

.fulltext-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .fulltext-search-row {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .fulltext-hint {
    margin: 8px 0 12px;
    color: #909399;
    font-size: 12px;
  }

  .fulltext-results {
    min-height: 40px;
  }

  .fulltext-item {
    padding: 12px 14px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all .15s;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, .12);
    }

    h4 {
      margin: 0 0 6px;
      font-size: 15px;
      color: #303133;
    }

    p {
      margin: 0 0 8px;
      color: #606266;
      font-size: 13px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .fulltext-meta {
      display: flex;
      gap: 14px;
      color: #909399;
      font-size: 12px;

      span {
        display: inline-flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.notification-header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.notification-list {
  min-height: 180px;
}

.notification-item {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;

  &:last-of-type {
    border-bottom: none;
  }

  &.unread {
    .notification-icon {
      color: #2563eb;
      background: #eff6ff;
    }

    .notification-main h4 {
      color: #111827;
      font-weight: 700;
    }
  }
}

.notification-icon {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #64748b;
  background: #f1f5f9;
}

.notification-main {
  min-width: 0;

  p {
    margin: 6px 0 8px;
    color: #606266;
    line-height: 1.6;
  }

  span {
    color: #909399;
    font-size: 13px;
  }
}

.notification-title-row {
  display: flex;
  align-items: center;
  gap: 10px;

  h4 {
    margin: 0;
    color: #303133;
    font-size: 16px;
  }
}

.behavior-card {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.plan-card {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.favorite-plan-card {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.plan-list {
  min-height: 180px;
}

.favorite-plan-list {
  min-height: 180px;
}

.plan-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;

  &:hover {
    .plan-info h4 {
      color: #409eff;
    }
  }

  &:last-of-type {
    border-bottom: none;
  }
}

.favorite-plan-item {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;

  &:hover {
    .plan-info h4 {
      color: #409eff;
    }
  }

  &:last-of-type {
    border-bottom: none;
  }
}

.plan-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  background: #eff6ff;
  font-size: 20px;
}

.favorite-plan-icon {
  color: #d97706;
  background: #fffbeb;
}

.plan-info {
  min-width: 0;

  h4 {
    margin: 0 0 8px;
    color: #303133;
    font-size: 16px;
  }

  p {
    margin: 0 0 10px;
    color: #606266;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #909399;
  font-size: 13px;
}

.more-plan-button {
  width: 100%;
  margin-top: 10px;
}

.favorite-plan-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
}

.behavior-list {
  min-height: 260px;
}

.behavior-item {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #f8fbff;
  }

  &:last-child {
    border-bottom: none;
  }
}

.behavior-cover {
  width: 140px;
  height: 88px;
  border-radius: 8px;
  background: #f5f7fa;
}

.behavior-cover-placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a8abb2;
  font-size: 13px;
  background: #f5f7fa;
}

.behavior-info {
  min-width: 0;

  h4 {
    margin: 0 0 8px;
    font-size: 17px;
    color: #303133;
  }

  p {
    margin: 0 0 10px;
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.behavior-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #909399;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

@media (max-width: 768px) {
  .notification-card .card-header,
  .notification-header-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .notification-item {
    grid-template-columns: 42px 1fr;
  }

  .notification-item > .el-button {
    grid-column: 1 / -1;
    justify-self: end;
  }

  .favorite-plan-item {
    grid-template-columns: 42px 1fr;
  }

  .favorite-plan-actions {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: flex-end;
  }

  .behavior-item {
    grid-template-columns: 1fr;
  }

  .behavior-cover {
    width: 100%;
    height: 160px;
  }
}
</style>
