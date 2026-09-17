package com.insightengine.starter.security.workspace;

/**
 * 空间维度权限缓存的**失效契约**（与 {@link WorkspacePermissionChecker} 的查询职责分开）。
 *
 * <h3>为什么要独立成一个接口</h3>
 * <ul>
 *   <li><b>依赖倒置</b>：业务 Service 只依赖接口，不直接依赖 workspace 的实现类
 *       （此前 {@code MemberServiceImpl}/{@code WorkspaceServiceImpl} 直接注入 {@code WorkspacePermissionCheckerImpl}，
 *       既暴露实现细节，也让"谁能失效缓存"没有契约约束）；</li>
 *   <li><b>无缓存实现可空实现</b>：若某服务用本地缓存或干脆不缓存，显式空实现即可（实现本接口时必须写出这个决定，
 *       而不是"忘了失效"这种静默错误）。</li>
 * </ul>
 *
 * <h3>调用纪律（重要）</h3>
 * <p><b>撤销类操作（移除成员 / 降角色 / 删除空间）必须在数据库变更之前调用失效</b>：
 * 若先改库再失效、而失效抛异常，就会出现"库里已经没权限、缓存里还有"的窗口（最长 = 缓存 TTL），
 * 属于**安全语义被破坏**。失败方向要选「宁可不改库」：先失效 → 失败则整个操作失败（库未动、权限状态仍然正确）。</p>
 *
 * @see com.insightengine.common.annotation.WorkspacePermission
 */
public interface WorkspacePermissionCacheInvalidator {

    /**
     * 失效某成员在某个空间的权限缓存（成员增删 / 改角色时调用）。
     */
    void evict(Long userId, Long workspaceId);

    /**
     * 失效某个空间的全部成员权限缓存（删除空间 / 批量变更时调用）。
     */
    void evictWorkspace(Long workspaceId);
}
