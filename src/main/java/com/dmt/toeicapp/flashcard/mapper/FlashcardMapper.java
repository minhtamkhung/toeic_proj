package com.dmt.toeicapp.flashcard.mapper;

import com.dmt.toeicapp.flashcard.dto.FlashcardResponse;
import com.dmt.toeicapp.flashcard.entity.Flashcard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {

    @Mapping(source = "topic.id",           target = "topicId")
    @Mapping(source = "topic.name",         target = "topicName")
    @Mapping(source = "createdBy.id",       target = "createdById")
    @Mapping(source = "createdBy.username", target = "createdByUsername")
    FlashcardResponse toResponse(Flashcard flashcard);
}