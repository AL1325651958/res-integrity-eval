package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成果新增/编辑请求
 */
@Data
public class AchievementDTO {

    @NotBlank(message = "成果类型不能为空")
    private String achType;

    @NotBlank(message = "标题不能为空")
    private String title;

    /** DOI/专利号/项目号/证书号 */
    private String achNo;

    /** 期刊名/立项部门/颁奖单位/学会名称 */
    private String sourceName;

    /** 发表/立项/授权时间 */
    private LocalDateTime publishTime;

    /** 级别：分区/项目级别/奖励级别 */
    private String level;

    /** 位次/角色 */
    private String rankInfo;

    /** 是否通讯作者 */
    private Integer isCorresponding;

    /** 经费/到账金额 */
    private BigDecimal fundAmount;

    /** 附件ID列表 */
    private List<Long> attachIds;
}
