package com.tourwise.service;

import com.tourwise.mapper.LogMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日记倒排索引（in-memory）—— 对应课设 (4)-⑦ 全文检索。
 *
 * 数据结构：term -> postings（包含该 term 的 logId 集合）。
 * 中文分词策略：bigram（连续两个字符）切分，避开了对外部分词器的依赖；
 * 同时对 ASCII 英文/数字按"小写后转 token"处理，命中英文关键词。
 *
 * 查询：把查询串同样切成 bigram + token 集合，分别在倒排表里取对应的 postings；
 * 然后用「最小集合优先 + 合并交集」选出同时命中所有 term 的 logId 集合。
 *
 * 时间：构建 O(总文本字符数)，查询 O(min(postings) * 查询 term 数)。
 * 优点：相比 SQL LIKE '%xxx%' 的全表扫描（每次都 O(N * L)），倒排索引把检索复杂度
 * 降到了 O(命中数)，且日记数量越大优势越明显。
 */
@Service
public class LogInvertedIndexService {

    private static final Logger log = LoggerFactory.getLogger(LogInvertedIndexService.class);

    private final LogMapper logMapper;

    /** term -> set of log ids. ConcurrentHashMap + 写锁保证增量更新线程安全。 */
    private final Map<String, Set<Long>> index = new ConcurrentHashMap<>();
    /** logId -> 该日记包含的 term 集合，用于删除/更新时反查清理 postings。 */
    private final Map<Long, Set<String>> termsByLog = new ConcurrentHashMap<>();

    public LogInvertedIndexService(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @PostConstruct
    public void buildOnStartup() {
        try {
            List<Map<String, Object>> rows = logMapper.findAllForIndex();
            for (Map<String, Object> row : rows) {
                Long id = toLong(row.get("id"));
                if (id == null) {
                    continue;
                }
                indexDocument(id, str(row.get("title")), str(row.get("content")));
            }
            log.info("日记倒排索引构建完成，文档数={}, term 数={}", termsByLog.size(), index.size());
        } catch (Exception e) {
            log.error("日记倒排索引构建失败，回退为空索引", e);
        }
    }

    /** 新增/更新一篇日记。 */
    public synchronized void indexDocument(Long logId, String title, String content) {
        if (logId == null) {
            return;
        }
        removeDocument(logId);
        Set<String> terms = tokenize(title, content);
        if (terms.isEmpty()) {
            return;
        }
        termsByLog.put(logId, terms);
        for (String term : terms) {
            index.computeIfAbsent(term, k -> ConcurrentHashMap.newKeySet()).add(logId);
        }
    }

    public synchronized void removeDocument(Long logId) {
        Set<String> terms = termsByLog.remove(logId);
        if (terms == null) {
            return;
        }
        for (String term : terms) {
            Set<Long> postings = index.get(term);
            if (postings == null) {
                continue;
            }
            postings.remove(logId);
            if (postings.isEmpty()) {
                index.remove(term);
            }
        }
    }

    /**
     * 检索：返回命中的 logId 列表（按 logId 倒序，即较新的日记优先）。
     * 多个 term 之间取交集，对应"必须同时包含"语义。
     */
    public List<Long> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Set<String> queryTerms = tokenize(keyword, null);
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        // 先取每个 term 的 postings；任意 term 缺失则没有命中
        List<Set<Long>> postingsList = new ArrayList<>(queryTerms.size());
        for (String term : queryTerms) {
            Set<Long> postings = index.get(term);
            if (postings == null || postings.isEmpty()) {
                return List.of();
            }
            postingsList.add(postings);
        }
        // 按大小升序排，从最小集合开始做交集，减少比较次数
        postingsList.sort((a, b) -> Integer.compare(a.size(), b.size()));

        Set<Long> result = new HashSet<>(postingsList.get(0));
        for (int i = 1; i < postingsList.size() && !result.isEmpty(); i++) {
            result.retainAll(postingsList.get(i));
        }
        if (result.isEmpty()) {
            return List.of();
        }

        List<Long> sorted = new ArrayList<>(result);
        sorted.sort(Collections.reverseOrder());
        if (sorted.size() > limit) {
            return new ArrayList<>(sorted.subList(0, limit));
        }
        return sorted;
    }

    public int documentCount() {
        return termsByLog.size();
    }

    public int termCount() {
        return index.size();
    }

    /**
     * 切词：
     * - 中文连续字符按 bigram（窗口=2）切分；
     * - ASCII 字母/数字小写后形成 token。
     * 把 title 视作高频字段一起处理（不做权重区分，仅命中）。
     */
    private static Set<String> tokenize(String title, String content) {
        Set<String> terms = new LinkedHashSet<>();
        appendTokens(terms, title);
        appendTokens(terms, content);
        return terms;
    }

    private static void appendTokens(Set<String> terms, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int n = text.length();
        StringBuilder asciiBuf = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < n; i++) {
            char c = chars[i];
            if (isAsciiAlnum(c)) {
                asciiBuf.append(Character.toLowerCase(c));
                continue;
            }
            if (asciiBuf.length() > 0) {
                terms.add(asciiBuf.toString());
                asciiBuf.setLength(0);
            }
            if (isCjk(c)) {
                // 单字也加入，便于单字符查询命中
                terms.add(String.valueOf(c));
                if (i + 1 < n && isCjk(chars[i + 1])) {
                    terms.add(new String(new char[]{c, chars[i + 1]}));
                }
            }
        }
        if (asciiBuf.length() > 0) {
            terms.add(asciiBuf.toString());
        }
    }

    private static boolean isAsciiAlnum(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isCjk(char c) {
        // 覆盖常用中日韩统一表意文字范围
        return c >= 0x4E00 && c <= 0x9FFF;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
