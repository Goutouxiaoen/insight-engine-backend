package com.insightengine.ums.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建角色请求体（IF §6.2）。
 *
 * <p>{@code code} 用正则约束为「小写字母/数字/下划线」，保证角色编码可安全地
 * 进入 JWT roles Claim 与 @PreAuthorize 权限表达式（避免空格/特殊字符破坏解析）。</p>
 */
@Data
public class RoleCreateRequest {

    /** 角色编码（如 hr_operator） */
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "角色编码须为小写字母/数字/下划线，且以字母开头")
    @Size(max = 64, message = "角色编码长度不能超过 64")
    private String code;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称长度不能超过 128")
    private String name;

    /** 数据范围：ALL/ORG/WS/SELF */
    @Pattern(regexp = "^(ALL|ORG|WS|SELF)$", message = "数据范围取值非法")
    private String scope;

    /** 角色描述 */
    @Size(max = 500, message = "角色描述长度不能超过 500")
    private String description;

    /** 关联权限 ID 列表（可为空） */
    private List<Long> permissionIds;
}
