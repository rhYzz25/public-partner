package com.xz.match.job.cosService;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.xz.match.common.ErrorCode;
import com.xz.match.config.CosClientConfig;
import com.xz.match.exception.MyCustomException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// note manager是完全可以复用的通用工具类
@SuppressWarnings("all")
@Component
public class CosManager {
	@Resource
	private COSClient cosClient;

	@Resource
	private CosClientConfig cosClientConfig;

	/** 上传头像
	 * @param key  唯一键
	 * @param file 文件
	 */
	public PutObjectResult uploadPicture(String key, File file) throws IOException {
		PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
		return cosClient.putObject(putObjectRequest);
	}

	public void validPicture(MultipartFile multipartFile) {
		if (multipartFile == null) throw new MyCustomException(ErrorCode.PARAMS_ERROR, "文件不能为空");
		// 1. 校验文件大小
		long fileSize = multipartFile.getSize();
		final long ONE_M = 1024 * 1024;
		if (fileSize > 10 * ONE_M) throw new MyCustomException( ErrorCode.PARAMS_ERROR, "文件大小不能超过 10MB");
		// 2. 校验文件后缀
		String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
		// 允许上传的文件后缀列表（或者集合）
		final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
		if (!ALLOW_FORMAT_LIST.contains(fileSuffix)) throw new MyCustomException(ErrorCode.PARAMS_ERROR, "文件类型错误");
	}

}
