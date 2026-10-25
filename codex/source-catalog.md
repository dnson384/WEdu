# Source catalogue WEdu

Snapshot working tree 2026-10-24, gồm staged/unstaged/untracked files. Chỉ mục này xác nhận file tồn tại, không chứng nhận feature hoàn tất hoặc đã deploy. [Module map](module-map.md), [API reference](api-reference.md), [database schema](database-schema.md), [current status](current-status.md). Chỉ đọc feature cần sửa để giảm context.

## Backend theo feature/layer

### Backend `admin`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [AdminController](../backend/src/main/java/com/wedu/exam_creation/admin/controller/AdminController.java) |
| `usecase` | [AdminUsecase](../backend/src/main/java/com/wedu/exam_creation/admin/usecase/AdminUsecase.java) |

### Backend `ai`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `properties` | [DeepSeekProperties](../backend/src/main/java/com/wedu/exam_creation/ai/properties/DeepSeekProperties.java) |
| `service` | [AIQuestionGenerationService](../backend/src/main/java/com/wedu/exam_creation/ai/service/AIQuestionGenerationService.java) |

### Backend `auth`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [AuthController](../backend/src/main/java/com/wedu/exam_creation/auth/controller/AuthController.java) |
| `dto/mapper` | [AuthDTOMapper](../backend/src/main/java/com/wedu/exam_creation/auth/dto/mapper/AuthDTOMapper.java) |
| `dto/request` | [ChangePasswordRequestDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/ChangePasswordRequestDTO.java), [ResetPasswordRequestDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/request/ResetPasswordRequestDTO.java) |
| `dto/response` | [AuthorizedResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/AuthorizedResponseDTO.java), [NewAccessTokenResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/NewAccessTokenResponseDTO.java), [UserResponseDTO](../backend/src/main/java/com/wedu/exam_creation/auth/dto/response/UserResponseDTO.java) |
| `usecase` | [AuthUsecase](../backend/src/main/java/com/wedu/exam_creation/auth/usecase/AuthUsecase.java) |

### Backend `chapter`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ChapterController](../backend/src/main/java/com/wedu/exam_creation/chapter/controller/ChapterController.java) |
| `domain/entity` | [BankStatEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/BankStatEntity.java), [ChapterEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/ChapterEntity.java), [LessonDataEntity](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/entity/LessonDataEntity.java) |
| `domain/repository` | [IChapterRepository](../backend/src/main/java/com/wedu/exam_creation/chapter/domain/repository/IChapterRepository.java) |
| `dto/mapper` | [ChapterDTOMapper](../backend/src/main/java/com/wedu/exam_creation/chapter/dto/mapper/ChapterDTOMapper.java) |
| `infrastructure/document` | [BankStatDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/BankStatDocument.java), [ChapterDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/ChapterDocument.java), [LessonDataDocument](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/document/LessonDataDocument.java) |
| `infrastructure/mapper` | [ChapterMapper](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/mapper/ChapterMapper.java) |
| `infrastructure/repository` | [ChapterRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/chapter/infrastructure/repository/ChapterRepositoryImpl.java) |
| `usecase` | [ChapterService](../backend/src/main/java/com/wedu/exam_creation/chapter/usecase/ChapterService.java), [ChapterUsecase](../backend/src/main/java/com/wedu/exam_creation/chapter/usecase/ChapterUsecase.java) |

### Backend `common`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `dto/ai/request` | [GenerateQuestionRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/request/GenerateQuestionRequestDTO.java) |
| `dto/ai/response` | [AIListQuestionsResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/response/AIListQuestionsResponseDTO.java), [AIQuestionResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/ai/response/AIQuestionResponseDTO.java) |
| `dto/chapter` | [NewBankStatDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewBankStatDTO.java), [NewChapterDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewChapterDTO.java), [NewLessonDataDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/NewLessonDataDTO.java) |
| `dto/chapter/response` | [BankStatResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/BankStatResponseDTO.java), [ChapterResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/ChapterResponseDTO.java), [LessonDataResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/LessonDataResponseDTO.java), [SavedChapterResponse](../backend/src/main/java/com/wedu/exam_creation/common/dto/chapter/response/SavedChapterResponse.java) |
| `dto/draft/response` | [ChapterDraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/ChapterDraftDTO.java), [DraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/DraftDTO.java), [LessonDraftDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/LessonDraftDTO.java), [MatrixDetailItemDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/MatrixDetailItemDTO.java), [MatrixItemDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/draft/response/MatrixItemDTO.java) |
| `dto/exam/response` | [ExamDetailDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamDetailDTO.java), [ExamGeneratedDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamGeneratedDTO.java), [ExamQuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamQuestionDTO.java), [ExamQuestionGeneratedDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/ExamQuestionGeneratedDTO.java), [QuestionDetailResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/exam/response/QuestionDetailResponseDTO.java) |
| `dto/question` | [NewQuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/NewQuestionDTO.java) |
| `dto/question/response` | [ContentDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/ContentDTO.java), [QuestionDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/QuestionDTO.java), [VariablesDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/question/response/VariablesDTO.java) |
| `dto/refreshToken/request` | [NewRTRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/refreshToken/request/NewRTRequestDTO.java) |
| `dto/refreshToken/response` | [RTResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/refreshToken/response/RTResponseDTO.java) |
| `dto/token` | [ATPayload](../backend/src/main/java/com/wedu/exam_creation/common/dto/token/ATPayload.java), [RTPayload](../backend/src/main/java/com/wedu/exam_creation/common/dto/token/RTPayload.java) |
| `dto/user/mapper` | [UserCommonDTOMapper](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/mapper/UserCommonDTOMapper.java) |
| `dto/user/request` | [NewUserRequestDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/NewUserRequestDTO.java), [UserUpdateFields](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/request/UserUpdateFields.java) |
| `dto/user/response` | [CommonUserResponseAllDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/response/CommonUserResponseAllDTO.java), [CommonUserResponseDTO](../backend/src/main/java/com/wedu/exam_creation/common/dto/user/response/CommonUserResponseDTO.java) |
| `exception` | [AIGenerationException](../backend/src/main/java/com/wedu/exam_creation/common/exception/AIGenerationException.java), [BadRequestException](../backend/src/main/java/com/wedu/exam_creation/common/exception/BadRequestException.java), [ForbiddenException](../backend/src/main/java/com/wedu/exam_creation/common/exception/ForbiddenException.java), [GlobalExceptionHandler](../backend/src/main/java/com/wedu/exam_creation/common/exception/GlobalExceptionHandler.java), [InternalServerException](../backend/src/main/java/com/wedu/exam_creation/common/exception/InternalServerException.java), [NotFoundException](../backend/src/main/java/com/wedu/exam_creation/common/exception/NotFoundException.java), [UnAuthorizedException](../backend/src/main/java/com/wedu/exam_creation/common/exception/UnAuthorizedException.java) |

