package com.insightengine.ums.service;

import com.insightengine.common.core.PageResult;
import com.insightengine.ums.dto.request.PasswordUpdateRequest;
import com.insightengine.ums.dto.request.UserCreateRequest;
import com.insightengine.ums.dto.request.UserPageQuery;
import com.insightengine.ums.dto.request.UserStatusRequest;
import com.insightengine.ums.dto.request.UserUpdateRequest;
import com.insightengine.ums.dto.response.UserPageVO;

/**
 * 用户管理服务接口（IF §4）。
 *
 * <p>覆盖用户分页/创建/更新/启停/改密五个管理接口（管理员操作，需对应权限）。</p>
 */
public interface UserService {

    /**
     * 用户分页列表（IF §4.1）。
     */
    PageResult<UserPageVO> page(UserPageQuery query);

    /**
     * 创建用户（IF §4.2，管理员）。
     *
     * @return 新用户 ID
     */
    Long create(UserCreateRequest request);

    /**
     * 更新用户昵称/手机号（IF §4.3）。
     */
    void update(Long id, UserUpdateRequest request);

    /**
     * 启用/禁用用户（IF §4.4）。
     */
    void updateStatus(Long id, UserStatusRequest request);

    /**
     * 修改密码（IF §4.5，本人操作）。
     */
    void updatePassword(Long userId, PasswordUpdateRequest request);
}
