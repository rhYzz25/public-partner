package com.xz.match.job.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExcelModel {
    @ExcelProperty("账号")
    private String account;

    @ExcelProperty("密码") // 实际导入建议存加密后的，这里演示用明文
    private String password;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("性别")
    private Integer gender; // 0-未知, 1-男, 2-女

    @ExcelProperty("自我介绍")
    private String introduction;
}