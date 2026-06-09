<template>
  <el-upload
    v-model:file-list="fileList"
    :action="uploadAction"
    :headers="uploadHeaders"
    :data="{ scene }"
    :on-success="handleSuccess"
    :on-remove="handleRemove"
    :before-upload="beforeUpload"
    multiple
    :limit="limit"
    list-type="picture-card"
    accept="image/*"
  >
    <el-icon><Plus /></el-icon>
    <template #tip>
      <div class="upload-tip">
        最多上传 {{ limit }} 张，单张不超过 5MB；图片会用于生成 AIGC 旅游动画。
      </div>
    </template>
  </el-upload>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  scene: { type: String, default: 'log' },
  limit: { type: Number, default: 9 }
})
const emit = defineEmits(['update:modelValue'])

const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/upload`
const uploadHeaders = ref({})
const refreshHeaders = () => {
  const token = localStorage.getItem('token')
  uploadHeaders.value = token ? { Authorization: `Bearer ${token}` } : {}
}
refreshHeaders()

const fileList = ref([])

watch(
  () => props.modelValue,
  (urls) => {
    if (!Array.isArray(urls) || urls.length === 0) {
      fileList.value = []
      return
    }
    const known = new Set(fileList.value.map((f) => f.url))
    urls.forEach((url, idx) => {
      if (!known.has(url)) {
        fileList.value.push({ name: `image-${idx}`, url, status: 'success' })
      }
    })
  },
  { immediate: true }
)

const collectUrls = () =>
  fileList.value
    .map((file) => file.response?.data?.url || file.url)
    .filter(Boolean)

const handleSuccess = (response, file) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.message || '图片上传失败')
    fileList.value = fileList.value.filter((item) => item.uid !== file.uid)
    return
  }
  file.url = response.data?.url
  emit('update:modelValue', collectUrls())
}

const handleRemove = () => {
  emit('update:modelValue', collectUrls())
}

const beforeUpload = (file) => {
  refreshHeaders()
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张图片不能超过 5MB')
    return false
  }
  return true
}

const reset = () => {
  fileList.value = []
  emit('update:modelValue', [])
}

defineExpose({ reset })
</script>

<style lang="scss" scoped>
.upload-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
