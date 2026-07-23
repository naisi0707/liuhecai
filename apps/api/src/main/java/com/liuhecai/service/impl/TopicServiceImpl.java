package com.liuhecai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuhecai.auth.AuthContext;
import com.liuhecai.auth.AuthUser;
import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.enums.LotteryType;
import com.liuhecai.common.exception.BusinessException;
import com.liuhecai.dto.CoinGrantRequest;
import com.liuhecai.dto.TopicCreateRequest;
import com.liuhecai.dto.TopicStatusRequest;
import com.liuhecai.entity.CoinLog;
import com.liuhecai.entity.Topic;
import com.liuhecai.entity.TopicOrder;
import com.liuhecai.entity.User;
import com.liuhecai.mapper.CoinLogMapper;
import com.liuhecai.mapper.TopicMapper;
import com.liuhecai.mapper.TopicOrderMapper;
import com.liuhecai.mapper.UserMapper;
import com.liuhecai.service.HtmlSanitizeService;
import com.liuhecai.service.TopicService;
import com.liuhecai.service.UserCoinService;
import com.liuhecai.tenant.TenantContext;
import com.liuhecai.vo.PurchaseResultVO;
import com.liuhecai.vo.TopicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_OFFLINE = 3;

    private final TopicMapper topicMapper;
    private final TopicOrderMapper topicOrderMapper;
    private final UserMapper userMapper;
    private final CoinLogMapper coinLogMapper;
    private final HtmlSanitizeService htmlSanitizeService;
    private final UserCoinService userCoinService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicVO createByAgent(TopicCreateRequest request) {
        AuthUser agent = requireAgent();
        String lotteryType = normalizeLottery(request.getLotteryType());
        int status = request.getStatus() == null ? STATUS_PENDING : request.getStatus();
        if (status < 0 || status > 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 无效");
        }
        LocalDateTime now = LocalDateTime.now();
        Topic topic = new Topic();
        topic.setTenantId(agent.getTenantId());
        topic.setTitle(request.getTitle().trim());
        topic.setLotteryType(lotteryType);
        topic.setIssueNo(request.getIssueNo().trim());
        topic.setPlayType(StringUtils.hasText(request.getPlayType()) ? request.getPlayType().trim() : "综合");
        topic.setPrice(request.getPrice());
        topic.setContent(htmlSanitizeService.sanitize(request.getContent()));
        if (StringUtils.hasText(request.getPreviewContent())) {
            topic.setPreviewContent(htmlSanitizeService.sanitize(request.getPreviewContent()));
        }
        topic.setViewCount(0);
        topic.setStatus(status);
        topic.setCreatedBy(agent.getId());
        topic.setCreatedAt(now);
        topic.setUpdatedAt(now);
        topicMapper.insert(topic);
        return toVo(topic, false, true, 0, null, null);
    }

    @Override
    public List<TopicVO> listForAgent() {
        requireAgent();
        return topicMapper.selectList(new LambdaQueryWrapper<Topic>()
                        .orderByDesc(Topic::getCreatedAt))
                .stream()
                .map(t -> toVo(t, false, true, null, null, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicVO updateStatus(Long topicId, TopicStatusRequest request) {
        requireAgent();
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        topic.setStatus(request.getStatus());
        topic.setUpdatedAt(LocalDateTime.now());
        topicMapper.updateById(topic);
        return toVo(topic, false, true, null, null, null);
    }

    @Override
    public List<TopicVO> listPublic(Long viewerUserId) {
        List<Topic> topics = topicMapper.selectList(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getStatus, STATUS_APPROVED)
                .orderByDesc(Topic::getCreatedAt));
        Set<Long> purchasedIds = purchasedTopicIds(viewerUserId, topics.stream().map(Topic::getId).toList());
        return topics.stream()
                .map(t -> {
                    boolean purchased = purchasedIds.contains(t.getId()) || isFree(t);
                    return toVo(t, purchased, false, null, null, null);
                })
                .collect(Collectors.toList());
    }

    @Override
    public TopicVO detail(Long topicId, Long viewerUserId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        if (!Objects.equals(topic.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_ON_SALE);
        }
        int views = topic.getViewCount() == null ? 0 : topic.getViewCount();
        topic.setViewCount(views + 1);
        topicMapper.updateById(topic);

        boolean purchased = isFree(topic) || hasPurchased(viewerUserId, topicId);
        Long purchaseCount = topicOrderMapper.selectCount(new LambdaQueryWrapper<TopicOrder>()
                .eq(TopicOrder::getTopicId, topicId));
        // 同彩种同期上下帖，避免串到测试帖
        Topic prev = topicMapper.selectOne(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getStatus, STATUS_APPROVED)
                .eq(Topic::getLotteryType, topic.getLotteryType())
                .eq(Topic::getIssueNo, topic.getIssueNo())
                .lt(Topic::getId, topicId)
                .orderByDesc(Topic::getId)
                .last("LIMIT 1"));
        Topic next = topicMapper.selectOne(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getStatus, STATUS_APPROVED)
                .eq(Topic::getLotteryType, topic.getLotteryType())
                .eq(Topic::getIssueNo, topic.getIssueNo())
                .gt(Topic::getId, topicId)
                .orderByAsc(Topic::getId)
                .last("LIMIT 1"));
        return toVo(topic, purchased, purchased,
                purchaseCount == null ? 0 : purchaseCount.intValue(), prev, next);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseResultVO purchase(Long topicId) {
        AuthUser authUser = requireUser();
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_FOUND);
        }
        if (!Objects.equals(topic.getStatus(), STATUS_APPROVED)) {
            throw new BusinessException(ErrorCode.TOPIC_NOT_ON_SALE);
        }
        if (isFree(topic) || hasPurchased(authUser.getId(), topicId)) {
            User user = userMapper.selectById(authUser.getId());
            return PurchaseResultVO.builder()
                    .topicId(String.valueOf(topicId))
                    .price(topic.getPrice())
                    .coinBalance(user == null ? 0 : user.getCoinBalance())
                    .alreadyPurchased(true)
                    .build();
        }

        User user = userMapper.selectById(authUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        int price = topic.getPrice() == null ? 0 : topic.getPrice();
        int balance = user.getCoinBalance() == null ? 0 : user.getCoinBalance();
        if (balance < price) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_COINS);
        }

        user.setCoinBalance(balance - price);
        int updated = userMapper.updateById(user);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COIN_CONFLICT);
        }

        TopicOrder order = new TopicOrder();
        order.setTenantId(authUser.getTenantId());
        order.setUserId(authUser.getId());
        order.setTopicId(topicId);
        order.setPrice(price);
        order.setCreatedAt(LocalDateTime.now());
        try {
            topicOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发下另一请求已下单：幂等返回，不重复扣（本事务回滚）
            throw new BusinessException(ErrorCode.COIN_CONFLICT, "请重试查看购买状态");
        }

        CoinLog log = new CoinLog();
        log.setTenantId(authUser.getTenantId());
        log.setUserId(authUser.getId());
        log.setChangeAmount(-price);
        log.setBalanceAfter(balance - price);
        log.setBizType("PURCHASE");
        log.setBizId(String.valueOf(topicId));
        log.setRemark("购帖:" + topic.getTitle());
        log.setCreatedAt(LocalDateTime.now());
        coinLogMapper.insert(log);

        return PurchaseResultVO.builder()
                .topicId(String.valueOf(topicId))
                .price(price)
                .coinBalance(balance - price)
                .alreadyPurchased(false)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer grantCoins(CoinGrantRequest request) {
        AuthUser agent = requireAgent();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim())
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String remark = StringUtils.hasText(request.getRemark()) ? request.getRemark() : "代理加币";
        return userCoinService.adjustCoins(user.getId(), request.getAmount(), "GRANT", remark, agent.getId());
    }

    private Set<Long> purchasedTopicIds(Long userId, List<Long> topicIds) {
        if (userId == null || topicIds == null || topicIds.isEmpty()) {
            return Set.of();
        }
        return topicOrderMapper.selectList(new LambdaQueryWrapper<TopicOrder>()
                        .eq(TopicOrder::getUserId, userId)
                        .in(TopicOrder::getTopicId, topicIds))
                .stream()
                .map(TopicOrder::getTopicId)
                .collect(Collectors.toSet());
    }

    private boolean hasPurchased(Long userId, Long topicId) {
        if (userId == null) {
            return false;
        }
        Long count = topicOrderMapper.selectCount(new LambdaQueryWrapper<TopicOrder>()
                .eq(TopicOrder::getUserId, userId)
                .eq(TopicOrder::getTopicId, topicId));
        return count != null && count > 0;
    }

    private boolean isFree(Topic topic) {
        return topic.getPrice() == null || topic.getPrice() <= 0;
    }

    private TopicVO toVo(Topic topic, boolean purchased, boolean contentVisible,
                         Integer purchaseCount, Topic prev, Topic next) {
        return TopicVO.builder()
                .id(String.valueOf(topic.getId()))
                .title(topic.getTitle())
                .lotteryType(topic.getLotteryType())
                .issueNo(topic.getIssueNo())
                .playType(topic.getPlayType())
                .price(topic.getPrice())
                .status(topic.getStatus())
                .purchased(purchased)
                .contentVisible(contentVisible)
                .previewContent(topic.getPreviewContent())
                .content(contentVisible ? topic.getContent() : null)
                .viewCount(topic.getViewCount() == null ? 0 : topic.getViewCount())
                .purchaseCount(purchaseCount)
                .prevTopicId(prev == null ? null : String.valueOf(prev.getId()))
                .prevTopicTitle(prev == null ? null : prev.getTitle())
                .nextTopicId(next == null ? null : String.valueOf(next.getId()))
                .nextTopicTitle(next == null ? null : next.getTitle())
                .createdAt(topic.getCreatedAt())
                .build();
    }

    private String normalizeLottery(String lotteryType) {
        if (!StringUtils.hasText(lotteryType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "lotteryType 不能为空");
        }
        String type = lotteryType.trim().toUpperCase();
        try {
            LotteryType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的彩种");
        }
        return type;
    }

    private AuthUser requireAgent() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        TenantContext.set(user.getTenantId());
        return user;
    }

    private AuthUser requireUser() {
        AuthUser user = AuthContext.get();
        if (user == null || user.getTenantId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