### Backend `config`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `` | [AppConfig](../backend/src/main/java/com/wedu/exam_creation/config/AppConfig.java), [JacksonConfig](../backend/src/main/java/com/wedu/exam_creation/config/JacksonConfig.java), [MongoConfig](../backend/src/main/java/com/wedu/exam_creation/config/MongoConfig.java), [S3Config](../backend/src/main/java/com/wedu/exam_creation/config/S3Config.java), [SecurityConfig](../backend/src/main/java/com/wedu/exam_creation/config/SecurityConfig.java) |

### Backend `draft`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [DraftController](../backend/src/main/java/com/wedu/exam_creation/draft/controller/DraftController.java) |
| `domain/entity` | [ChapterDraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/ChapterDraftEntity.java), [DraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/DraftEntity.java), [LessonDraftEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/LessonDraftEntity.java), [MatrixDetailItemEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/MatrixDetailItemEntity.java), [MatrixItemEntity](../backend/src/main/java/com/wedu/exam_creation/draft/domain/entity/MatrixItemEntity.java) |
| `domain/payload` | [UpdateChaptersPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateChaptersPayload.java), [UpdateLessonsPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateLessonsPayload.java), [UpdateMatrixDetailsPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateMatrixDetailsPayload.java), [UpdateMatrixPayload](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateMatrixPayload.java), [UpdateParam](../backend/src/main/java/com/wedu/exam_creation/draft/domain/payload/UpdateParam.java) |
| `domain/repository` | [IDraftRepository](../backend/src/main/java/com/wedu/exam_creation/draft/domain/repository/IDraftRepository.java) |
| `dto/mapper` | [DraftDTOMapper](../backend/src/main/java/com/wedu/exam_creation/draft/dto/mapper/DraftDTOMapper.java) |
| `dto/request` | [CreateDraftDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/CreateDraftDTO.java), [UpdateDraftDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/UpdateDraftDTO.java), [UpdateParamDTO](../backend/src/main/java/com/wedu/exam_creation/draft/dto/request/UpdateParamDTO.java) |
| `infrastructure/document` | [ChapterDraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/ChapterDraftDocument.java), [DraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/DraftDocument.java), [LessonDraftDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/LessonDraftDocument.java), [MatrixDetailItemDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/MatrixDetailItemDocument.java), [MatrixItemDocument](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/document/MatrixItemDocument.java) |
| `infrastructure/mapper` | [DraftMapper](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/mapper/DraftMapper.java) |
| `infrastructure/repository` | [DraftRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/draft/infrastructure/repository/DraftRepositoryImpl.java) |
| `usecase` | [DraftService](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftService.java), [DraftUsecase](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/DraftUsecase.java) |
| `usecase/util` | [DraftUtil](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/DraftUtil.java) |
| `usecase/util/dto` | [BankPool](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/BankPool.java), [Candidate](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/Candidate.java), [ComboKey](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/ComboKey.java), [RemainderItem](../backend/src/main/java/com/wedu/exam_creation/draft/usecase/util/dto/RemainderItem.java) |

### Backend `exam`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ExamController](../backend/src/main/java/com/wedu/exam_creation/exam/controller/ExamController.java) |
| `domain/entity` | [ChapterExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ChapterExamEntity.java), [ExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamEntity.java), [ExamMatrixDetailItemEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamMatrixDetailItemEntity.java), [ExamMatrixItemEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/ExamMatrixItemEntity.java), [LessonExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/LessonExamEntity.java), [QuestionExamEntity](../backend/src/main/java/com/wedu/exam_creation/exam/domain/entity/QuestionExamEntity.java) |
| `domain/repository` | [IExamRepository](../backend/src/main/java/com/wedu/exam_creation/exam/domain/repository/IExamRepository.java) |
| `dto/mapper` | [ExamCommonDraftDTOMapper](../backend/src/main/java/com/wedu/exam_creation/exam/dto/mapper/ExamCommonDraftDTOMapper.java), [ExamResponseDTOMapper](../backend/src/main/java/com/wedu/exam_creation/exam/dto/mapper/ExamResponseDTOMapper.java) |
| `dto/response` | [ExamChapterDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamChapterDTO.java), [ExamDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamDTO.java), [ExamGeneratedResponseDTO](../backend/src/main/java/com/wedu/exam_creation/exam/dto/response/ExamGeneratedResponseDTO.java) |
| `infrastructure/document` | [ChapterExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ChapterExamDocument.java), [ExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamDocument.java), [ExamMatrixDetailItemDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamMatrixDetailItemDocument.java), [ExamMatrixItemDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/ExamMatrixItemDocument.java), [LessonExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/LessonExamDocument.java), [QuestionExamDocument](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/document/QuestionExamDocument.java) |
| `infrastructure/mapper` | [ExamMapper](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/mapper/ExamMapper.java) |
| `infrastructure/repository` | [ExamRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/exam/infrastructure/repository/ExamRepositoryImpl.java) |
| `usecase` | [ExamService](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamService.java), [ExamUsecase](../backend/src/main/java/com/wedu/exam_creation/exam/usecase/ExamUsecase.java) |

