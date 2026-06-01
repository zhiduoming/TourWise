-- 北邮双校区路线图扩展数据
-- 执行顺序：schema.sql -> seed.sql -> campus_poi_seed.sql -> bupt_route_edges_seed.sql
-- 说明：本文件按“校区 + POI 名称”匹配节点，不依赖 pois 自增 ID。
--      路线边按双向步行图插入，用于 Dijkstra 最短路径、最优路线和路线演示。

USE tourist_system;

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  p_from.id,
  p_to.id,
  edge_data.distance_m,
  edge_data.duration_min,
  edge_data.transport_type,
  edge_data.congestion_factor,
  edge_data.is_indoor,
  edge_data.description
FROM (
  -- 北京邮电大学沙河校区：主轴、教学区、生活区、运动区、服务区
  SELECT 4 AS place_group_id, '南校门' AS from_name, '甲子钟广场' AS to_name, 520 AS distance_m, 8 AS duration_min, 'walk' AS transport_type, 1.05 AS congestion_factor, 0 AS is_indoor, '南校门进入校园后到达甲子钟广场' AS description
  UNION ALL SELECT 4, '北校门', '雁北园学生公寓', 260, 4, 'walk', 1.00, 0, '北校门到北侧宿舍区'
  UNION ALL SELECT 4, '东校门', '东体育场', 300, 5, 'walk', 1.00, 0, '东校门到东体育场'
  UNION ALL SELECT 4, '西校门', '快递中心', 240, 4, 'walk', 1.05, 0, '西校门到生活服务区'
  UNION ALL SELECT 4, '西校门', '校医院', 280, 5, 'walk', 1.05, 0, '西校门到校医院'
  UNION ALL SELECT 4, '甲子钟广场', '沙河校区图书馆', 350, 5, 'walk', 1.00, 0, '甲子钟广场到沙河校区图书馆'
  UNION ALL SELECT 4, '甲子钟广场', '教学实验综合楼 S 楼', 260, 4, 'walk', 1.00, 0, '甲子钟广场到教学实验综合楼 S 楼'
  UNION ALL SELECT 4, '甲子钟广场', '中心绿地', 160, 3, 'walk', 1.00, 0, '甲子钟广场到中心绿地'
  UNION ALL SELECT 4, '甲子钟广场', '下沉广场', 220, 4, 'walk', 1.10, 0, '甲子钟广场到下沉广场'
  UNION ALL SELECT 4, '沙河校区图书馆', '教学实验综合楼 N 楼', 220, 4, 'walk', 1.00, 0, '沙河校区图书馆到教学实验综合楼 N 楼'
  UNION ALL SELECT 4, '沙河校区图书馆', '共享自习室', 120, 2, 'indoor', 1.00, 1, '图书馆到共享自习室室内学习路线'
  UNION ALL SELECT 4, '共享自习室', '共享研讨室', 80, 2, 'indoor', 1.00, 1, '共享自习室到共享研讨室'
  UNION ALL SELECT 4, '教学实验综合楼 N 楼', '教学实验综合楼 S 楼', 160, 3, 'indoor', 1.00, 1, '教学实验综合楼 N 楼到 S 楼'
  UNION ALL SELECT 4, '教学实验综合楼 S 楼', '公共教学楼 S1 楼', 180, 3, 'indoor', 1.00, 1, '教学实验综合楼 S 楼到公共教学楼 S1 楼'
  UNION ALL SELECT 4, '公共教学楼 S1 楼', '公共教学楼 S2 楼', 120, 2, 'indoor', 1.00, 1, '公共教学楼 S1 楼到 S2 楼'
  UNION ALL SELECT 4, '公共教学楼 S2 楼', '公共教学楼 S3 楼', 120, 2, 'indoor', 1.00, 1, '公共教学楼 S2 楼到 S3 楼'
  UNION ALL SELECT 4, '教学实验综合楼 S 楼', '智慧教学楼', 240, 4, 'walk', 1.00, 0, '教学实验综合楼 S 楼到智慧教学楼'
  UNION ALL SELECT 4, '智慧教学楼', '综合办公楼', 230, 4, 'walk', 1.05, 0, '智慧教学楼到综合办公楼'
  UNION ALL SELECT 4, '综合办公楼', '师生综合服务大厅', 260, 5, 'walk', 1.05, 0, '综合办公楼到师生综合服务大厅'
  UNION ALL SELECT 4, '教学实验综合楼 N 楼', '网络空间安全学院楼', 260, 5, 'walk', 1.00, 0, '教学实验综合楼 N 楼到网络空间安全学院楼'
  UNION ALL SELECT 4, '网络空间安全学院楼', '数字媒体与设计艺术学院楼', 180, 3, 'walk', 1.00, 0, '网安学院楼到数媒学院楼'
  UNION ALL SELECT 4, '数字媒体与设计艺术学院楼', '工程训练中心', 220, 4, 'walk', 1.00, 0, '数媒学院楼到工程训练中心'
  UNION ALL SELECT 4, '工程训练中心', '东体育场', 360, 6, 'walk', 1.05, 0, '工程训练中心到东体育场'
  UNION ALL SELECT 4, '东体育场', '西体育场', 620, 10, 'walk', 1.10, 0, '东体育场到西体育场'
  UNION ALL SELECT 4, '东体育场', '篮球场', 180, 3, 'walk', 1.20, 0, '东体育场到篮球场'
  UNION ALL SELECT 4, '篮球场', '排球场', 90, 2, 'walk', 1.10, 0, '篮球场到排球场'
  UNION ALL SELECT 4, '篮球场', '网球场', 130, 2, 'walk', 1.10, 0, '篮球场到网球场'
  UNION ALL SELECT 4, '网球场', '羽毛球场', 90, 2, 'walk', 1.05, 0, '网球场到羽毛球场'
  UNION ALL SELECT 4, '羽毛球场', '乒乓球场', 70, 1, 'walk', 1.05, 0, '羽毛球场到乒乓球场'
  UNION ALL SELECT 4, '东体育场', '健身房', 240, 4, 'walk', 1.05, 0, '东体育场到健身房'
  UNION ALL SELECT 4, '健身房', '体育馆（在建）', 180, 3, 'walk', 1.00, 0, '健身房到体育馆'
  UNION ALL SELECT 4, '雁北园学生公寓', '北区食堂', 220, 4, 'walk', 1.25, 0, '雁北园学生公寓到北区食堂'
  UNION ALL SELECT 4, '雁南园学生公寓', '南区食堂', 220, 4, 'walk', 1.20, 0, '雁南园学生公寓到南区食堂'
  UNION ALL SELECT 4, '留学生公寓', '北区食堂', 300, 5, 'walk', 1.10, 0, '留学生公寓到北区食堂'
  UNION ALL SELECT 4, '北区食堂', '风味餐厅', 260, 4, 'walk', 1.25, 0, '北区食堂到风味餐厅'
  UNION ALL SELECT 4, '南区食堂', '风味餐厅', 160, 3, 'walk', 1.15, 0, '南区食堂到风味餐厅'
  UNION ALL SELECT 4, '风味餐厅', '教工餐厅', 120, 2, 'walk', 1.00, 0, '风味餐厅到教工餐厅'
  UNION ALL SELECT 4, '南区食堂', '咖啡店', 180, 3, 'walk', 1.00, 0, '南区食堂到咖啡店'
  UNION ALL SELECT 4, '咖啡店', '校园超市', 140, 2, 'walk', 1.05, 0, '咖啡店到校园超市'
  UNION ALL SELECT 4, '校园超市', '快递中心', 150, 3, 'walk', 1.20, 0, '校园超市到快递中心'
  UNION ALL SELECT 4, '快递中心', '洗衣房', 130, 2, 'walk', 1.10, 0, '快递中心到洗衣房'
  UNION ALL SELECT 4, '洗衣房', '理发店', 110, 2, 'walk', 1.00, 0, '洗衣房到理发店'
  UNION ALL SELECT 4, '打印店', '校园超市', 120, 2, 'walk', 1.05, 0, '打印店到校园超市'
  UNION ALL SELECT 4, '校医院', '师生综合服务大厅', 180, 3, 'walk', 1.05, 0, '校医院到师生综合服务大厅'
  UNION ALL SELECT 4, '师生综合服务大厅', '学生活动中心', 260, 4, 'walk', 1.05, 0, '师生综合服务大厅到学生活动中心'
  UNION ALL SELECT 4, '学生活动中心', '下沉广场', 180, 3, 'walk', 1.10, 0, '学生活动中心到下沉广场'
  UNION ALL SELECT 4, '中心绿地', '景观湖', 160, 3, 'walk', 1.00, 0, '中心绿地到景观湖'
  UNION ALL SELECT 4, '景观湖', '友谊林', 260, 4, 'walk', 1.00, 0, '景观湖到友谊林'
  UNION ALL SELECT 4, '友谊林', '北校门', 420, 7, 'walk', 1.00, 0, '友谊林到北校门'
  UNION ALL SELECT 4, '下沉广场', '南校门', 360, 6, 'walk', 1.10, 0, '下沉广场到南校门'

  -- 北京邮电大学西土城校区：主教学轴、实验区、宿舍食堂区、运动服务区、景观区
  UNION ALL SELECT 3, '西门', '主楼', 260, 4, 'walk', 1.05, 0, '西门到主楼'
  UNION ALL SELECT 3, '东门', '图书馆', 240, 4, 'walk', 1.05, 0, '东门到图书馆'
  UNION ALL SELECT 3, '南门', '标准田径场', 300, 5, 'walk', 1.00, 0, '南门到标准田径场'
  UNION ALL SELECT 3, '北门', '教七楼', 220, 4, 'walk', 1.00, 0, '北门到教七楼'
  UNION ALL SELECT 3, '主楼', '图书馆', 180, 3, 'walk', 1.10, 0, '主楼到图书馆'
  UNION ALL SELECT 3, '主楼', '行政办公楼', 150, 3, 'walk', 1.00, 0, '主楼到行政办公楼'
  UNION ALL SELECT 3, '主楼', '教一楼', 120, 2, 'walk', 1.10, 0, '主楼到教一楼'
  UNION ALL SELECT 3, '教一楼', '教二楼', 90, 2, 'indoor', 1.00, 1, '教一楼到教二楼'
  UNION ALL SELECT 3, '教二楼', '教三楼', 90, 2, 'indoor', 1.00, 1, '教二楼到教三楼'
  UNION ALL SELECT 3, '教三楼', '教四楼', 90, 2, 'indoor', 1.00, 1, '教三楼到教四楼'
  UNION ALL SELECT 3, '教四楼', '教五楼', 90, 2, 'indoor', 1.00, 1, '教四楼到教五楼'
  UNION ALL SELECT 3, '教五楼', '教六楼', 90, 2, 'indoor', 1.00, 1, '教五楼到教六楼'
  UNION ALL SELECT 3, '教六楼', '教七楼', 90, 2, 'indoor', 1.00, 1, '教六楼到教七楼'
  UNION ALL SELECT 3, '第一实验楼', '第二实验楼', 80, 2, 'indoor', 1.00, 1, '第一实验楼到第二实验楼'
  UNION ALL SELECT 3, '第二实验楼', '第三实验楼', 80, 2, 'indoor', 1.00, 1, '第二实验楼到第三实验楼'
  UNION ALL SELECT 3, '第三实验楼', '第四实验楼', 80, 2, 'indoor', 1.00, 1, '第三实验楼到第四实验楼'
  UNION ALL SELECT 3, '第四实验楼', '第五实验楼', 80, 2, 'indoor', 1.00, 1, '第四实验楼到第五实验楼'
  UNION ALL SELECT 3, '第五实验楼', '第六实验楼', 80, 2, 'indoor', 1.00, 1, '第五实验楼到第六实验楼'
  UNION ALL SELECT 3, '第六实验楼', '第七实验楼', 80, 2, 'indoor', 1.00, 1, '第六实验楼到第七实验楼'
  UNION ALL SELECT 3, '第七实验楼', '第八实验楼', 80, 2, 'indoor', 1.00, 1, '第七实验楼到第八实验楼'
  UNION ALL SELECT 3, '第八实验楼', '第九实验楼', 80, 2, 'indoor', 1.00, 1, '第八实验楼到第九实验楼'
  UNION ALL SELECT 3, '第九实验楼', '第十实验楼', 80, 2, 'indoor', 1.00, 1, '第九实验楼到第十实验楼'
  UNION ALL SELECT 3, '图书馆', '科研楼', 160, 3, 'walk', 1.00, 0, '图书馆到科研楼'
  UNION ALL SELECT 3, '科研楼', '未来学习大楼', 150, 3, 'walk', 1.00, 0, '科研楼到未来学习大楼'
  UNION ALL SELECT 3, '未来学习大楼', '逸夫楼', 140, 2, 'walk', 1.00, 0, '未来学习大楼到逸夫楼'
  UNION ALL SELECT 3, '逸夫楼', '北门', 260, 4, 'walk', 1.00, 0, '逸夫楼到北门'
  UNION ALL SELECT 3, '综合服务楼', '师生服务中心', 120, 2, 'walk', 1.05, 0, '综合服务楼到师生服务中心'
  UNION ALL SELECT 3, '师生服务中心', '后勤服务中心', 120, 2, 'walk', 1.05, 0, '师生服务中心到后勤服务中心'
  UNION ALL SELECT 3, '后勤服务中心', '校医院', 140, 3, 'walk', 1.00, 0, '后勤服务中心到校医院'
  UNION ALL SELECT 3, '校医院', '学一食堂', 180, 3, 'walk', 1.10, 0, '校医院到学一食堂'
  UNION ALL SELECT 3, '学一食堂', '学二食堂', 90, 2, 'walk', 1.20, 0, '学一食堂到学二食堂'
  UNION ALL SELECT 3, '学二食堂', '学三食堂', 90, 2, 'walk', 1.20, 0, '学二食堂到学三食堂'
  UNION ALL SELECT 3, '学三食堂', '清真食堂', 100, 2, 'walk', 1.10, 0, '学三食堂到清真食堂'
  UNION ALL SELECT 3, '清真食堂', '教工食堂', 120, 2, 'walk', 1.00, 0, '清真食堂到教工食堂'
  UNION ALL SELECT 3, '教工食堂', '风味餐厅', 120, 2, 'walk', 1.05, 0, '教工食堂到风味餐厅'
  UNION ALL SELECT 3, '风味餐厅', '时光广场', 180, 3, 'walk', 1.10, 0, '风味餐厅到时光广场'
  UNION ALL SELECT 3, '时光广场', '校训石广场', 120, 2, 'walk', 1.00, 0, '时光广场到校训石广场'
  UNION ALL SELECT 3, '校训石广场', '摩斯码路景观区', 120, 2, 'walk', 1.00, 0, '校训石广场到摩斯码路景观区'
  UNION ALL SELECT 3, '摩斯码路景观区', '中心绿地', 150, 3, 'walk', 1.00, 0, '摩斯码路景观区到中心绿地'
  UNION ALL SELECT 3, '中心绿地', '北湖景观区', 180, 3, 'walk', 1.00, 0, '中心绿地到北湖景观区'
  UNION ALL SELECT 3, '北湖景观区', '图书馆', 220, 4, 'walk', 1.00, 0, '北湖景观区到图书馆'
  UNION ALL SELECT 3, '学生 1 公寓', '学一食堂', 220, 4, 'walk', 1.25, 0, '学生 1 公寓到学一食堂'
  UNION ALL SELECT 3, '学生 8 公寓', '北门', 260, 4, 'walk', 1.05, 0, '学生 8 公寓到北门'
  UNION ALL SELECT 3, '学生 16 公寓', '北门', 340, 6, 'walk', 1.05, 0, '学生 16 公寓到北门'
  UNION ALL SELECT 3, '体育馆', '风雨操场', 150, 3, 'walk', 1.00, 0, '体育馆到风雨操场'
  UNION ALL SELECT 3, '风雨操场', '标准田径场', 180, 3, 'walk', 1.05, 0, '风雨操场到标准田径场'
  UNION ALL SELECT 3, '标准田径场', '篮球场', 160, 3, 'walk', 1.10, 0, '标准田径场到篮球场'
  UNION ALL SELECT 3, '篮球场', '排球场', 80, 2, 'walk', 1.05, 0, '篮球场到排球场'
  UNION ALL SELECT 3, '篮球场', '网球场', 120, 2, 'walk', 1.05, 0, '篮球场到网球场'
  UNION ALL SELECT 3, '网球场', '羽毛球场', 90, 2, 'walk', 1.05, 0, '网球场到羽毛球场'
  UNION ALL SELECT 3, '羽毛球场', '乒乓球场', 70, 1, 'walk', 1.00, 0, '羽毛球场到乒乓球场'
  UNION ALL SELECT 3, '校园快递中心', '菜鸟驿站', 80, 2, 'walk', 1.20, 0, '校园快递中心到菜鸟驿站'
  UNION ALL SELECT 3, '菜鸟驿站', '校园超市', 100, 2, 'walk', 1.10, 0, '菜鸟驿站到校园超市'
  UNION ALL SELECT 3, '校园超市', '教育超市', 90, 2, 'walk', 1.05, 0, '校园超市到教育超市'
  UNION ALL SELECT 3, '教育超市', '南门', 260, 4, 'walk', 1.00, 0, '教育超市到南门'
) AS edge_data
JOIN pois AS p_from
  ON p_from.place_group_id = edge_data.place_group_id
 AND p_from.name = edge_data.from_name
