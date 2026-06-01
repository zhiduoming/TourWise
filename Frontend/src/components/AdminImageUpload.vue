<template>
  <el-upload
    v-if="isAdmin && targetId"
    class="admin-image-upload"
    :show-file-list="false"
    accept="image/*"
    :http-request="handleUpload"
    @click.stop
  >
    <el-button type="primary" size="small" plain :loading="uploading">
      <el-icon><Upload /></el-icon>
      {{ label }}
    </el-button>
  </el-upload>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { uploadAdminImage } from '@/api/adminImage'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  targetType: {
    type: String,
    required: true
  },
  targetId: {
    type: [Number, String],
    required: true
  },
  label: {
    type: String,
    default: '上传图片'
  }
})

const emit = defineEmits(['success'])
const userStore = useUserStore()
const uploading = ref(false)
const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

const handleUpload = async ({ file }) => {
  if (!file) return
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  uploading.value = true
  try {
    const res = await uploadAdminImage(props.targetType, props.targetId, formData)
    const imageUrl = res.data?.imageUrl || res.data?.url
    emit('success', imageUrl)
    ElMessage.success('图片上传成功')
  } catch (error) {
    console.error('管理员图片上传失败:', error)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.admin-image-upload {
  display: inline-flex;
}
</style>