### Backend `exporter`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ExporterController](../backend/src/main/java/com/wedu/exam_creation/exporter/controller/ExporterController.java) |
| `dto/request` | [ExportRequestDTO](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/ExportRequestDTO.java), [ImageCacheData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/ImageCacheData.java), [QuestionData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/QuestionData.java), [QuestionsSortedData](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/QuestionsSortedData.java), [WordPayloadRequestDTO](../backend/src/main/java/com/wedu/exam_creation/exporter/dto/request/WordPayloadRequestDTO.java) |
| `usecase` | [ExporterUsecase](../backend/src/main/java/com/wedu/exam_creation/exporter/usecase/ExporterUsecase.java) |

### Backend `importer`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `dto/pandoc` | [ExtractedHeaderResult](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/ExtractedHeaderResult.java), [HeaderContent](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/HeaderContent.java), [PandocEleOutput](../backend/src/main/java/com/wedu/exam_creation/importer/dto/pandoc/PandocEleOutput.java) |
| `dto/parsed` | [BankStatDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/BankStatDTO.java), [LessonDataImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/LessonDataImporterDTO.java), [NewChapterImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/NewChapterImporterDTO.java), [NewQuestionImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/NewQuestionImporterDTO.java), [OptionsDataImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/OptionsDataImporterDTO.java), [ParsedDataOutput](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/ParsedDataOutput.java), [QuestionContentImporterDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/QuestionContentImporterDTO.java), [VariablesDTO](../backend/src/main/java/com/wedu/exam_creation/importer/dto/parsed/VariablesDTO.java) |
| `infrastructure/pandoc` | [PandocConverter](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/PandocConverter.java) |
| `infrastructure/pandoc/helper` | [BulletListExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/BulletListExtractor.java), [HeaderExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/HeaderExtractor.java), [IsArrayEqual](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/IsArrayEqual.java), [ParaExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/ParaExtractor.java), [TitleExtractor](../backend/src/main/java/com/wedu/exam_creation/importer/infrastructure/pandoc/helper/TitleExtractor.java) |
| `service` | [ImporterService](../backend/src/main/java/com/wedu/exam_creation/importer/service/ImporterService.java) |

### Backend `mail`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `service` | [MailService](../backend/src/main/java/com/wedu/exam_creation/mail/service/MailService.java) |

### Backend `moderator`

Symbols mới cho xóa tệp: `ModeratorController.deleteQuestionFile(String key)` (DELETE), `ModeratorUsecase.deleteQuestionFile(String key)` (validate prefix/segment, gọi storage). [Test](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorDeleteQuestionFileTest.java) đi qua JWT, method security, usecase và S3Service với S3Client mock.

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [ModeratorController](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) |
| `usecase` | [ModeratorUsecase](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) |

### Backend `notification`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `telegram` | [TelegramNotificationService](../backend/src/main/java/com/wedu/exam_creation/notification/telegram/TelegramNotificationService.java) |

### Backend `passwordResetToken`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [PasswordResetTokenEntity](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/domain/entity/PasswordResetTokenEntity.java) |
| `domain/repository` | [IPasswordResetTokenRepository](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/domain/repository/IPasswordResetTokenRepository.java) |
| `infrastructure/document` | [PasswordResetTokenDocument](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/document/PasswordResetTokenDocument.java) |
| `infrastructure/mapper` | [PasswordResetTokenMapper](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/mapper/PasswordResetTokenMapper.java) |
| `infrastructure/repository` | [PasswordResetTokenRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/infrastructure/repository/PasswordResetTokenRepositoryImpl.java) |
| `usecase` | [PasswordResetTokenService](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenService.java), [PasswordResetTokenUsecase](../backend/src/main/java/com/wedu/exam_creation/passwordResetToken/usecase/PasswordResetTokenUsecase.java) |

### Backend `question`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [ContentEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/ContentEntity.java), [QuestionEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/QuestionEntity.java), [VariablesEntity](../backend/src/main/java/com/wedu/exam_creation/question/domain/entity/VariablesEntity.java) |
| `domain/repository` | [IQuestionRepository](../backend/src/main/java/com/wedu/exam_creation/question/domain/repository/IQuestionRepository.java) |
| `dto/mapper` | [QuestionDTOMapper](../backend/src/main/java/com/wedu/exam_creation/question/dto/mapper/QuestionDTOMapper.java) |
| `dto/request` | [ExamMatrixDetailDTO](../backend/src/main/java/com/wedu/exam_creation/question/dto/request/ExamMatrixDetailDTO.java) |
| `infrastructure/document` | [ContentDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/ContentDocument.java), [QuestionDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/QuestionDocument.java), [VariablesDocument](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/document/VariablesDocument.java) |
| `infrastructure/mapper` | [QuestionMapper](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/mapper/QuestionMapper.java) |
| `infrastructure/repository` | [QuestionRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/question/infrastructure/repository/QuestionRepositoryImpl.java) |
| `usecase` | [QuestionService](../backend/src/main/java/com/wedu/exam_creation/question/usecase/QuestionService.java) |

