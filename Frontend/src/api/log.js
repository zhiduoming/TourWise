import request from './request'

// 获取日志列表（支持多种过滤）
export function getLogList(params = {}) {
  return request({
    url: '/log/list',
    method: 'get',
    params
  })
}

// 获取当前登录用户自己的日志
export function getMyLogList(params = {}) {
  return request({
    url: '/log/my',
    method: 'get',
    params
  })
}

// 获取日志详情
export function getLogDetail(id) {
  return request({
    url: `/log/${id}`,
    method: 'get'
  })
}

// 创建日志
export function createLog(data) {
  return request({
    url: '/log/create',
    method: 'post',
    data
  })
}

// 删除日志
export function deleteLog(id) {
  return request({
    url: `/log/${id}`,
    method: 'delete'
  })
}

// 点赞日志
export function toggleLike(id) {
  return request({
    url: `/log/${id}/like`,
    method: 'post'
  })
}

// 举报日志或评论
export function createReport(data) {
  return request({
    url: '/reports',
    method: 'post',
    data
  })
}
