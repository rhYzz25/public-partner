package com.xz.match.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 用户队伍关系
 * @TableName user_team
 */
@Data
public class UserTeam {
    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}