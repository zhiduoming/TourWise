package com.tourwise.common;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopKSelectorTests {

    @Test
    void pickTopKBySingleScore() {
        TopKSelector<Integer> selector = new TopKSelector<Integer>(3, Comparator.naturalOrder());
        selector.offerAll(List.of(5, 1, 9, 4, 7, 2, 8, 3, 6));
        assertEquals(List.of(9, 8, 7), selector.toListDescending());
    }

    @Test
    void matchesFullSortOnRandomInput() {
        Random random = new Random(42);
        List<Integer> data = random.ints(2000, 0, 100_000).boxed().toList();
        int k = 10;

        TopKSelector<Integer> selector = new TopKSelector<Integer>(k, Comparator.naturalOrder());
        selector.offerAll(data);
        List<Integer> heapTopK = selector.toListDescending();

        List<Integer> sortedTopK = data.stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .toList();

        assertEquals(sortedTopK, heapTopK);
    }
}
