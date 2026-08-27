package com.hospital.integrity.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公示请求
 */
@Data
public class PublicityDTO {

    /** ALL全院 / DEPT科室 / OWN个人 */
    private String scope;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
