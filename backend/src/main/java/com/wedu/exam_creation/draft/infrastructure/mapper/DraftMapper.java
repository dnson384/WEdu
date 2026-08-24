package com.wedu.exam_creation.draft.infrastructure.mapper;

import com.wedu.exam_creation.draft.domain.entity.DraftEntity;
import com.wedu.exam_creation.draft.infrastructure.document.DraftDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DraftMapper {
    DraftEntity toEntity(DraftDocument draftDocument);

    DraftDocument toDocument(DraftEntity draftEntity);
}
