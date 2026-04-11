package com.xz.match.model.request;

import java.io.Serial;
import java.io.Serializable;

public class PageRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 100L;

	// think 如果并发的时候不是final会出问题?
	public static final Integer pageNum = 1;
	public static final Integer pageSize = 10;
}
