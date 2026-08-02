package io.github.fushuwei.scaskeleton.datasource.api.response.driver;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 驱动选项响应（用于下拉选择）
 *
 * @author Fu Wei
 */
@Data
@AllArgsConstructor
public class DriverOptionResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String driverName;
    private String driverVersion;
}
