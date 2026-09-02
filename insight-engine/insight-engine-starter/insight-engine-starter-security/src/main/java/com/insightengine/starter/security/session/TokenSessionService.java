package com.insightengine.starter.security.session;

/**
 * 登录态校验服务接口（可选能力，与 {@link com.insightengine.starter.security.blacklist.TokenBlacklistService} 同模式）。
 *
 * <p>用途：改密/禁用/登出后使已签发 token 失效（踢人/强制重新登录，TD §6.1 登录态缓存）。
 * 登录时服务端缓存该用户当前 access token 摘要（{@code ie:auth:token:{userId}}），
 * 改密/禁用时删除缓存；本接口在认证过滤器里「每次请求」校验缓存存在且摘要匹配。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>只定义接口、不绑定实现，starter-security 保持对 Redis 的零依赖；
 *       引入 Redis 的服务（如 UMS）提供实现，未提供的服务退化为纯无状态 JWT 校验；</li>
 *   <li>与黑名单的粒度差异：黑名单按「单条 token」记（登出），登录态按「用户」记（踢人），
 *       删一个 key 即该用户全部会话失效。</li>
 * </ul>
 */
public interface TokenSessionService {

    /**
     * 判断该用户的某个 access token 是否为「当前有效登录态」签发的令牌。
     *
     * <p>登录成功时服务端会写入该用户当前 token 摘要；若用户改密/被禁用（登录态被删）或
     * 该 token 不是最新登录签发的（摘要不匹配），都应返回 {@code false} 拒绝访问。</p>
     *
     * @param userId 用户 ID（已从 token 载荷解析出）
     * @param token  完整 token 原文
     * @return true 表示登录态有效，允许继续；false 表示已失效，应拒绝
     */
    boolean isActive(Long userId, String token);
}
