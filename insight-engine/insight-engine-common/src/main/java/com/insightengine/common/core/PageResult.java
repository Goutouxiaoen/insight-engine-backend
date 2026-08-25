package com.insightengine.common.core;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果。
 *
 * <p>所有「分页列表」接口的 {@code data} 统一使用本结构（TD §4.6 / IF §2.3）：</p>
 * <pre>
 * { "records": [...], "total": 120, "pageNum": 1, "pageSize": 10 }
 * </pre>
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页记录列表，空页返回空列表而非 null（IF §2.3 约定） */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页条数 */
    private int pageSize;

    /**
     * 私有构造：强制通过静态工厂方法创建，保证字段初始化一致。
     */
    private PageResult() {
    }

    /**
     * 构建分页结果。
     *
     * @param records  当前页记录（不可为 null）
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        // 防御：调用方传入 null 时降级为空列表，避免前端反序列化后 NPE
        result.records = records == null ? Collections.emptyList() : records;
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        return result;
    }

    /**
     * 构建空分页结果。
     *
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(Collections.emptyList(), 0L, pageNum, pageSize);
    }
}
