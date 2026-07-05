package com.fckedu.exam_creation.draft.usecase;

import com.fckedu.exam_creation.common.dto.draft.response.DraftDTO;
import com.fckedu.exam_creation.draft.domain.entity.DraftEntity;
import com.fckedu.exam_creation.draft.domain.repository.IDraftRepository;
import com.fckedu.exam_creation.draft.dto.mapper.DraftDTOMapper;
import org.springframework.stereotype.Service;

@Service
public class DraftService {
    private final IDraftRepository repo;
    private final DraftDTOMapper mapper;

    public DraftService(IDraftRepository repo, DraftDTOMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public DraftDTO getDraft(String draftId, String userId) {
        DraftEntity draft = repo.getDraft(draftId, userId);
        return mapper.toDTO(draft);
    }

    public void deleteDraft(String draftId, String userId) {
        repo.deleteDraft(draftId, userId);
    }

}
