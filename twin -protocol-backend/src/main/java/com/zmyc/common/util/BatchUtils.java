package com.zmyc.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatchUtils {

    private static final int DEFAULT_BATCH_SIZE = 500;

    /**
     * 分批执行，默认每批500条
     */
    public static <T> void execute(List<T> list, Consumer<List<T>> action) {
        execute(list, DEFAULT_BATCH_SIZE, action);
    }

    /**
     * 分批执行，指定每批大小
     */
    public static <T> void execute(List<T> list, int batchSize, Consumer<List<T>> action) {
        if (list == null || list.isEmpty()) return;
        for (int i = 0; i < list.size(); i += batchSize) {
            action.accept(list.subList(i, Math.min(i + batchSize, list.size())));
        }
    }

    private BatchUtils() {
    }
}
