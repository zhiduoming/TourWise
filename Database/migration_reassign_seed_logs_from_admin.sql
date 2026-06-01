USE tourist_system;

-- chen 是后台和主测试账号，不应该默认挂载演示游记。
-- 保留演示游记本身，只把作者迁移到普通演示用户，避免个人中心误以为当前用户发布过这些内容。
UPDATE travel_logs
SET user_id = 2
WHERE id = 1
  AND user_id = 1
  AND title = '甲子钟广场拍照路线';

UPDATE travel_logs
SET user_id = 3
WHERE id = 4
  AND user_id = 1
  AND title = '通信主题博物馆参观';
