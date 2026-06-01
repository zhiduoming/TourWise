import request from './request'

// 搜索设施
export function searchFacilities(params) {
  return request({
    url: '/search/facilities',
    method: 'get',
    params
  })
}

// 获取设施详情（景点详情）
export function getFacilityDetail(id) {
  return request({
    url: `/search/facility/${id}`,
    method: 'get'
  })
}

// 获取设施类型列表
export function getFacilityTypes() {
  return request({
    url: '/search/types',
    method: 'get'
  })
}

// 获取景点标签
export function getSearchTags() {
  return request({
    url: '/search/tags',
    method: 'get'
  })
}

// 获取热门推荐（用于景点列表）
export function getHotTop10() {
  return request({
    url: '/recommend/hot-top10',
    method: 'get'
  })
}

// 获取景点 AI 简介（第一次调用生成并缓存，后续直接返回；force=true 时重新生成）
export function getSpotAiSummary(id, force = false) {
  return request({
    url: `/search/facility/${id}/ai-summary`,
    method: 'get',
    params: force ? { force: true } : {}
  })
}