### Backend `refreshToken`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `domain/entity` | [RefreshTokenEntity](../backend/src/main/java/com/wedu/exam_creation/refreshToken/domain/entity/RefreshTokenEntity.java) |
| `domain/repository` | [IRefreshTokenRepository](../backend/src/main/java/com/wedu/exam_creation/refreshToken/domain/repository/IRefreshTokenRepository.java) |
| `dto/mapper` | [RefreshTokenDTOMapper](../backend/src/main/java/com/wedu/exam_creation/refreshToken/dto/mapper/RefreshTokenDTOMapper.java) |
| `infrastructure/document` | [RefreshTokenDocument](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/document/RefreshTokenDocument.java) |
| `infrastructure/mapper` | [RefreshTokenMapper](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/mapper/RefreshTokenMapper.java) |
| `infrastructure/repository` | [RefreshTokenRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/refreshToken/infrastructure/repository/RefreshTokenRepositoryImpl.java) |
| `usecase` | [RefreshTokenService](../backend/src/main/java/com/wedu/exam_creation/refreshToken/usecase/RefreshTokenService.java) |

### Backend `security`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `constant` | [SecurityConstants](../backend/src/main/java/com/wedu/exam_creation/security/constant/SecurityConstants.java) |
| `dto` | [CookieDataDTO](../backend/src/main/java/com/wedu/exam_creation/security/dto/CookieDataDTO.java) |
| `infrastructure/filter` | [JwtAuthenticationFilter](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/filter/JwtAuthenticationFilter.java) |
| `infrastructure/handler` | [OAuth2AuthenticationSuccessHandler](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/handler/OAuth2AuthenticationSuccessHandler.java) |
| `infrastructure/principal` | [CustomUserDetails](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/principal/CustomUserDetails.java) |
| `infrastructure/provider` | [JwtTokenProvider](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/provider/JwtTokenProvider.java) |
| `infrastructure/service` | [CustomOAuth2UserService](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomOAuth2UserService.java), [CustomUserDetailsService](../backend/src/main/java/com/wedu/exam_creation/security/infrastructure/service/CustomUserDetailsService.java) |
| `service` | [SecurityService](../backend/src/main/java/com/wedu/exam_creation/security/service/SecurityService.java) |

### Backend `storage`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [StorageController](../backend/src/main/java/com/wedu/exam_creation/storage/controller/StorageController.java) |
| `dto/response` | [S3ObjectInfo](../backend/src/main/java/com/wedu/exam_creation/storage/dto/response/S3ObjectInfo.java) |
| `service` | [S3Service](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) |

### Backend `user`

| Layer / thư mục | Symbols / source |
| --- | --- |
| `controller` | [UserController](../backend/src/main/java/com/wedu/exam_creation/user/controller/UserController.java) |
| `domain/entity` | [UserEntity](../backend/src/main/java/com/wedu/exam_creation/user/domain/entity/UserEntity.java) |
| `domain/repository` | [IUserRepository](../backend/src/main/java/com/wedu/exam_creation/user/domain/repository/IUserRepository.java) |
| `dto/mapper` | [UserDTOMapper](../backend/src/main/java/com/wedu/exam_creation/user/dto/mapper/UserDTOMapper.java) |
| `dto/request` | [LoginUserRequestDTO](../backend/src/main/java/com/wedu/exam_creation/user/dto/request/LoginUserRequestDTO.java), [RTPayloadRequestDTO](../backend/src/main/java/com/wedu/exam_creation/user/dto/request/RTPayloadRequestDTO.java) |
| `infrastructure/document` | [UserDocument](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/document/UserDocument.java) |
| `infrastructure/mapper` | [UserMapper](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/mapper/UserMapper.java) |
| `infrastructure/repository` | [UserRepositoryImpl](../backend/src/main/java/com/wedu/exam_creation/user/infrastructure/repository/UserRepositoryImpl.java) |
| `usecase` | [UserService](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserService.java), [UserUsecase](../backend/src/main/java/com/wedu/exam_creation/user/usecase/UserUsecase.java) |

## Frontend App Router

Handler DELETE nằm cùng [question-files route](../frontend/app/api/moderator/question-files/route.ts). Symbols mới: [DeleteQuestionFileUsecase.execute](../frontend/usecases/moderator/DeleteQuestionFileUsecase.ts), `ModeratorService.deleteQuestionFile`, `IModeratorRepository.deleteQuestionFile`, `ModeratorRepositoryImpl.deleteQuestionFile`. [DeleteQuestionFileDialog](../frontend/presentation/components/Moderator/DeleteQuestionFileDialog.tsx) được dùng bởi trang quản lý files; native dialog quản lý focus bàn phím, Hủy/Escape chỉ đóng khi không đang gửi request.