JOIN pois AS p_to
  ON p_to.place_group_id = edge_data.place_group_id
 AND p_to.name = edge_data.to_name
WHERE NOT EXISTS (
  SELECT 1
  FROM route_edges AS existing
  WHERE existing.from_poi_id = p_from.id
    AND existing.to_poi_id = p_to.id
    AND existing.transport_type = edge_data.transport_type
);

INSERT INTO route_edges
  (from_poi_id, to_poi_id, distance_m, duration_min, transport_type, congestion_factor, is_indoor, description)
SELECT
  p_to.id,
  p_from.id,
  edge_data.distance_m,
  edge_data.duration_min,
  edge_data.transport_type,
  edge_data.congestion_factor,
  edge_data.is_indoor,
  CONCAT(edge_data.to_name, '返回', edge_data.from_name)
FROM (
  -- 与上方无向边保持一致，第二段负责补充反向边。
  SELECT 4 AS place_group_id, '南校门' AS from_name, '甲子钟广场' AS to_name, 520 AS distance_m, 8 AS duration_min, 'walk' AS transport_type, 1.05 AS congestion_factor, 0 AS is_indoor
  UNION ALL SELECT 4, '北校门', '雁北园学生公寓', 260, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '东校门', '东体育场', 300, 5, 'walk', 1.00, 0
  UNION ALL SELECT 4, '西校门', '快递中心', 240, 4, 'walk', 1.05, 0
  UNION ALL SELECT 4, '西校门', '校医院', 280, 5, 'walk', 1.05, 0
  UNION ALL SELECT 4, '甲子钟广场', '沙河校区图书馆', 350, 5, 'walk', 1.00, 0
  UNION ALL SELECT 4, '甲子钟广场', '教学实验综合楼 S 楼', 260, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '甲子钟广场', '中心绿地', 160, 3, 'walk', 1.00, 0
  UNION ALL SELECT 4, '甲子钟广场', '下沉广场', 220, 4, 'walk', 1.10, 0
  UNION ALL SELECT 4, '沙河校区图书馆', '教学实验综合楼 N 楼', 220, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '沙河校区图书馆', '共享自习室', 120, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '共享自习室', '共享研讨室', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '教学实验综合楼 N 楼', '教学实验综合楼 S 楼', 160, 3, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '教学实验综合楼 S 楼', '公共教学楼 S1 楼', 180, 3, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '公共教学楼 S1 楼', '公共教学楼 S2 楼', 120, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '公共教学楼 S2 楼', '公共教学楼 S3 楼', 120, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 4, '教学实验综合楼 S 楼', '智慧教学楼', 240, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '智慧教学楼', '综合办公楼', 230, 4, 'walk', 1.05, 0
  UNION ALL SELECT 4, '综合办公楼', '师生综合服务大厅', 260, 5, 'walk', 1.05, 0
  UNION ALL SELECT 4, '教学实验综合楼 N 楼', '网络空间安全学院楼', 260, 5, 'walk', 1.00, 0
  UNION ALL SELECT 4, '网络空间安全学院楼', '数字媒体与设计艺术学院楼', 180, 3, 'walk', 1.00, 0
  UNION ALL SELECT 4, '数字媒体与设计艺术学院楼', '工程训练中心', 220, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '工程训练中心', '东体育场', 360, 6, 'walk', 1.05, 0
  UNION ALL SELECT 4, '东体育场', '西体育场', 620, 10, 'walk', 1.10, 0
  UNION ALL SELECT 4, '东体育场', '篮球场', 180, 3, 'walk', 1.20, 0
  UNION ALL SELECT 4, '篮球场', '排球场', 90, 2, 'walk', 1.10, 0
  UNION ALL SELECT 4, '篮球场', '网球场', 130, 2, 'walk', 1.10, 0
  UNION ALL SELECT 4, '网球场', '羽毛球场', 90, 2, 'walk', 1.05, 0
  UNION ALL SELECT 4, '羽毛球场', '乒乓球场', 70, 1, 'walk', 1.05, 0
  UNION ALL SELECT 4, '东体育场', '健身房', 240, 4, 'walk', 1.05, 0
  UNION ALL SELECT 4, '健身房', '体育馆（在建）', 180, 3, 'walk', 1.00, 0
  UNION ALL SELECT 4, '雁北园学生公寓', '北区食堂', 220, 4, 'walk', 1.25, 0
  UNION ALL SELECT 4, '雁南园学生公寓', '南区食堂', 220, 4, 'walk', 1.20, 0
  UNION ALL SELECT 4, '留学生公寓', '北区食堂', 300, 5, 'walk', 1.10, 0
  UNION ALL SELECT 4, '北区食堂', '风味餐厅', 260, 4, 'walk', 1.25, 0
  UNION ALL SELECT 4, '南区食堂', '风味餐厅', 160, 3, 'walk', 1.15, 0
  UNION ALL SELECT 4, '风味餐厅', '教工餐厅', 120, 2, 'walk', 1.00, 0
  UNION ALL SELECT 4, '南区食堂', '咖啡店', 180, 3, 'walk', 1.00, 0
  UNION ALL SELECT 4, '咖啡店', '校园超市', 140, 2, 'walk', 1.05, 0
  UNION ALL SELECT 4, '校园超市', '快递中心', 150, 3, 'walk', 1.20, 0
  UNION ALL SELECT 4, '快递中心', '洗衣房', 130, 2, 'walk', 1.10, 0
  UNION ALL SELECT 4, '洗衣房', '理发店', 110, 2, 'walk', 1.00, 0
  UNION ALL SELECT 4, '打印店', '校园超市', 120, 2, 'walk', 1.05, 0
  UNION ALL SELECT 4, '校医院', '师生综合服务大厅', 180, 3, 'walk', 1.05, 0
  UNION ALL SELECT 4, '师生综合服务大厅', '学生活动中心', 260, 4, 'walk', 1.05, 0
  UNION ALL SELECT 4, '学生活动中心', '下沉广场', 180, 3, 'walk', 1.10, 0
  UNION ALL SELECT 4, '中心绿地', '景观湖', 160, 3, 'walk', 1.00, 0
  UNION ALL SELECT 4, '景观湖', '友谊林', 260, 4, 'walk', 1.00, 0
  UNION ALL SELECT 4, '友谊林', '北校门', 420, 7, 'walk', 1.00, 0
  UNION ALL SELECT 4, '下沉广场', '南校门', 360, 6, 'walk', 1.10, 0
  UNION ALL SELECT 3, '西门', '主楼', 260, 4, 'walk', 1.05, 0
  UNION ALL SELECT 3, '东门', '图书馆', 240, 4, 'walk', 1.05, 0
  UNION ALL SELECT 3, '南门', '标准田径场', 300, 5, 'walk', 1.00, 0
  UNION ALL SELECT 3, '北门', '教七楼', 220, 4, 'walk', 1.00, 0
  UNION ALL SELECT 3, '主楼', '图书馆', 180, 3, 'walk', 1.10, 0
  UNION ALL SELECT 3, '主楼', '行政办公楼', 150, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '主楼', '教一楼', 120, 2, 'walk', 1.10, 0
  UNION ALL SELECT 3, '教一楼', '教二楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '教二楼', '教三楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '教三楼', '教四楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '教四楼', '教五楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '教五楼', '教六楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '教六楼', '教七楼', 90, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第一实验楼', '第二实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第二实验楼', '第三实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第三实验楼', '第四实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第四实验楼', '第五实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第五实验楼', '第六实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第六实验楼', '第七实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第七实验楼', '第八实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第八实验楼', '第九实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '第九实验楼', '第十实验楼', 80, 2, 'indoor', 1.00, 1
  UNION ALL SELECT 3, '图书馆', '科研楼', 160, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '科研楼', '未来学习大楼', 150, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '未来学习大楼', '逸夫楼', 140, 2, 'walk', 1.00, 0
  UNION ALL SELECT 3, '逸夫楼', '北门', 260, 4, 'walk', 1.00, 0
  UNION ALL SELECT 3, '综合服务楼', '师生服务中心', 120, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '师生服务中心', '后勤服务中心', 120, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '后勤服务中心', '校医院', 140, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '校医院', '学一食堂', 180, 3, 'walk', 1.10, 0
  UNION ALL SELECT 3, '学一食堂', '学二食堂', 90, 2, 'walk', 1.20, 0
  UNION ALL SELECT 3, '学二食堂', '学三食堂', 90, 2, 'walk', 1.20, 0
  UNION ALL SELECT 3, '学三食堂', '清真食堂', 100, 2, 'walk', 1.10, 0
  UNION ALL SELECT 3, '清真食堂', '教工食堂', 120, 2, 'walk', 1.00, 0
  UNION ALL SELECT 3, '教工食堂', '风味餐厅', 120, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '风味餐厅', '时光广场', 180, 3, 'walk', 1.10, 0
  UNION ALL SELECT 3, '时光广场', '校训石广场', 120, 2, 'walk', 1.00, 0
  UNION ALL SELECT 3, '校训石广场', '摩斯码路景观区', 120, 2, 'walk', 1.00, 0
  UNION ALL SELECT 3, '摩斯码路景观区', '中心绿地', 150, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '中心绿地', '北湖景观区', 180, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '北湖景观区', '图书馆', 220, 4, 'walk', 1.00, 0
  UNION ALL SELECT 3, '学生 1 公寓', '学一食堂', 220, 4, 'walk', 1.25, 0
  UNION ALL SELECT 3, '学生 8 公寓', '北门', 260, 4, 'walk', 1.05, 0
  UNION ALL SELECT 3, '学生 16 公寓', '北门', 340, 6, 'walk', 1.05, 0
  UNION ALL SELECT 3, '体育馆', '风雨操场', 150, 3, 'walk', 1.00, 0
  UNION ALL SELECT 3, '风雨操场', '标准田径场', 180, 3, 'walk', 1.05, 0
  UNION ALL SELECT 3, '标准田径场', '篮球场', 160, 3, 'walk', 1.10, 0
  UNION ALL SELECT 3, '篮球场', '排球场', 80, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '篮球场', '网球场', 120, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '网球场', '羽毛球场', 90, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '羽毛球场', '乒乓球场', 70, 1, 'walk', 1.00, 0
  UNION ALL SELECT 3, '校园快递中心', '菜鸟驿站', 80, 2, 'walk', 1.20, 0
  UNION ALL SELECT 3, '菜鸟驿站', '校园超市', 100, 2, 'walk', 1.10, 0
  UNION ALL SELECT 3, '校园超市', '教育超市', 90, 2, 'walk', 1.05, 0
  UNION ALL SELECT 3, '教育超市', '南门', 260, 4, 'walk', 1.00, 0
) AS edge_data
JOIN pois AS p_from
  ON p_from.place_group_id = edge_data.place_group_id
 AND p_from.name = edge_data.from_name
JOIN pois AS p_to
  ON p_to.place_group_id = edge_data.place_group_id
 AND p_to.name = edge_data.to_name
WHERE NOT EXISTS (
  SELECT 1
  FROM route_edges AS existing
  WHERE existing.from_poi_id = p_to.id
    AND existing.to_poi_id = p_from.id
    AND existing.transport_type = edge_data.transport_type
);
