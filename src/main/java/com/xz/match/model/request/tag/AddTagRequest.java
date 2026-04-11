package com.xz.match.model.request.tag;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AddTagRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private String name;
	private String kind;
}
