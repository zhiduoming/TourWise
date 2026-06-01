import request from './request'

// 获取用户资料
export function getProfile() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

// 获取当前用户兴趣画像
export function getPreferenceProfile() {
  return request({
    url: '/user/profile/preferences',
    method: 'get'
  })
}

// 更新用户资料
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

// 上传头像
export function uploadAvatar(formData) {
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除头像
export function deleteAvatar() {
  return request({
    url: '/user/avatar',
    method: 'delete'
  })
}
