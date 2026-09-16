package com.insightengine.workspace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 切换当前工作空间请求体（IF §5.5）。
 */
@Data
public class WorkspaceSwitchRequest {

    /** 目标工作空间 ID（服务端校验当前用户必须是其成员） */
    @NotNull(message = "工作空间不能为空")
    private Long workspaceId;
}
