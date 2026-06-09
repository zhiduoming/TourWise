package com.tourwise.service;

import com.tourwise.mapper.LogMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogInvertedIndexServiceTests {

    @Test
    void searchHitsBigramAndAscii() {
        LogMapper mapper = mock(LogMapper.class);
        when(mapper.findAllForIndex()).thenReturn(Collections.emptyList());
        LogInvertedIndexService svc = new LogInvertedIndexService(mapper);
        svc.indexDocument(1L, "北邮西土城校区漫游", "在主楼附近散步，路过教三和图书馆");
        svc.indexDocument(2L, "沙河校区的食堂", "今天吃了 KFC 套餐，味道不错");
        svc.indexDocument(3L, "颐和园一日游", "湖光山色，体验北京的春天");

        // bigram 命中：含"北邮" 的只有 #1
        assertEquals(List.of(1L), svc.search("北邮", 10));
        // 英文 token 命中：含 kfc 的只有 #2
        assertEquals(List.of(2L), svc.search("kfc", 10));
        // 多 term 取交集：含"北京" 与 "春天" 的只有 #3
        assertEquals(List.of(3L), svc.search("北京 春天", 10));
        // 单字命中：图（仅 #1）
        assertEquals(List.of(1L), svc.search("图", 10));
        // 没命中
        assertTrue(svc.search("巴黎", 10).isEmpty());
    }

    @Test
    void removeAndUpdate() {
        LogMapper mapper = mock(LogMapper.class);
        when(mapper.findAllForIndex()).thenReturn(Collections.emptyList());
        LogInvertedIndexService svc = new LogInvertedIndexService(mapper);
        svc.indexDocument(1L, "颐和园", "北京春天");
        svc.removeDocument(1L);
        assertTrue(svc.search("颐和园", 10).isEmpty());

        svc.indexDocument(1L, "新标题", "更新后的内容");
        assertEquals(List.of(1L), svc.search("更新", 10));
    }
}
