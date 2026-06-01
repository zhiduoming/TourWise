import request from './request'

// 获取可进行路线规划的具体景点/校区
export function getRouteScopes() {
  return request({
    url: '/route/scopes',
    method: 'get'
  })
}

// 获取可用于景点之间规划的大景点
export function getRouteSpots() {
  return request({
    url: '/route/spots',
    method: 'get'
  })
}

// 获取两点间最短路径
export function getShortestPath(data) {
  return request({
    url: '/route/shortest',
    method: 'post',
    data
  })
}

// 获取多景点最优路线 (TSP)
export function getOptimalRoute(data) {
  return request({
    url: '/route/optimal',
    method: 'post',
    data
  })
}

// 获取楼宇内路径
export function getIndoorPath(data) {
  return request({
    url: '/route/indoor',
    method: 'post',
    data
  })
}

// 获取所有兴趣点
export function getPOIs(params) {
  return request({
    url: '/route/pois',
    method: 'get',
    params
  })
}

// 获取景点/校区路线底图
export function getRouteMap(placeGroupId) {
  return request({
    url: `/route/maps/${placeGroupId}`,
    method: 'get'
  })
}

// 管理员上传景点/校区路线底图
export function uploadRouteMap(placeGroupId, formData) {
  return request({
    url: `/admin/maps/${placeGroupId}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取高德地图配置
export function getAmapConfig() {
  return request({
    url: '/route/amap/config',
    method: 'get'
  })
}

// 高德真实道路路线规划
export function planAmapRoute(data) {
  return request({
    url: '/route/amap/plan',
    method: 'post',
    data
  })
}

// 解析当前位置对应的大景点或内部 POI
export function resolveLocation(data) {
  return request({
    url: '/route/location/resolve',
    method: 'post',
    data
  })
}

// 保存路线规划记录
export function saveRouteRecord(data) {
  return request({
    url: '/route/records',
    method: 'post',
    data
  })
}

// 获取我的路线规划记录
export function getRouteRecords(params) {
  return request({
    url: '/route/records',
    method: 'get',
    params
  })
}

// 删除我的路线规划记录
export function deleteRouteRecord(id) {
  return request({
    url: `/route/records/${id}`,
    method: 'delete'
  })
}

// 管理员读取景点内部路网
export function getAdminRouteGraph(placeGroupId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}`,
    method: 'get'
  })
}

// 管理员保存景点内部路网
export function saveAdminRouteGraph(placeGroupId, data) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}`,
    method: 'put',
    data
  })
}

// 管理员保存景点内部路网，并可同步创建版本快照
export function saveAdminRouteGraphWithVersion(placeGroupId, data) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/with-version`,
    method: 'put',
    data
  })
}

// 管理员导出当前路网 JSON
export function exportAdminRouteGraph(placeGroupId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/export`,
    method: 'get'
  })
}

// 管理员导入路网 JSON
export function importAdminRouteGraph(placeGroupId, data) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/import`,
    method: 'post',
    data
  })
}

// 管理员查看路网版本列表
export function getAdminRouteGraphVersions(placeGroupId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/versions`,
    method: 'get'
  })
}

// 管理员检查当前路网数据质量
export function getAdminRouteGraphQuality(placeGroupId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/quality`,
    method: 'get'
  })
}

// 管理员一键清理可自动判定的异常路线边
export function cleanupAdminRouteGraphQuality(placeGroupId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/quality/cleanup`,
    method: 'post'
  })
}

// 管理员在路网标注页补充业务 POI
export function createAdminRouteGraphPoi(placeGroupId, data) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/pois`,
    method: 'post',
    data
  })
}

// 管理员在路网标注页删除误加业务 POI
export function deleteAdminRouteGraphPoi(placeGroupId, poiId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/pois/${poiId}`,
    method: 'delete'
  })
}

// 管理员创建路网版本快照
export function createAdminRouteGraphVersion(placeGroupId, data) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/versions`,
    method: 'post',
    data
  })
}

// 管理员查看某个路网版本的完整快照
export function getAdminRouteGraphVersionSnapshot(placeGroupId, versionId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/versions/${versionId}/snapshot`,
    method: 'get'
  })
}

// 管理员对比某个版本和当前路网的差异
export function diffAdminRouteGraphVersion(placeGroupId, versionId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/versions/${versionId}/diff`,
    method: 'get'
  })
}

// 管理员恢复某个路网版本
export function restoreAdminRouteGraphVersion(placeGroupId, versionId) {
  return request({
    url: `/admin/route-graphs/${placeGroupId}/versions/${versionId}/restore`,
    method: 'post'
  })
}
