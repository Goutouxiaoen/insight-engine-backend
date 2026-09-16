package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建组织请求体（IF §5.1）。
 *
 * <p>编码在同租户内唯一（DB 唯一索引 {@code uk_org_code_tenant}），Service 层先查友好提示、
 * 索引兜底防并发。</p>
 */
@Data
public class OrgCreateRequest {

    /** 组织名称 */
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 128, message = "组织名称长度不能超过 128 个字符")
    private String name;

    /** 组织编码（小写字母开头，2~64 位字母/数字/中划线） */
    @NotBlank(message = "组织编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$", message = "编码须以小写字母开头，2~64 位字母/数字/中划线")
    private String code;
}
