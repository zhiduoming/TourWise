import request from './request'

// 获取美食列表
export function getFoodList(params) {
  return request({
    url: '/food/list',
    method: 'get',
    params
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
