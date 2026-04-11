package com.xz.match.job;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.xz.match.model.entity.User;
import com.xz.match.job.domain.UserExcelModel;
import com.xz.match.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserImportListener implements ReadListener<UserExcelModel> {

    // 每次处理 1000 条，减少数据库压力
    private static final int BATCH_COUNT = 1000;
    private List<User> cachedDataList = new ArrayList<>(BATCH_COUNT);

    // 监听器不是被 Spring 管理的 Bean，所以需要通过构造器把 Service 传进来
    private final UserService userService;

    public UserImportListener(UserService userService) {
        this.userService = userService;
    }

    /**
     * 每一行数据读取都会调用此方法
     */
    @Override
    public void invoke(UserExcelModel data, AnalysisContext context) {
        // 1. 数据转换：Excel模型 -> 数据库实体
        User user = new User();
        user.setAccount(data.getAccount());
        // 记得对 Excel 里的明文密码进行加密
        user.setPassword(DigestUtils.md5DigestAsHex(("SALT" + data.getPassword()).getBytes()));
        user.setNickname(data.getNickname());
        user.setGender(data.getGender());
        user.setIntroduction(data.getIntroduction());
        
        cachedDataList.add(user);

        // 2. 达到阈值，批量插入
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 关键：清理内存
            cachedDataList.clear();
        }
    }

    /**
     * 所有数据解析完成后会调用此方法
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 别忘了最后剩下的那点“碎屑”
        if (!cachedDataList.isEmpty()) {
            saveData();
        }
        log.info("所有数据解析完成！");
    }

    private void saveData() {
        log.info("开始插入 {} 条数据...", cachedDataList.size());
        userService.saveBatch(cachedDataList);
        log.info("插入成功！");
    }
}