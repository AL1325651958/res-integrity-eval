package com.hospital.integrity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private UserInfoVO user;
}
