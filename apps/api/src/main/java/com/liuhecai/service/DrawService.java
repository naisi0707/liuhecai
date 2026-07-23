package com.liuhecai.service;

import com.liuhecai.dto.DrawOverrideRequest;
import com.liuhecai.vo.DrawHistoryItemVO;
import com.liuhecai.vo.DrawResultVO;

import java.util.List;
import java.util.Map;

public interface DrawService {
    Map<String, Object> fetchAll();

    DrawResultVO latest(String lotteryType);

    List<DrawResultVO> latestAll();

    List<DrawHistoryItemVO> history(String lotteryType, Integer year, int pageSize);

    DrawResultVO override(DrawOverrideRequest request);
}
