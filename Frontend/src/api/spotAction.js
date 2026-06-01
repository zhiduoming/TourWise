import request from './request'

export function getSpotActionState(targetId, targetType = 'poi') {
  return request({
    url: `/user/spot-actions/${targetId}`,
    method: 'get',
    params: { targetType }
  })
}

export function recordSpotBrowse(data) {
  return request({
    url: '/user/spot-actions/browse',
    method: 'post',
    data
  })
}

export function toggleSpotFavorite(data) {
  return request({
    url: '/user/spot-actions/favorite',
    method: 'post',
    data
  })
}

export function toggleSpotWant(data) {
  return request({
    url: '/user/spot-actions/want',
    method: 'post',
    data
  })
}

export function toggleSpotVisited(data) {
  return request({
    url: '/user/spot-actions/visited',
    method: 'post',
    data
  })
}

export function dislikeSpot(data) {
  return request({
    url: '/user/spot-actions/dislike',
    method: 'post',
    data
  })
}

export function getSpotActionList(params) {
  return request({
    url: '/user/spot-actions/list',
    method: 'get',
    params
  })
}
