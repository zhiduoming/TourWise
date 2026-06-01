import request from './request'

// 获取圈子列表（已加入的和其他圈子）
export function getCircleList(keyword = '') {
  return request({
    url: '/circle/list',
    method: 'get',
    params: { keyword }
  })
}

// 获取圈子详情
export function getCircleDetail(id) {
  return request({
    url: `/circle/${id}`,
    method: 'get'
  })
}

// 创建圈子
export function createCircle(data) {
  return request({
    url: '/circle/create',
    method: 'post',
    data
  })
}

// 加入圈子
export function joinCircle(id) {
  return request({
    url: `/circle/${id}/join`,
    method: 'post'
  })
}

// 退出圈子
export function leaveCircle(id) {
  return request({
    url: `/circle/${id}/leave`,
    method: 'post'
  })
}

// 获取圈子日志列表
export function getCircleLogs(id, page = 1, pageSize = 10) {
  return request({
    url: `/circle/${id}/logs`,
    method: 'get',
    params: { page, pageSize }
  })
}

// 创建圈子日志
export function createLog(id, data) {
  return request({
    url: `/circle/${id}/logs`,
    method: 'post',
    data
  })
}

// 点赞圈子日志
export function toggleLike(logId) {
  return request({
    url: `/circle/${logId}/like`,
    method: 'post'
  })
}

// 获取日志评论列表
export function getComments(logId) {
  return request({
    url: `/circle/${logId}/comments`,
    method: 'get'
  })
}

// 发布评论
export function createComment(logId, data) {
  return request({
    url: `/circle/${logId}/comments`,
    method: 'post',
    data
  })
}
