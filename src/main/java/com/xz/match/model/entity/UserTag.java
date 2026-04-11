package com.xz.match.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserTag implements Serializable {
    private Long id;
    private Long userId;
    private Long tagId;
    private Date createTime;

    public UserTag() {}

}