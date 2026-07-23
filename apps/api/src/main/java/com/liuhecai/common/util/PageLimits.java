package com.liuhecai.common.util;

public final class PageLimits {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageLimits() {
    }

    public static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static int clampPage(int page) {
        return Math.max(page, 1);
    }
}
