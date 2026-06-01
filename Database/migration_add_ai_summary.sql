USE tourist_system;

ALTER TABLE pois
  ADD COLUMN ai_summary TEXT DEFAULT NULL COMMENT 'AI生成的景点简介（DeepSeek缓存）',
  ADD COLUMN ai_summary_at DATETIME DEFAULT NULL COMMENT 'AI简介生成时间';
