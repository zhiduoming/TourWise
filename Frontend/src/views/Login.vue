<template>
  <div class="login-container">
    <div class="bg-decor">
      <div class="bg-blob blob-1"></div>
      <div class="bg-blob blob-2"></div>
      <div class="bg-blob blob-3"></div>
      <div class="bg-grid"></div>
      <div class="bg-particles">
        <span v-for="n in 18" :key="n" :style="particleStyle(n)"></span>
      </div>
    </div>
    <el-card class="login-card">
      <div class="login-header">
        <el-icon :size="48" color="#667eea"><Location /></el-icon>
        <h2>个性化旅游系统</h2>
        <p>欢迎登录</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="密码登录" name="password">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-width="80px"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="用户名/手机号"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
              <el-link type="primary" style="margin-left: auto">忘记密码？</el-link>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                style="width: 100%"
                :loading="loading"
                @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="80px"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input
                v-model="registerForm.phone"
                placeholder="手机号"
                prefix-icon="Phone"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                style="width: 100%"
                :loading="loading"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <p>登录即代表您同意</p>
        <el-link type="primary">用户协议</el-link>
        <span>和</span>
        <el-link type="primary">隐私政策</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('password')
const loading = ref(false)

const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerForm = reactive({
  username: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
    loading.value = true
    await userStore.loginAction(loginForm)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

const particleStyle = (n) => {
  const seedA = (n * 73) % 100
  const seedB = (n * 131) % 100
  const seedC = (n * 197) % 100
  const size = 6 + (seedA % 10)
  return {
    left: `${seedA}%`,
    top: `${seedB}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDuration: `${12 + (seedC % 10)}s`,
    animationDelay: `${-(seedB % 8)}s`,
    opacity: 0.25 + (seedC % 40) / 100
  }
}

const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()
    loading.value = true
    await userStore.registerAction(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'password'
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(120deg, #1e3a8a, #4338ca, #7c3aed, #db2777);
  background-size: 400% 400%;
  animation: gradientShift 18s ease infinite;
}

@keyframes gradientShift {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  mix-blend-mode: screen;
  animation: blobFloat 16s ease-in-out infinite;
}

.blob-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -100px;
  background: radial-gradient(circle, #38bdf8, transparent 70%);
}

.blob-2 {
  width: 480px;
  height: 480px;
  bottom: -160px;
  right: -120px;
  background: radial-gradient(circle, #f472b6, transparent 70%);
  animation-delay: -6s;
}

.blob-3 {
  width: 360px;
  height: 360px;
  top: 40%;
  left: 55%;
  background: radial-gradient(circle, #facc15, transparent 70%);
  opacity: 0.35;
  animation-delay: -10s;
}

@keyframes blobFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(40px, -30px) scale(1.08); }
  66%      { transform: translate(-30px, 40px) scale(0.95); }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
}

.bg-particles {
  position: absolute;
  inset: 0;
}

.bg-particles span {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
  animation: particleRise linear infinite;
}

@keyframes particleRise {
  0%   { transform: translateY(20vh) scale(0.6); opacity: 0; }
  20%  { opacity: 0.6; }
  100% { transform: translateY(-110vh) scale(1.1); opacity: 0; }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 450px;
  padding: 28px 20px;
  border-radius: 18px !important;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  box-shadow:
    0 24px 60px rgba(15, 23, 42, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.25) inset;
  animation: cardIn 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.97); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 24px;
    color: #303133;
    margin: 16px 0 8px;
  }

  p {
    color: #909399;
    font-size: 14px;
  }
}

.login-tabs {
  margin-bottom: 24px;
}

.login-footer {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;

  p {
    margin-bottom: 8px;
  }
}
</style>