| Route URL | Page source |
| --- | --- |
| `/auth/forgot-password` | [app/(site)/auth/forgot-password/page.tsx](../frontend/app/(site)/auth/forgot-password/page.tsx) |
| `/auth/login` | [app/(site)/auth/login/page.tsx](../frontend/app/(site)/auth/login/page.tsx) |
| `/auth/register` | [app/(site)/auth/register/page.tsx](../frontend/app/(site)/auth/register/page.tsx) |
| `/auth/reset-password` | [app/(site)/auth/reset-password/page.tsx](../frontend/app/(site)/auth/reset-password/page.tsx) |
| `/exam/[examId]` | [app/(site)/exam/[examId]/page.tsx](../frontend/app/(site)/exam/[examId]/page.tsx) |
| `/exam/all` | [app/(site)/exam/all/page.tsx](../frontend/app/(site)/exam/all/page.tsx) |
| `/generate/[draftId]/[chapterId]` | [app/(site)/generate/[draftId]/[chapterId]/page.tsx](../frontend/app/(site)/generate/[draftId]/[chapterId]/page.tsx) |
| `/generate/[draftId]/chapter` | [app/(site)/generate/[draftId]/chapter/page.tsx](../frontend/app/(site)/generate/[draftId]/chapter/page.tsx) |
| `/generate/[draftId]/details` | [app/(site)/generate/[draftId]/details/page.tsx](../frontend/app/(site)/generate/[draftId]/details/page.tsx) |
| `/generate/[draftId]/matrix` | [app/(site)/generate/[draftId]/matrix/page.tsx](../frontend/app/(site)/generate/[draftId]/matrix/page.tsx) |
| `/generate/[draftId]` | [app/(site)/generate/[draftId]/page.tsx](../frontend/app/(site)/generate/[draftId]/page.tsx) |
| `/generate/new` | [app/(site)/generate/new/page.tsx](../frontend/app/(site)/generate/new/page.tsx) |
| `/generate` | [app/(site)/generate/page.tsx](../frontend/app/(site)/generate/page.tsx) |
| `/landing-page` | [app/(site)/landing-page/page.tsx](../frontend/app/(site)/landing-page/page.tsx) |
| `/me/bill` | [app/(site)/me/bill/page.tsx](../frontend/app/(site)/me/bill/page.tsx) |
| `/me` | [app/(site)/me/page.tsx](../frontend/app/(site)/me/page.tsx) |
| `/moderator` | [app/(site)/moderator/page.tsx](../frontend/app/(site)/moderator/page.tsx) |
| `/moderator/question-bank` | [app/(site)/moderator/question-bank/page.tsx](../frontend/app/(site)/moderator/question-bank/page.tsx) |
| `/moderator/question-files` | [app/(site)/moderator/question-files/page.tsx](../frontend/app/(site)/moderator/question-files/page.tsx) |
| `/moderator/review` | [app/(site)/moderator/review/page.tsx](../frontend/app/(site)/moderator/review/page.tsx) |
| `/upload` | [app/(site)/upload/page.tsx](../frontend/app/(site)/upload/page.tsx) |
| `/` | [app/page.tsx](../frontend/app/page.tsx) |

Layouts/entry points: [root layout](../frontend/app/layout.tsx), [moderator layout](../frontend/app/(site)/moderator/layout.tsx), [proxy](../frontend/proxy.ts), [QueryProvider](../frontend/provider/QueryProvider.tsx). Review/question-bank moderator pages là placeholders; question-files có list/download. Handlers đầy đủ ở API reference.

### Frontend `domain`

| Thư mục | Files / symbols |
| --- | --- |
| `entities` | [auth.entity](../frontend/domain/entities/auth.entity.ts), [chapter.entity](../frontend/domain/entities/chapter.entity.ts), [draft.entity](../frontend/domain/entities/draft.entity.ts), [exam.entity](../frontend/domain/entities/exam.entity.ts), [generateExam.entity](../frontend/domain/entities/generateExam.entity.ts), [question.entity](../frontend/domain/entities/question.entity.ts), [questionFile.entity](../frontend/domain/entities/questionFile.entity.ts), [user.entity](../frontend/domain/entities/user.entity.ts) |
| `repositories` | [IAdminRepository](../frontend/domain/repositories/IAdminRepository.ts), [IAuthRepository](../frontend/domain/repositories/IAuthRepository.ts), [IChapterRepository](../frontend/domain/repositories/IChapterRepository.ts), [IDraftRepository](../frontend/domain/repositories/IDraftRepository.ts), [IExamRepository](../frontend/domain/repositories/IExamRepository.ts), [IExporterRepository](../frontend/domain/repositories/IExporterRepository.ts), [IModeratorRepository](../frontend/domain/repositories/IModeratorRepository.ts), [IStorageRepository](../frontend/domain/repositories/IStorageRepository.ts), [IUserRepository](../frontend/domain/repositories/IUserRepository.ts) |

### Frontend `usecases`

