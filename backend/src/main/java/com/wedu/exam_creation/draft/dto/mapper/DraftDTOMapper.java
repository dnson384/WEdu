package com.wedu.exam_creation.draft.dto.mapper;

import com.wedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.wedu.exam_creation.common.dto.draft.response.MatrixDetailItemDTO;
import com.wedu.exam_creation.draft.domain.entity.DraftEntity;
import com.wedu.exam_creation.draft.domain.entity.MatrixDetailItemEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DraftDTOMapper {
    DraftDTO toDTO(DraftEntity entity);

    DraftEntity toEntity(DraftDTO dto);

    MatrixDetailItemEntity detailDTOToEntity(MatrixDetailItemDTO dto);
}
