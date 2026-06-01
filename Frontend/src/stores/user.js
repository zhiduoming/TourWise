import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getUserInfo, logout } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function loginAction(loginForm) {
    const res = await login(loginForm)
    token.value = res.data.token
    userInfo.value = res.data.user
    localStorage.setItem('token', res.data.token)
    return res
  }

  async function registerAction(registerForm) {
    const res = await register(registerForm)
    return res
  }

  async function getUserInfoAction() {
    const res = await getUserInfo()
    userInfo.value = res.data
    return res
  }

  async function logoutAction() {
    await logout()
    clearLoginState()
  }

  function clearLoginState() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    loginAction,
    registerAction,
    getUserInfoAction,
    logoutAction,
    clearLoginState
  }
})
