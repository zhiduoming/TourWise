import request from './request'

export function generateItinerary(data) {
  return request({
    url: '/itinerary/generate',
    method: 'post',
    data
  })
}

export function saveItineraryPlan(data) {
  return request({
    url: '/itinerary/plans',
    method: 'post',
    data
  })
}

export function getItineraryPlans(params) {
  return request({
    url: '/itinerary/plans',
    method: 'get',
    params
  })
}

export function getFavoriteItineraryPlans(params) {
  return request({
    url: '/itinerary/plans/favorites',
    method: 'get',
    params
  })
}

export function getItineraryPlanDetail(id) {
  return request({
    url: `/itinerary/plans/${id}`,
    method: 'get'
  })
}

export function getSharedItineraryPlan(id) {
  return request({
    url: `/itinerary/shared-plans/${id}`,
    method: 'get'
  })
}

export function getHotSharedItineraryPlans(params) {
  return request({
    url: '/itinerary/shared-plans/hot',
    method: 'get',
    params
  })
}

export function copyItineraryPlan(id) {
  return request({
    url: `/itinerary/plans/${id}/copy`,
    method: 'post'
  })
}

export function toggleItineraryFavorite(id) {
  return request({
    url: `/itinerary/plans/${id}/favorite`,
    method: 'post'
  })
}

export function deleteItineraryPlan(id) {
  return request({
    url: `/itinerary/plans/${id}`,
    method: 'delete'
  })
}
