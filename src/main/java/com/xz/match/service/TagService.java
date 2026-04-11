package com.xz.match.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.User;

import java.util.List;

public interface TagService extends IService<Tag> {

	Boolean updateTag(User user, List<String> tagsList);
}