| Thư mục | Files / symbols |
| --- | --- |
| `admin` | [DeleteUserUsecase](../frontend/usecases/admin/DeleteUserUsecase.ts), [FindUsersUsecase](../frontend/usecases/admin/FindUsersUsecase.ts), [GetAllUsersUsecase](../frontend/usecases/admin/GetAllUsersUsecase.ts), [LockUserUsecase](../frontend/usecases/admin/LockUserUsecase.ts), [UnlockUserUsecase](../frontend/usecases/admin/UnlockUserUsecase.ts), [UpdateUserRoleUsecase](../frontend/usecases/admin/UpdateUserRoleUsecase.ts) |
| `auth` | [ChangePasswordUsecase](../frontend/usecases/auth/ChangePasswordUsecase.ts), [ForgotPasswordUsecase](../frontend/usecases/auth/ForgotPasswordUsecase.ts), [LoginUsecase](../frontend/usecases/auth/LoginUsecase.ts), [LogoutUsecase](../frontend/usecases/auth/LogoutUsecase.ts), [RegisterUsecase](../frontend/usecases/auth/RegisterUsecase.ts), [ResetPasswordUsecase](../frontend/usecases/auth/ResetPasswordUsecase.ts) |
| `chapter` | [GetAllChaptersUsecase](../frontend/usecases/chapter/GetAllChaptersUsecase.ts) |
| `draft` | [CreateDraftUsecase](../frontend/usecases/draft/CreateDraftUsecase.ts), [DeleteDraftUsecase](../frontend/usecases/draft/DeleteDraftUsecase.ts), [GenerateMatrixDetailsUsecase](../frontend/usecases/draft/GenerateMatrixDetailsUsecase.ts), [GenerateMatrixUsecase](../frontend/usecases/draft/GenerateMatrixUsecase.ts), [GetAllUserDraftUsecase](../frontend/usecases/draft/GetAllUserDraftUsecase.ts), [GetDraftUsecase](../frontend/usecases/draft/GetDraftUsecase.ts), [GetRecentDraftUsecase](../frontend/usecases/draft/GetRecentDraftUsecase.ts), [UpdateChaptersDraftUsecase](../frontend/usecases/draft/UpdateChaptersDraftUsecase.ts), [UpdateLessonsDraftUsecase](../frontend/usecases/draft/UpdateLessonsDraftUsecase.ts) |
| `exam` | [DeleteExamUsecase](../frontend/usecases/exam/DeleteExamUsecase.ts), [GenerateExamUsecase](../frontend/usecases/exam/GenerateExamUsecase.ts), [GetAllExamsUsecase](../frontend/usecases/exam/GetAllExamsUsecase.ts), [GetExamByIdUsecase](../frontend/usecases/exam/GetExamByIdUsecase.ts), [GetRecentExamUsecase](../frontend/usecases/exam/GetRecentExamUsecase.ts) |
| `exporter` | [ExportExamUsecase](../frontend/usecases/exporter/ExportExamUsecase.ts) |
| `moderator` | [GetQuestionFilesUsecase](../frontend/usecases/moderator/GetQuestionFilesUsecase.ts), [ParseUsecase](../frontend/usecases/moderator/ParseUsecase.ts) |
| `storage` | [UploadAvatarUsecase](../frontend/usecases/storage/UploadAvatarUsecase.ts), [UploadFileToQuestionBankUsecase](../frontend/usecases/storage/UploadFileToQuestionBankUsecase.ts) |
| `user` | [GetMeUsecase](../frontend/usecases/user/GetMeUsecase.ts), [UpdateAvatarUsecase](../frontend/usecases/user/UpdateAvatarUsecase.ts), [UpdateUsernameUsecase](../frontend/usecases/user/UpdateUsernameUsecase.ts) |

### Frontend `infrastructure`

| Thư mục | Files / symbols |
| --- | --- |
| `repositories` | [AdminRepositoryImpl](../frontend/infrastructure/repositories/AdminRepositoryImpl.ts), [AuthRepositoryImpl](../frontend/infrastructure/repositories/AuthRepositoryImpl.ts), [ChapterRepositoryImpl](../frontend/infrastructure/repositories/ChapterRepositoryImpl.ts), [DraftRepositoryImpl](../frontend/infrastructure/repositories/DraftRepositoryImpl.ts), [ExamsRepositoryImpl](../frontend/infrastructure/repositories/ExamsRepositoryImpl.ts), [ExporterRepositoryImpl](../frontend/infrastructure/repositories/ExporterRepositoryImpl.ts), [ModeratorRepositoryImpl](../frontend/infrastructure/repositories/ModeratorRepositoryImpl.ts), [StorageRepositoryImpl](../frontend/infrastructure/repositories/StorageRepositoryImpl.ts), [UserRepositoryImpl](../frontend/infrastructure/repositories/UserRepositoryImpl.ts) |

### Frontend `presentation`

[QuestionFileRow](../frontend/presentation/components/Moderator/QuestionFileRow.tsx) tách JSX lặp trong danh sách tệp: props `file` (QuestionFileEntity kèm sizeLabel/lastModifiedLabel), `isDeleting`, `onDelete`. Hiển thị thông tin tệp, liên kết tải xuống và nút xóa; được dùng trong `QuestionFilesPage`.

Hook mới [useQuestionFiles](../frontend/presentation/hooks/Moderator/useQuestionFiles.ts), được dùng bởi [QuestionFilesPage](../frontend/app/(site)/moderator/question-files/page.tsx), chứa toàn bộ logic danh sách/xóa. Trả `files` kèm `sizeLabel`/`lastModifiedLabel`, trạng thái query/xóa, lỗi/thông báo/tệp được chọn và các handler `handleRefresh`, `handleDeleteClick`, `handleCancelDelete`, `handleConfirmDelete`. Đây là dữ liệu trình bày nội bộ; không thay QuestionFileEntity hoặc DTO HTTP.

