import request from './request'

// 获取推荐列表
export function getRecommendations(params) {
  return request({
    url: '/recommend/list',
    method: 'get',
    params
  })
}

// 获取热门推荐 Top10
export function getHotTop10() {
  return request({
    url: '/recommend/hot-top10',
    method: 'get'
  })
}

// 提交用户评分
export function submitRating(data) {
  return request({
    url: '/recommend/rating',
    method: 'post',
    data
  })
}
