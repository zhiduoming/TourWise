package com.tourwise.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 基于"大小为 K 的最小堆"的 TopK 选择器。
 *
 * 课设要求 (3)-③、(5)-①：用户通常只看前 10 个，要求不经过完全排序选出前 K。
 * 思路：维护一个大小为 K 的最小堆，遍历 N 个候选时若当前元素大于堆顶就替换堆顶。
 * 时间复杂度 O(N log K)，远优于完全排序 O(N log N)；空间 O(K)。
 * 最后按"得分降序"输出堆内元素即可。
 */
public final class TopKSelector<T> {

    private final int k;
    private final Comparator<T> comparator;
    private final PriorityQueue<T> heap;

    /**
     * @param k          需要的 Top 数量
     * @param comparator "得分从小到大"的比较器，堆顶始终是当前 Top-K 中得分最低者
     */
    public TopKSelector(int k, Comparator<T> comparator) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive");
        }
        this.k = k;
        this.comparator = comparator;
        this.heap = new PriorityQueue<>(k, comparator);
    }

    public void offer(T item) {
        if (item == null) {
            return;
        }
        if (heap.size() < k) {
            heap.offer(item);
            return;
        }
        // 堆顶是 Top-K 里最小的；若新元素更大才替换
        if (comparator.compare(item, heap.peek()) > 0) {
            heap.poll();
            heap.offer(item);
        }
    }

    public void offerAll(Iterable<T> items) {
        if (items == null) {
            return;
        }
        for (T item : items) {
            offer(item);
        }
    }

    /** 按得分从高到低输出当前 TopK。 */
    public List<T> toListDescending() {
        List<T> snapshot = new ArrayList<>(heap);
        snapshot.sort(comparator.reversed());
        return snapshot;
    }

    public int size() {
        return heap.size();
    }
}
