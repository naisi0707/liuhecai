package com.liuhecai.draw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuhecai.config.DrawProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDrawSourceTest {

    @Test
    void fetchLatest_fromZhiboApis() {
        DrawProperties props = new DrawProperties();
        props.setSources(Map.of(
                "MACAU_NEW", List.of("https://zhibo.77kj.vip/kj/data/am.js"),
                "MACAU_OLD", List.of("https://zhibo.77kj.vip/kj/data/48am.js"),
                "HK", List.of("https://zhibo.77kj.vip/kj/data/xg.js")
        ));
        HttpDrawSource source = new HttpDrawSource(RestClient.builder(), new ObjectMapper(), props);

        Optional<FetchedDraw> macauNew = source.fetchLatest("MACAU_NEW");
        assertTrue(macauNew.isPresent(), "MACAU_NEW should parse");
        assertEquals(6, macauNew.get().numbers().size());
        assertTrue(macauNew.get().zodiacs().size() >= 7, "zodiacs should cover 6+special");
        assertTrue(macauNew.get().zodiacs().stream().allMatch(z -> z != null && !z.isBlank()));

        Optional<FetchedDraw> macauOld = source.fetchLatest("MACAU_OLD");
        assertTrue(macauOld.isPresent(), "MACAU_OLD should parse");

        Optional<FetchedDraw> hk = source.fetchLatest("HK");
        assertTrue(hk.isPresent(), "HK should parse");
        assertEquals("HK", hk.get().lotteryType());
    }
}
