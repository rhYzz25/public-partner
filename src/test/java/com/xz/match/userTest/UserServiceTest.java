package com.xz.match.userTest;

import com.xz.match.model.vo.UserVO;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class UserServiceTest {
	@Resource
	private UserService userService;

	@Test
	void testSearch() {
		// feat 主要进行边界值检测以及分支测试
		List<UserVO> userVOS = userService.search("yuanshen");
		for (UserVO userVO : userVOS) {
			System.out.println(userVO.getNickname());
		}
	}
}
