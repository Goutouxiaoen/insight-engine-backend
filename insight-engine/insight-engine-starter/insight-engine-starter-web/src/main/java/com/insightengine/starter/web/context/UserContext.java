package com.insightengine.starter.web.context;

/**
 * 用户上下文持有器。
 *
 * <p>基于 {@link ThreadLocal} 保存当前请求的 {@link LoginUser}，供业务代码
 * （数据权限拦截器、审计埋点、Service 层）随时读取当前登录用户身份。</p>
 *
 * <p>生命周期：由 {@code UserContextFilter} 在请求进入时解析网关下发的明文头并 {@code set}，
 * 请求结束时 {@code clear}。之所以必须 {@code clear}，是因为 Web 容器线程池复用线程，
 * 不清理会导致后续请求读到上一个请求的脏数据（串号）。</p>
 *
 * <p>设计要点：本类只提供静态访问方法，不直接 new，由过滤器统一管理生命周期。</p>
 */
public final class UserContext {

    private UserContext() {
        // 工具类禁止实例化
    }

    /** 线程本地持有器，隔离每个请求的用户身份 */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /**
     * 写入当前请求用户。
     */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /**
     * 读取当前请求用户。
     *
     * @return 未登录或过滤器未填充时为 {@code null}
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 读取当前用户 ID（便捷方法）。
     *
     * @return 未登录时为 {@code null}
     */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    /**
     * 读取当前租户 ID（便捷方法）。
     *
     * @return 未登录时为 {@code null}
     */
    public static Long getTenantId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getTenantId();
    }

    /**
     * 读取当前工作空间 ID（便捷方法）。
     *
     * @return 未登录或非空间级操作时为 {@code null}
     */
    public static Long getWorkspaceId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getWorkspaceId();
    }

    /**
     * 清理当前线程上下文，必须在请求结束时调用，防止线程复用导致数据串号。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
