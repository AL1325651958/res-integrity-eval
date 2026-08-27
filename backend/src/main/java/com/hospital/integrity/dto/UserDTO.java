package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/编辑请求
 */
@Data
public class UserDTO {

    @NotBlank(message = "工号不能为空")
    private String empNo;

    @NotBlank(message = "账号不能为空")
    private String username;

    /** 为空时默认 123456 */
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Long deptId;

    private String title;

    private String phone;

    private String email;

    private Integer status;

    private List<Long> roleIds;
}
