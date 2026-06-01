ALTER TABLE travel_logs
  ADD COLUMN itinerary_plan_id BIGINT DEFAULT NULL COMMENT '关联保存行程' AFTER circle_id,
  ADD KEY idx_travel_logs_itinerary_plan (itinerary_plan_id),
  ADD CONSTRAINT fk_travel_logs_itinerary_plan
    FOREIGN KEY (itinerary_plan_id) REFERENCES itinerary_plans(id) ON DELETE SET NULL;
