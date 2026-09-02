package com.insightengine.ums.dto.response;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户分页列表项（IF §4.1）。
 *
 * <p>{@code phone} 已脱敏输出；时间字段 ISO-8601 UTC（IF §2.5）。</p>
 */
@Data
public class UserPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号（脱敏后） */
    private String phone;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
