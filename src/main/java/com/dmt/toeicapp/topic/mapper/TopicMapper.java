package com.dmt.toeicapp.topic.mapper;

import com.dmt.toeicapp.topic.dto.TopicResponse;
import com.dmt.toeicapp.topic.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    @Mapping(source = "createdBy.id",       target = "createdById")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    @Mapping(source = "system",             target = "isSystem")  // fix: entity "system" → DTO "isSystem"
    TopicResponse toResponse(Topic topic);
}