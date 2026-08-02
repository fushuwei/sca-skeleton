package io.github.fushuwei.scaskeleton.datasource.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用删除请求（对齐 system 模块 DeleteRequest 模式）
 *
 * @author Fu Wei
 */
@Data
public class DeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "ID 不能为空")
    private String id;
}
