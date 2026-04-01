package com.dmt.toeicapp.progress.mapper;

import com.dmt.toeicapp.progress.dto.ProgressResponse;
import com.dmt.toeicapp.progress.entity.UserProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(source = "flashcard.id",   target = "flashcardId")
    @Mapping(source = "flashcard.word", target = "flashcardWord")
    @Mapping(source = "status",         target = "status",
            qualifiedByName = {},
            defaultExpression = "java(source.getStatus().name())")
    ProgressResponse toResponse(UserProgress source);
}