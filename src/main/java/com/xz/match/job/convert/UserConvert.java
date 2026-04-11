package com.xz.match.job.convert;

import com.xz.match.model.entity.User;
import com.xz.match.model.request.user.UpdateUserRequest;
import com.xz.match.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserConvert {
	UserVO userToUserVO(User user);

	User updateUserRequestToUser(UpdateUserRequest updateUserRequest);
}
