package io.github.fushuwei.scaskeleton.system.api.dto.user;

import lombok.Data;

/**
 * 用户分页查询请求 DTO。
 *
 * @author Fu Wei
 */
@Data
public class UserPageRequest {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;
    /** 每页条数 */
    private Integer pageSize = 20;

    /** 用户名模糊查询 */
    private String username;
    /** 昵称模糊查询 */
    private String nickname;
    /** 用户状态筛选 */
    private String status;
    /** 部门 ID 筛选 */
    private String deptId;
}
