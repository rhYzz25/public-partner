package com.xz.match.excelTest;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.xz.match.model.entity.User;
import com.xz.match.job.UserImportListener;
import com.xz.match.job.domain.UserExcelModel;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class UserDataTest {

	@Resource
	private UserService userService;

	@Test
    public void generateData() {
        String fileName = "test_users.xlsx";
        List<UserExcelModel> list = new ArrayList<>();
        
        // 模拟 10,000 条数据
        for (int i = 0; i < 10000; i++) {
            UserExcelModel data = new UserExcelModel();
            data.setAccount("user_" + i);
            data.setPassword("12345678"); // 初始统一密码
            data.setNickname("伙伴" + i);
            data.setGender(i % 3); // 循环生成 0, 1, 2
            data.setIntroduction("我喜欢唱跳rap篮球,编号是" + i);
            list.add(data);
        }

        // 写出文件
        EasyExcel.write(fileName, UserExcelModel.class).sheet("用户信息").doWrite(list);
        System.out.println("生成成功！文件路径：" + fileName);
    }

	// 1 sec 217ms 10000
	// 4 sec 733ms 100000
	@Test
	public void insertDate() {
		StopWatch stopWatch = new StopWatch();
		System.out.println("start insert date");
		stopWatch.start();
		final int count = 100000;
		ArrayList<User> users = new ArrayList<>();
		Long k = 3L;
		for (int i = 0; i < count; i++) {
			User user = new User();
			user.setId(k++);
			user.setAccount("userDate2_" + i);
			user.setPassword("123456789");
			user.setGender(i % 3);
			user.setIntroduction("喜欢唱跳rap篮球哈哈u" + i);
			users.add(user);
		}
		userService.saveBatch(users, 1000);
		stopWatch.stop();
		System.out.println("finish insert date");
	}

	// 可以了
	@Test
	public void fileInsert() throws FileNotFoundException {
		long currentTimeMillis = System.currentTimeMillis();
		String filePath = "/Users/zy/partnerByrhYzz/backend/test_users.xlsx";
		File file = new File(filePath);

		try(FileInputStream fos = new FileInputStream(file)) {
			EasyExcel.read(fos, UserExcelModel.class, new UserImportListener(userService))
					.excelType(ExcelTypeEnum.XLSX)
					.sheet()
					.doRead();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		long lastTimeMillis = System.currentTimeMillis();
		System.out.println(lastTimeMillis - currentTimeMillis);
	}
}