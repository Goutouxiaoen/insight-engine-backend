package com.insightengine.common.core;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询请求基类。
 *
 * <p>所有「分页列表」接口的查询参数 DTO 继承本类，统一分页字段（TD §4.6 / IF §2.3）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>{@code pageSize} 上限 100，防止恶意超大分页拖垮数据库与内存；</li>
 *   <li>字段带默认值，未传参时走兜底，避免空指针；</li>
 *   <li>不在此类做 JSR-303 校验注解，交由各子类按需声明，保持基类零依赖、可复用。</li>
 * </ul>
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认起始页码 */
    public static final int DEFAULT_PAGE_NUM = 1;
    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 每页条数上限 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 页码，从 1 开始 */
    private int pageNum = DEFAULT_PAGE_NUM;

    /** 每页条数，默认 10，上限 100 */
    private int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 获取安全的每页条数：若传入值超过上限则截断为上限，避免越界。
     * <p>子类或 Service 在真正分页前调用本方法，确保 pageSize 落在 [1, 100] 区间。</p>
     */
    public int getSafePageSize() {
        if (this.pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(this.pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 获取安全的页码：页码至少为 1。
     */
    public int getSafePageNum() {
        return Math.max(this.pageNum, DEFAULT_PAGE_NUM);
    }
}
