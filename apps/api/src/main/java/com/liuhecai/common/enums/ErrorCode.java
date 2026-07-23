package com.liuhecai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_FAILED(40001, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录"),
    FORBIDDEN(40300, "无权限"),
    USER_NOT_FOUND(10001, "用户不存在"),
    USERNAME_EXISTS(10002, "用户名已存在"),
    TENANT_NOT_FOUND(30001, "租户不存在"),
    DOMAIN_NOT_FOUND(30002, "域名未绑定站点"),
    TENANT_DISABLED(30003, "站点已停用"),
    DOMAIN_EXISTS(30004, "域名已被占用"),
    AGENT_USERNAME_EXISTS(30005, "该站代理用户名已存在"),
    TOPIC_NOT_FOUND(40010, "资料帖不存在"),
    TOPIC_NOT_ON_SALE(40011, "资料帖未上架"),
    INSUFFICIENT_COINS(40012, "金币不足"),
    COIN_CONFLICT(40013, "余额变更冲突，请重试"),
    RECHARGE_NOT_FOUND(40020, "充值申请不存在"),
    RECHARGE_ALREADY_HANDLED(40021, "充值申请已处理"),
    BAD_CREDENTIALS(40101, "用户名或密码错误"),
    ACCOUNT_DISABLED(40102, "账号已停用"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;
}
