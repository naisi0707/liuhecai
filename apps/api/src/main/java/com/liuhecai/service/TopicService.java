package com.liuhecai.service;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.dto.CoinGrantRequest;
import com.liuhecai.dto.TopicCreateRequest;
import com.liuhecai.dto.TopicStatusRequest;
import com.liuhecai.vo.PurchaseResultVO;
import com.liuhecai.vo.TopicVO;

import java.util.List;

public interface TopicService {
    TopicVO createByAgent(TopicCreateRequest request);

    List<TopicVO> listForAgent();

    TopicVO updateStatus(Long topicId, TopicStatusRequest request);

    PageResult<TopicVO> listPublic(Long viewerUserId, int page, int size);

    TopicVO detail(Long topicId, Long viewerUserId);

    PurchaseResultVO purchase(Long topicId);

    Integer grantCoins(CoinGrantRequest request);
}
