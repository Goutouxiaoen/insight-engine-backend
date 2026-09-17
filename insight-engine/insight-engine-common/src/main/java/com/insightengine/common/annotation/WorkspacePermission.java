package com.insightengine.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 空间维度权限校验注解（**第二层鉴权**，TD §7.5 / IF §3.0）。
 *
 * <h3>为什么需要它（与 {@code @PreAuthorize} 的分工）</h3>
 * <pre>
 * 第一层 @PreAuthorize("hasAuthority('member:create')")
 *   回答：这个「动作类别」我能不能做？—— 判据来自 token 的 perms（用户维度全量、跨空间聚合）
 *   局限：**回答不了「在哪个空间做」**。同一用户在 A 空间是 ws_admin（能加成员）、
 *         在 B 空间只是 end_user（不能），但两个空间的 token perms 是完全一样的。
 * 第二层 @WorkspacePermission("member:create")
 *   回答：我在「当前/指定的这个空间」里有没有这个权限？—— 服务端按 (userId, workspaceId) 查成员关系
 *         （ie_member → ie_role → ie_role_permission → ie_permission）
 * </pre>
 *
 * <p>两层必须都过：第一层挡住"越权动作类别"（垂直越权），第二层挡住"跨空间操作"
 * （水平越权，最容易漏的一类）。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 1) 目标空间在方法参数里 → 用 SpEL 指定（否则默认取当前 token 的 ws_id）
 * @WorkspacePermission(value = "member:create", workspaceIdExpr = "#request.workspaceId")
 * @PostMapping("/invite")
 * public Result<Long> invite(@RequestBody MemberInviteRequest request) { ... }
 *
 * // 2) 目标空间在方法参数里的简单场景
 * @WorkspacePermission(value = "ws:write", workspaceIdExpr = "#id")
 * @PutMapping("/{id}")
 * public Result<Void> update(@PathVariable Long id, ...) { ... }
 *
 * // 3) 目标空间需要先查库才知道（如按 memberId 反查其所属空间）→ 注解不适用，
 * //    在业务方法内注入 WorkspacePermissionChecker 手动判定（保持检查显式可见）。
 * }</pre>
 *
 * <p>注意：<b>组织级动作不要用本注解</b>（如 `ws:create` / `ws:delete` —— 空间增删属组织级，
 * 与"在哪个空间"无关，用 {@code @PreAuthorize} 即可）。</p>
 *
 * @see com.insightengine.common.constant.AuthQuerySql 空间维度查询 SQL（二次判定用，非 token 口径）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WorkspacePermission {

    /**
     * 权限编码（IF §6 权限字典），如 {@code member:create}。
     */
    String value();

    /**
     * 目标工作空间 ID 的 SpEL 表达式（相对方法参数求值），如 {@code "#request.workspaceId"}、{@code "#id"}。
     *
     * <p>留空表示使用<b>当前请求 token 的 {@code ws_id}</b>（即"当前所处空间"）。</p>
     */
    String workspaceIdExpr() default "";

    /**
     * 校验失败时的业务提示（默认由切面给出通用提示）。
     */
    String message() default "";
}
