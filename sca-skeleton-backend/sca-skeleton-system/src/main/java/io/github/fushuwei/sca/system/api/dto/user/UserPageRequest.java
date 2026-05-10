package io.github.fushuwei.sca.system.api.dto.user;

import lombok.Data;

/**
 * 用户分页查询请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class UserPageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    private String username;
    private String nickname;
    private String status;
    private String deptId;
}