| Thư mục | Files / symbols |
| --- | --- |
| `common` | [icons](../frontend/presentation/common/icons.tsx) |
| `components/Category` | [chapterSelect](../frontend/presentation/components/Category/chapterSelect.tsx), [lessonOptionSelect](../frontend/presentation/components/Category/lessonOptionSelect.tsx), [lessonSelect](../frontend/presentation/components/Category/lessonSelect.tsx) |
| `components/common` | [Logo](../frontend/presentation/components/common/Logo.tsx) |
| `components/Draft` | [DraftCard](../frontend/presentation/components/Draft/DraftCard.tsx) |
| `components/Exam` | [EssayContent](../frontend/presentation/components/Exam/EssayContent.tsx), [ExamCard](../frontend/presentation/components/Exam/ExamCard.tsx), [MultipleChoiceContent](../frontend/presentation/components/Exam/MultipleChoiceContent.tsx), [QuestionBlock](../frontend/presentation/components/Exam/QuestionBlock.tsx), [QuestionContent](../frontend/presentation/components/Exam/QuestionContent.tsx), [TrueFalseContent](../frontend/presentation/components/Exam/TrueFalseContent.tsx) |
| `components/Exam/Filter` | [DropDownFilter](../frontend/presentation/components/Exam/Filter/DropDownFilter.tsx), [FilterBar](../frontend/presentation/components/Exam/Filter/FilterBar.tsx) |
| `components/Generate` | [ChapterExamBlock](../frontend/presentation/components/Generate/ChapterExamBlock.tsx), [ExamChapterSelect](../frontend/presentation/components/Generate/ExamChapterSelect.tsx), [ExamLessonSelect](../frontend/presentation/components/Generate/ExamLessonSelect.tsx), [LessonExamBlock](../frontend/presentation/components/Generate/LessonExamBlock.tsx), [MatrixBlock](../frontend/presentation/components/Generate/MatrixBlock.tsx), [MatrixDetailsBlock](../frontend/presentation/components/Generate/MatrixDetailsBlock.tsx) |
| `components/layout` | [Error](../frontend/presentation/components/layout/Error.tsx), [Loader](../frontend/presentation/components/layout/Loader.tsx), [Navbar](../frontend/presentation/components/layout/Navbar.tsx), [NavBarLanding](../frontend/presentation/components/layout/NavBarLanding.tsx), [NavigationBTN](../frontend/presentation/components/layout/NavigationBTN.tsx), [Noti](../frontend/presentation/components/layout/Noti.tsx), [Success](../frontend/presentation/components/layout/Success.tsx) |
| `hooks/Auth` | [useAuth](../frontend/presentation/hooks/Auth/useAuth.ts), [useForgotPassword](../frontend/presentation/hooks/Auth/useForgotPassword.ts), [useLogin](../frontend/presentation/hooks/Auth/useLogin.ts), [useRegister](../frontend/presentation/hooks/Auth/useRegister.ts), [useResetPassword](../frontend/presentation/hooks/Auth/useResetPassword.ts) |
| `hooks/Dashboard` | [useDashboard](../frontend/presentation/hooks/Dashboard/useDashboard.ts) |
| `hooks/Draft` | [useAllDraft](../frontend/presentation/hooks/Draft/useAllDraft.ts) |
| `hooks/Exam` | [useAllExam](../frontend/presentation/hooks/Exam/useAllExam.ts), [useExam](../frontend/presentation/hooks/Exam/useExam.ts), [useExamFilter](../frontend/presentation/hooks/Exam/useExamFilter.ts) |
| `hooks/Generate` | [useChapter](../frontend/presentation/hooks/Generate/useChapter.ts) |
| `hooks/Generate/Child` | [useAlreadyStructure](../frontend/presentation/hooks/Generate/Child/useAlreadyStructure.ts), [useExamStructure](../frontend/presentation/hooks/Generate/Child/useExamStructure.ts), [useMatrix](../frontend/presentation/hooks/Generate/Child/useMatrix.ts), [useMatrixDetails](../frontend/presentation/hooks/Generate/Child/useMatrixDetails.ts), [useSelectChapterPage](../frontend/presentation/hooks/Generate/Child/useSelectChapterPage.ts), [useSelectLessonPage](../frontend/presentation/hooks/Generate/Child/useSelectLessonPage.ts) |
| `hooks/layout` | [useNavBar](../frontend/presentation/hooks/layout/useNavBar.ts) |
| `hooks/Me` | [useChangePassword](../frontend/presentation/hooks/Me/useChangePassword.ts), [useUpdateAvatar](../frontend/presentation/hooks/Me/useUpdateAvatar.ts), [useUpdateUsername](../frontend/presentation/hooks/Me/useUpdateUsername.ts) |
| `hooks/QuestionBank` | [useUploadToQuestionBank](../frontend/presentation/hooks/QuestionBank/useUploadToQuestionBank.ts) |
| `schemas` | [auth.schema](../frontend/presentation/schemas/auth.schema.ts), [draft.schema](../frontend/presentation/schemas/draft.schema.ts), [export.schema](../frontend/presentation/schemas/export.schema.ts), [generate.schema](../frontend/presentation/schemas/generate.schema.ts) |
| `services` | [AdminService](../frontend/presentation/services/AdminService.ts), [AuthService](../frontend/presentation/services/AuthService.ts), [ChapterService](../frontend/presentation/services/ChapterService.ts), [DraftService](../frontend/presentation/services/DraftService.ts), [ExamService](../frontend/presentation/services/ExamService.ts), [ExportService](../frontend/presentation/services/ExportService.ts), [MorderatorService](../frontend/presentation/services/MorderatorService.ts), [StorageService](../frontend/presentation/services/StorageService.ts), [user.service](../frontend/presentation/services/user.service.ts), [UserService](../frontend/presentation/services/UserService.ts) |
| `utils` | [transformExamResToUI](../frontend/presentation/utils/transformExamResToUI.ts), [transformMatrixDetailsToUI](../frontend/presentation/utils/transformMatrixDetailsToUI.ts), [transformMatrixToUI](../frontend/presentation/utils/transformMatrixToUI.ts) |

### Frontend `services`

| Thư mục | Files / symbols |
| --- | --- |
| `.` | [userService](../frontend/services/userService.ts) |

### Frontend `provider`

| Thư mục | Files / symbols |
| --- | --- |
| `.` | [QueryProvider](../frontend/provider/QueryProvider.tsx) |

## Exporter

