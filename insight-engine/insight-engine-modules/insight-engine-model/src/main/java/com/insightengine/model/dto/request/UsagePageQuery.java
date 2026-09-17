package com.insightengine.model.dto.request;

import com.insightengine.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 用量分页查询参数（IF §7.8）。
 *
 * <p>时间范围用**日期**（含首含尾，闭区间），服务端按"自然日"换算为时间戳——与 IF §7.8 示例
 * （{@code start=2026-08-01&end=2026-08-25}）一致，避免前端还要拼时分秒。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UsagePageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 模型 ID（可选；不传 = 全部模型） */
    private Long modelId;

    /** 起始日期（含），格式 {@code yyyy-MM-dd} */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate start;

    /** 结束日期（含），格式 {@code yyyy-MM-dd} */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end;
}
