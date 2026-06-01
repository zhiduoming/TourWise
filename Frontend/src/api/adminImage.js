import request from './request'

export function uploadAdminImage(targetType, targetId, formData) {
  return request({
    url: `/admin/images/${targetType}/${targetId}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