| Source | Đường dẫn trong src |
| --- | --- |
| [app](../exporter-service/src/app.ts) | `app.ts` |
| [exporterController](../exporter-service/src/routes/exporter/controller/exporterController.ts) | `routes/exporter/controller/exporterController.ts` |
| [MathContext](../exporter-service/src/routes/exporter/dto/request/MathContext.ts) | `routes/exporter/dto/request/MathContext.ts` |
| [WordPayloadRequestDTO](../exporter-service/src/routes/exporter/dto/request/WordPayloadRequestDTO.ts) | `routes/exporter/dto/request/WordPayloadRequestDTO.ts` |
| [exporter.route](../exporter-service/src/routes/exporter/exporter.route.ts) | `routes/exporter/exporter.route.ts` |
| [OptionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/OptionBuilder.ts) | `routes/exporter/infrastructure/builder/OptionBuilder.ts` |
| [QuestionBuilder](../exporter-service/src/routes/exporter/infrastructure/builder/QuestionBuilder.ts) | `routes/exporter/infrastructure/builder/QuestionBuilder.ts` |
| [latexToOmml](../exporter-service/src/routes/exporter/infrastructure/converter/latexToOmml.ts) | `routes/exporter/infrastructure/converter/latexToOmml.ts` |
| [generateWordFile](../exporter-service/src/routes/exporter/usecase/generateWordFile.ts) | `routes/exporter/usecase/generateWordFile.ts` |

## Backend tests

Test tồn tại không đồng nghĩa pass; chưa chạy trong lượt tài liệu. ModeratorQuestionFilesTest còn kiểm route /storage/documents đã bị xóa khỏi controller hiện tại.

| Package | Test source |
| --- | --- |
| `com/wedu/exam_creation` | [BackendApplicationTests](../backend/src/test/java/com/wedu/exam_creation/BackendApplicationTests.java) |
| `com/wedu/exam_creation/auth/controller` | [AuthControllerLoginTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerLoginTest.java), [AuthControllerLogoutTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerLogoutTest.java), [AuthControllerRegisterTest](../backend/src/test/java/com/wedu/exam_creation/auth/controller/AuthControllerRegisterTest.java) |
| `com/wedu/exam_creation/auth/usecase` | [AuthUsecaseLoginTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLoginTest.java), [AuthUsecaseLogoutTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseLogoutTest.java), [AuthUsecaseRegisterTest](../backend/src/test/java/com/wedu/exam_creation/auth/usecase/AuthUsecaseRegisterTest.java) |
| `com/wedu/exam_creation/moderator/controller` | [ModeratorQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFilesTest.java) |
| `com/wedu/exam_creation/storage/controller` | [StorageAvatarControllerTest](../backend/src/test/java/com/wedu/exam_creation/storage/controller/StorageAvatarControllerTest.java) |
| `com/wedu/exam_creation/storage/service` | [StorageAvatarServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageAvatarServiceTest.java), [StorageDocumentServiceTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageDocumentServiceTest.java), [StorageQuestionFilesTest](../backend/src/test/java/com/wedu/exam_creation/storage/service/StorageQuestionFilesTest.java) |
| `com/wedu/exam_creation/user/controller` | [UserControllerMeTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerMeTest.java), [UserControllerUpdateAvatarTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerUpdateAvatarTest.java), [UserControllerUpdateUsernameTest](../backend/src/test/java/com/wedu/exam_creation/user/controller/UserControllerUpdateUsernameTest.java) |
| `com/wedu/exam_creation/user/usecase` | [UserUsecaseMeTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseMeTest.java), [UserUsecaseUpdateAvatarTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseUpdateAvatarTest.java), [UserUsecaseUpdateUsernameTest](../backend/src/test/java/com/wedu/exam_creation/user/usecase/UserUsecaseUpdateUsernameTest.java) |

## Symbols preview DOCX — 2026-10-25

- [QuestionFilePreviewDialog](../frontend/presentation/components/Moderator/QuestionFilePreviewDialog.tsx): native modal, iframe sandbox, Đóng/Tải xuống, Escape và khôi phục focus.
- [useQuestionFilePreview](../frontend/presentation/hooks/Moderator/useQuestionFilePreview.ts): tải binary, import động docx-preview, render, retry, AbortController, bỏ kết quả cũ; export previewDocument chứa CSP. useQuestionFiles thêm previewFile/handlePreviewClick/handleClosePreview; QuestionFileRow thêm onPreview.
- [Next GET content](../frontend/app/api/moderator/question-files/content/route.ts) → [GetQuestionFileContentUsecase.execute](../frontend/usecases/moderator/GetQuestionFileContentUsecase.ts) → IModeratorRepository.getQuestionFileContent / ModeratorRepositoryImpl.getQuestionFileContent. ModeratorService.getQuestionFileContent gọi BFF với signal.
- Backend ModeratorController.getQuestionFileContent → ModeratorUsecase.getQuestionFileContent (dùng chung validateQuestionFileKey với xóa) → S3Service.readDocumentFile → S3Client.getObjectAsBytes. [ModeratorQuestionFileContentTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) có 15 cases.

## Backend preview content symbols — 2026-10-25

[ModeratorController.getQuestionFileContent](../backend/src/main/java/com/wedu/exam_creation/moderator/controller/ModeratorController.java) là GET /moderator/question-files/content, trả ResponseEntity<byte[]>; [ModeratorUsecase.getQuestionFileContent / validateQuestionFileKey](../backend/src/main/java/com/wedu/exam_creation/moderator/usecase/ModeratorUsecase.java) kiểm key/extension và ánh xạ lỗi; [S3Service.readDocumentFile](../backend/src/main/java/com/wedu/exam_creation/storage/service/S3Service.java) đọc binary. [ModeratorQuestionFileContentTest](../backend/src/test/java/com/wedu/exam_creation/moderator/controller/ModeratorQuestionFileContentTest.java) có 15 cases MVC/security/usecase/storage mock.
