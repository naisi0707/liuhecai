package com.liuhecai.service;

public interface OpAuditService {

    void record(String action, String targetType, String targetId, String detail);
}
