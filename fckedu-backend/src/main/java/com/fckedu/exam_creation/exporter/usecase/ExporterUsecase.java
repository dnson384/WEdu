package com.fckedu.exam_creation.exporter.usecase;

import com.fckedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.fckedu.exam_creation.common.dto.exam.response.ExamQuestionDTO;
import com.fckedu.exam_creation.exam.usecase.ExamService;
import com.fckedu.exam_creation.exporter.dto.request.ImageCacheData;
import com.fckedu.exam_creation.exporter.dto.request.QuestionData;
import com.fckedu.exam_creation.exporter.dto.request.QuestionsSortedData;
import com.fckedu.exam_creation.exporter.dto.request.WordPayloadRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
public class ExporterUsecase {
    private static final String EXPRESS_EXPORTER_URL = "http://localhost:8000/exporter/word";
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private ExamService examService;

    public byte[] exportAsWord(String examId) {
        ExamDetailDTO exam = examService.getExamById(examId);

        WordPayloadRequestDTO wordPayloadRequestDTO = new WordPayloadRequestDTO();
        wordPayloadRequestDTO.setTitle(exam.getName());

        List<QuestionsSortedData> questionsSorted = new ArrayList<>();
        Set<String> questionImageUrls = new HashSet<>();
        Set<String> optionImageUrls = new HashSet<>();


        exam.getGroups().forEach(group -> {
            group.getQuestions().forEach(question -> {
                // Gom URL ảnh của câu hỏi
                if (question.getQuestion() != null && question.getQuestion().getVariables() != null
                        && question.getQuestion().getVariables().getImage() != null) {
                    questionImageUrls.addAll(question.getQuestion().getVariables().getImage().values());
                }
                // Gom URL ảnh của các đáp án
                if (question.getOptions() != null) {
                    question.getOptions().forEach(opt -> {
                        if (opt.getVariables() != null && opt.getVariables().getImage() != null) {
                            optionImageUrls.addAll(opt.getVariables().getImage().values());
                        }
                    });
                }
            });
        });

        List<QuestionsSortedData> questionsSortedData = transformToQuestionsSorted(exam.getGroups());

        wordPayloadRequestDTO.setQuestionsSorted(questionsSortedData);

        // Tải song song tất cả ảnh từ Presigned URL về và mã hóa sang Base64
        Map<String, String> questionImageCache = fetchImagesAsBase64(questionImageUrls);
        Map<String, String> optionImageCache = fetchImagesAsBase64(optionImageUrls);

        ImageCacheData imageCache = new ImageCacheData();
        imageCache.setQuestions(questionImageCache);
        imageCache.setOptions(optionImageCache);
        wordPayloadRequestDTO.setImageCache(imageCache);

        // Gửi Request sang Express Server và nhận mảng nhị phân file Word
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WordPayloadRequestDTO> requestEntity = new HttpEntity<>(wordPayloadRequestDTO, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                EXPRESS_EXPORTER_URL,
                HttpMethod.POST,
                requestEntity,
                byte[].class
        );

        return response.getBody();
    }

    private Map<String, String> fetchImagesAsBase64(Set<String> urls) {
        Map<String, String> base64Cache = new ConcurrentHashMap<>();
        if (urls == null || urls.isEmpty()) return base64Cache;

        List<CompletableFuture<Void>> futures = urls.stream()
                .filter(Objects::nonNull)
                .map(url -> CompletableFuture.runAsync(() -> {
                    try {
                        byte[] imageBytes = restTemplate.getForObject(java.net.URI.create(url), byte[].class);

                        if (imageBytes != null) {
                            String base64String = Base64.getEncoder().encodeToString(imageBytes);
                            base64Cache.put(url, base64String);
                        }
                    } catch (Exception e) {
                        System.err.println("Không thể tải ảnh từ Presigned URL [" + url + "]: " + e.getMessage());
                    }
                }))
                .toList();

        // Đợi tất cả các tiến trình tải ảnh hoàn tất
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return base64Cache;
    }

    private List<QuestionsSortedData> transformToQuestionsSorted(List<ExamQuestionDTO> groups) {
        List<QuestionsSortedData> questionsSorted = new ArrayList<>();

        for (ExamQuestionDTO group : groups) {
            QuestionsSortedData sortedData = new QuestionsSortedData();
            sortedData.setQuestionType(group.getQuestionType());

            List<QuestionData> qDataList = new ArrayList<>();
            group.getQuestions().forEach(question -> {
                QuestionData questionData = new QuestionData(
                        question.getId(),
                        question.getQuestion(),
                        question.getOptions()
                );
                qDataList.add(questionData);
            });

            sortedData.setQuestionsData(qDataList);

            int exitedIndex = IntStream.range(0, questionsSorted.size())
                    .filter(i -> questionsSorted.get(i)
                            .getQuestionType()
                            .equals(group.getQuestionType()))
                    .findFirst()
                    .orElse(-1);

            if (exitedIndex >= 0) {
                questionsSorted.get(exitedIndex).getQuestionsData().addAll(qDataList);
            } else if (exitedIndex == -1) {
                questionsSorted.add(sortedData);
            }
        }

        List<String> customOrder = List.of("Nhiều lựa chọn", "Đúng sai", "Trả lời ngắn");

        questionsSorted.sort(Comparator.comparingInt(item -> {
            int index = customOrder.indexOf(item.getQuestionType());
            // Nếu có loại câu hỏi nào không nằm trong danh sách trên, đẩy nó xuống cuối cùng (Integer.MAX_VALUE)
            return index != -1 ? index : Integer.MAX_VALUE;
        }));

        return questionsSorted;
    }
}
