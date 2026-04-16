package com.xz.match.job.convert;

import com.xz.match.model.entity.JoinReq;
import com.xz.match.model.vo.JoinReqVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface JoinReqConvert {
	JoinReqVO toVO(JoinReq joinReq);
}
