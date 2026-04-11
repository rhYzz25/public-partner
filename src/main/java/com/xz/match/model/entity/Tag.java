package com.xz.match.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Tag implements Serializable {
	@TableId(type = IdType.AUTO)
	private Long id;
	private String name;
	private String kind;
	private Date createTime;

	public Tag() {
	}
}