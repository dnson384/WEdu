package com.fckedu.exam_creation.importer.infrastructure.pandoc;

import com.fckedu.exam_creation.importer.dto.parsed.*;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.ExtractedHeaderResult;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.HeaderContent;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.dto.PandocEleOutput;
import com.fckedu.exam_creation.importer.infrastructure.pandoc.helper.*;
import com.fckedu.exam_creation.storage.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class PandocConverter {
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Autowired
    public PandocConverter(S3Service s3Service) {
        this.s3Service = s3Service;
        this.objectMapper = new ObjectMapper();
    }

    public ParsedDataOutput parse(byte[] fileBuffer) throws Exception {
        String userDir = System.getProperty("user.dir");
        String workspacePath = Paths.get(userDir, "static", "uploads", "pandoc_"
                + UUID.randomUUID()).toString();

        File workspaceFolder = new File(workspacePath);
        if (!workspaceFolder.exists()) {
            workspaceFolder.mkdirs();
        }

        Path tempFilePath = Paths.get(workspacePath, "input.docx");

        try {
            // 1. Lưu buffer thành file tạm
            Files.write(tempFilePath, fileBuffer);

            // 2. Chạy lệnh Pandoc bằng ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "pandoc",
                    "-f", "docx",
                    "-t", "json",
                    "--mathjax",
                    "--extract-media=" + workspacePath,
                    tempFilePath.toString()
            );

            Process process = processBuilder.start();

            // Đọc kết quả từ luồng đầu ra của Pandoc
            byte[] processOutput = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Pandoc process failed with exit code: " + exitCode);
            }

            String jsonResult = new String(processOutput);

            // Đẩy ảnh lên S3
            Map<String, String> uploadedMediaMap = new HashMap<>();
            File mediaFolder = new File(workspacePath, "media");

            if (mediaFolder.exists() && mediaFolder.isDirectory()) {
                File[] images = mediaFolder.listFiles();
                if (images != null) {
                    for (File img : images) {
                        String s3Key = s3Service.uploadLocalFile(img, "media");
                        uploadedMediaMap.put("media/" + img.getName(), s3Key);
                    }
                }
            }

            // 3. Phân tích JSON AST bằng Jackson
            JsonNode ast = objectMapper.readTree(jsonResult);
            JsonNode meta = ast.get("meta");
            JsonNode blocks = ast.get("blocks");

            List<NewQuestionImporterDTO> questionsRes = new ArrayList<>();
            NewCategoryImporterDTO categoryRes = new NewCategoryImporterDTO();
            categoryRes.setLessons(new ArrayList<>());

            String chapter = "";
            String lesson = "";

            ObjectMapper mapper = new ObjectMapper();

            // Xử lý chapter và lesson với meta
            if (meta != null && meta.isObject()) {
                if (meta.has("title") && meta.get("title").has("c")) {
                    JsonNode cNode = meta.get("title").get("c");

                    if (cNode.isArray()) {
                        List<PandocEleOutput> content = mapper.convertValue(cNode, new TypeReference<List<PandocEleOutput>>() {
                        });

                        chapter = TitleExtractor.execute(content);
                    }
                }
                if (meta.has("subtitle") && meta.get("subtitle").has("c")) {
                    JsonNode cNode = meta.get("subtitle").get("c");
                    if (cNode.isArray()) {
                        List<PandocEleOutput> content = mapper.convertValue(cNode, new TypeReference<List<PandocEleOutput>>() {
                        });

                        lesson = TitleExtractor.execute(content);

                    }
                }
            }

            // Tạo category cho chương và bài
            categoryRes.setChapter(chapter);
            LessonDataImporterDTO lessonData = new LessonDataImporterDTO(lesson, new ArrayList<BankStatDTO>());
            categoryRes.getLessons().add(lessonData);

            BankStatDTO tempBankStat = new BankStatDTO();

            if (blocks != null && blocks.isArray()) {
                boolean hasOptions = checkHasOptions(blocks);

                for (JsonNode ele : blocks) {
                    String type = ele.get("t").asString();
                    JsonNode content = ele.get("c");

                    if ("Header".equals(type)) {
                        List<PandocEleOutput> mainContent = mapper.convertValue(content.get(2), new TypeReference<List<PandocEleOutput>>() {
                        });

                        HeaderContent headerContent = new HeaderContent(content.get(0).asInt(), mainContent);

                        ExtractedHeaderResult result = HeaderExtractor.execute(headerContent);

                        Integer level = result.getLevel();
                        String text = result.getText();

                        // Dạng bài
                        if (level == 1) {
                            questionsRes.add(new NewQuestionImporterDTO());
                            questionsRes.get(questionsRes.size() - 1).setExerciseType(text);
                            tempBankStat.setExerciseType(text);
                        }
                        // Độ khó
                        else if (level == 2) {
                            questionsRes.get(questionsRes.size() - 1).setDifficultyLevel(text);
                            tempBankStat.getDifficultyLevels().add(text);
                        }
                        // Yêu cầu cần đạt
                        else if (level == 3) {
                            questionsRes.get(questionsRes.size() - 1).getLearningOutcomes().add(text);
                            tempBankStat.getLearningOutcomes().add(text);
                        }
                        // Loại câu hỏi
                        else if (level == 4) {
                            questionsRes.get(questionsRes.size() - 1).setQuestionType(text);
                            tempBankStat.setQuestionType(text);
                        }

                        boolean isFillTemp = !tempBankStat.getExerciseType().trim().isEmpty() &&
                                !tempBankStat.getDifficultyLevels().isEmpty() &&
                                !tempBankStat.getLearningOutcomes().isEmpty() &&
                                !tempBankStat.getQuestionType().trim().isEmpty();

                        // Chỉnh sửa thông số trong danh mục tổng
                        if (isFillTemp) {
                            int curStatIndex = -1;

                            List<BankStatDTO> bankStats = categoryRes.getLessons().get(0).getBankStats();

                            for (int i = 0; i < bankStats.size(); i++) {
                                BankStatDTO stat = bankStats.get(i);

                                boolean isMatch = stat.getExerciseType().equals(tempBankStat.getExerciseType()) &&
                                        IsArrayEqual.execute(stat.getDifficultyLevels(), tempBankStat.getDifficultyLevels()) &&
                                        IsArrayEqual.execute(stat.getDifficultyLevels(), tempBankStat.getDifficultyLevels()) &&
                                        stat.getQuestionType().equals(tempBankStat.getQuestionType());

                                if (isMatch) {
                                    curStatIndex = i;
                                    break;
                                }
                            }

                            if (curStatIndex < 0) {
                                categoryRes.getLessons().get(0).getBankStats().add(tempBankStat);
                            } else {
                                BankStatDTO foundStat = bankStats.get(curStatIndex);
                                foundStat.setCount(foundStat.getCount() + 1);
                            }

                            tempBankStat = new BankStatDTO();
                        }
                    } else if ("Para".equals(type)) {
                        ParaExtractor.execute(
                                content,
                                questionsRes.get(questionsRes.size() - 1),
                                uploadedMediaMap);
                    } else if (hasOptions && "BulletList".equals(type)) {
                        for (JsonNode raw : content) {
                            questionsRes.get(questionsRes.size() - 1).getOptions().add(new OptionsDataImporterDTO());

                            BulletListExtractor.execute(raw.get(0).get("c"),
                                    questionsRes.get(questionsRes.size() - 1),
                                    questionsRes.get(questionsRes.size() - 1).getOptions().size() - 1,
                                    uploadedMediaMap);
                        }
                    }
                }

            }

            if (!questionsRes.isEmpty()) {
                return new ParsedDataOutput(questionsRes, categoryRes);

            } else {
                throw new Exception("File rỗng");
            }
        } catch (Exception err) {
            System.err.println("Lỗi Pandoc: " + err.getMessage());
            throw new Exception("Lỗi chuyển đổi file.");
        } finally {
            // 5. Xóa file tạm
            deleteDirectoryRecursively(workspaceFolder);
        }
    }

    private boolean checkHasOptions(JsonNode blocks) {
        for (JsonNode ele : blocks) {
            if ("BulletList".equals(ele.get("t").asString())) {
                return true;
            }
        }
        return false;
    }

    private void deleteDirectoryRecursively(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        file.delete();
    }
}
