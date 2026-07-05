package com.fckedu.exam_creation.draft.infrastructure.mapper;

import com.fckedu.exam_creation.draft.domain.entity.DraftEntity;
import com.fckedu.exam_creation.draft.infrastructure.document.DraftDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DraftMapper {
    DraftEntity toEntity(DraftDocument draftDocument);

    DraftDocument toDocument(DraftEntity draftEntity);
}
