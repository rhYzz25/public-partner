package com.xz.match.job.convert;

import com.xz.match.model.entity.Tag;
import com.xz.match.model.request.tag.AddTagRequest;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TagConvert {
	Tag addTagRequestToTag(AddTagRequest addTagRequest);
}
