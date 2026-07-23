package com.liuhecai.common.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 六合彩生肖 / 五行（与原站 zhibo 报码页 getShengxiao / getWuxing 一致）。
 */
public final class ZodiacHelper {
    private static final String[] ZODIACS = {
            "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
    };

    private static final String[] WUXING_CYCLE = {
            "金", "金", "火", "火", "木", "木", "土", "土", "金", "金",
            "火", "火", "水", "水", "土", "土", "金", "金", "木", "木",
            "水", "水", "土", "土", "火", "火", "木", "木", "水", "水",
            "金", "金", "火", "火", "木", "木", "土", "土", "金", "金",
            "火", "火", "水", "水", "土", "土", "金", "金", "木", "木",
            "水", "水", "土", "土", "火", "火", "木", "木", "水", "水"
    };

    private ZodiacHelper() {
    }

    public static String ofNumber(int number) {
        return ofNumber(number, LocalDate.now().getYear());
    }

    public static String ofNumber(int number, int yearly) {
        if (number < 1 || number > 49) {
            return "?";
        }
        int val = Math.floorMod(yearly - 1924, 12);
        String[] list = new String[12];
        int idx = 0;
        for (int i = val; i >= 0; i--) {
            list[idx++] = ZODIACS[i];
        }
        for (int i = 11; i > val; i--) {
            list[idx++] = ZODIACS[i];
        }
        return list[(number - 1) % 12];
    }

    public static String wuxingOf(int number, int yearly) {
        if (number < 1 || number > 49) {
            return "?";
        }
        int i = Math.floorMod(yearly - 1922 - number - 1, 60);
        return WUXING_CYCLE[i];
    }

    public static List<String> ofNumbers(List<Integer> numbers, int special) {
        return ofNumbers(numbers, special, LocalDate.now().getYear());
    }

    public static List<String> ofNumbers(List<Integer> numbers, int special, int yearly) {
        List<String> list = new ArrayList<>();
        for (Integer n : numbers) {
            list.add(ofNumber(n, yearly));
        }
        list.add(ofNumber(special, yearly));
        return list;
    }

    public static List<String> wuxingsOf(List<Integer> numbers, int special, int yearly) {
        List<String> list = new ArrayList<>();
        for (Integer n : numbers) {
            list.add(wuxingOf(n, yearly));
        }
        list.add(wuxingOf(special, yearly));
        return list;
    }
}
