import request from './request'

export function getAdminDashboard() {
  return request({
    url: '/admin/dashboard',
    method: 'get'
  })
}

export function getRouteQuality() {
  return request({
    url: '/admin/route-quality',
    method: 'get'
  })
}

export function getAdminSpots(params) {
  return request({
    url: '/admin/spots',
    method: 'get',
    params
  })
}

export function createAdminSpot(data) {
  return request({
    url: '/admin/spots',
    method: 'post',
    data
  })
}

export function updateAdminSpot(id, data) {
  return request({
    url: `/admin/spots/${id}`,
    method: 'put',
    data
  })
}

export function updateAdminSpotStatus(id, status) {
  return request({
    url: `/admin/spots/${id}/status`,
    method: 'patch',
    params: { status }
  })
}

export function getAdminSpotPois(spotId, params) {
  return request({
    url: `/admin/spots/${spotId}/pois`,
    method: 'get',
    params
  })
}

export function createAdminPoi(spotId, data) {
  return request({
    url: `/admin/spots/${spotId}/pois`,
    method: 'post',
    data
  })
}

export function updateAdminPoi(poiId, data) {
  return request({
    url: `/admin/pois/${poiId}`,
    method: 'put',
    data
  })
}

export function updateAdminPoiStatus(poiId, status) {
  return request({
    url: `/admin/pois/${poiId}/status`,
    method: 'patch',
    params: { status }
  })
}

export function getAdminPoiCategories() {
  return request({
    url: '/admin/poi-categories',
    method: 'get'
  })
}

export function getAdminTags(params) {
  return request({
    url: '/admin/tags',
    method: 'get',
    params
  })
}

export function getAdminSpotTags(spotId) {
  return request({
    url: `/admin/spots/${spotId}/tags`,
    method: 'get'
  })
}

export function updateAdminSpotTags(spotId, tags) {
  return request({
    url: `/admin/spots/${spotId}/tags`,
    method: 'put',
    data: { tags }
  })
}

export function getContentReports(params) {
  return request({
    url: '/admin/reports',
    method: 'get',
    params
  })
}

export function handleContentReport(id, data) {
  return request({
    url: `/admin/reports/${id}`,
    method: 'patch',
    data
  })
}

export function getAdminLogs(params) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params
  })
}

export function deleteAdminLog(id) {
  return request({
    url: `/admin/logs/${id}`,
    method: 'delete'
  })
}

export function getAdminLogComments(logId) {
  return request({
    url: `/admin/logs/${logId}/comments`,
    method: 'get'
  })
}

export function deleteAdminComment(commentId) {
  return request({
    url: `/admin/logs/comments/${commentId}`,
    method: 'delete'
  })
}

export function getAdminCircles(params) {
  return request({
    url: '/admin/circles',
    method: 'get',
    params
  })
}

export function updateAdminCircle(id, data) {
  return request({
    url: `/admin/circles/${id}`,
    method: 'put',
    data
  })
}

export function updateAdminCircleStatus(id, status) {
  return request({
    url: `/admin/circles/${id}/status`,
    method: 'patch',
    params: { status }
  })
}
