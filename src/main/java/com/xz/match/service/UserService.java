package com.xz.match.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xz.match.model.entity.User;
import com.xz.match.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService extends IService<User> {
	long register(String account, String password, String checkPassword);

	UserVO login(String username, String password, HttpServletRequest request);

	Boolean logout(HttpServletRequest request);

	Boolean update(User user, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	List<UserVO> search(String keyword);

	List<UserVO> searchByTags(List<String> tagList);

	Page<UserVO> recommend(int PageNum, int pageSize, HttpServletRequest request);

	Page<UserVO> recommendByUserId(int PageNum, int pageSize, Long userId);

	UserVO safeUser(User originalUser);

}
