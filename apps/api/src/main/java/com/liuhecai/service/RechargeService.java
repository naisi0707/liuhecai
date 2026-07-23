package com.liuhecai.service;

import com.liuhecai.dto.RechargeCreateRequest;
import com.liuhecai.dto.RechargeRejectRequest;
import com.liuhecai.vo.RechargeVO;

import java.util.List;

public interface RechargeService {
    RechargeVO create(RechargeCreateRequest request);

    List<RechargeVO> listMine();

    List<RechargeVO> listForAgent(Integer status);

    RechargeVO approve(Long id);

    RechargeVO reject(Long id, RechargeRejectRequest request);
}
