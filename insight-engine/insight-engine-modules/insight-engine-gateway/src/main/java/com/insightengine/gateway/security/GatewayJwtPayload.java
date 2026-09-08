package com.insightengine.gateway.security;

import java.util.List;

/**
 * 网关解析出的访问令牌载荷。
 *
 * <p>字段与 UMS 签发时的 JWT Claim（TD §7.2 扩展）一一对应，
 * 网关只取「下发明文头」所需的身份字段，不需要 perms（权限授权由业务服务自行处理）。</p>
 *
 * @param userId      用户 ID（JWT {@code sub}）
 * @param tenantId    租户 ID（Claim {@code tenant_id}）
 * @param workspaceId 当前工作空间 ID（Claim {@code ws_id}，组织级管理员可为 null）
 * @param roles       角色编码列表（Claim {@code roles}）
 */
public record GatewayJwtPayload(Long userId, Long tenantId, Long workspaceId, List<String> roles) {
}
