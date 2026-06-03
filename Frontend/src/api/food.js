import request from './request'

// 获取美食列表
export function getFoodList(params) {
  return request({
    url: '/food/list',
    method: 'get',
    params
  })
}

// 获取美食分页列表（含 total）
export function getFoodPagedList(params) {
  return request({
    url: '/food/paged-list',
    method: 'get',
    params
  })
}

// 获取/生成美食 AI 简介（force=true 强制重新生成）
export function getFoodAiSummary(id, force = false) {
  return request({
    url: `/food/list/${id}/ai-summary`,
    method: 'get',
    params: force ? { force: true } : {}
  })
}

// 获取美食详情
export function getFoodDetail(id) {
  return request({
    url: `/food/list/${id}`,
    method: 'get'
  })
}

// 获取美食推荐
export function getFoodRecommendations() {
  return request({
    url: '/food/recommend',
    method: 'get'
  })
}

// 提交美食评价
export function submitFoodReview(data) {
  return request({
    url: '/food/review',
    method: 'post',
    data
  })
}
