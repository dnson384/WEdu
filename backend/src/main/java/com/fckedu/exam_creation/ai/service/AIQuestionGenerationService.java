package com.fckedu.exam_creation.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fckedu.exam_creation.ai.properties.DeepSeekProperties;
import com.fckedu.exam_creation.common.exception.AIGenerationException;
import com.fckedu.exam_creation.question.dto.request.GenerateQuestionRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIQuestionGenerationService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DeepSeekProperties properties;

    public AIQuestionGenerationService(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String generateQuestions(List<GenerateQuestionRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        String prompt = buildPrompt(requests);
        String endpoint = String.format(
                "/models/%s:generateContent?key=%s",
                properties.getModel(),
                properties.getApiKey()
        );

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.7
        );

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Lỗi: ", e);
            throw new AIGenerationException("Không thể gọi DeepSeek API: " + e.getMessage());
        }

        return extractGeneratedText(rawResponse);
    }

    private String extractGeneratedText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception e) {
            log.error("Lỗi tại hàm extractGeneratedText: ", e);
            throw new AIGenerationException("Không thể trích xuất nội dung từ phản hồi DeepSeek");
        }
    }

    private String buildPrompt(List<GenerateQuestionRequestDTO> requests) {
        int totalQuestions = requests.stream()
                .mapToInt(req -> req.getNumberOfQuestions() != null ? req.getNumberOfQuestions() : 0)
                .sum();

        StringBuilder specsBuilder = new StringBuilder();
        for (int i = 0; i < requests.size(); i++) {
            GenerateQuestionRequestDTO req = requests.get(i);
            specsBuilder.append(String.format("""
                            Yêu cầu %d:
                            - Số lượng câu hỏi cần tạo: %d câu
                            - Chương: "%s"
                            - Bài: "%s"
                            - Dạng bài: "%s"
                            - Mức độ: "%s"
                            - Yêu cầu cần đạt: "%s"
                            - Loại câu hỏi: "%s"
                            """,
                    i + 1,
                    req.getNumberOfQuestions(),
                    req.getChapter(),
                    req.getLesson(),
                    req.getExerciseType(),
                    req.getDifficultyLevel(),
                    req.getLearningOutcome(),
                    req.getQuestionType()
            ));
        }

        return """
                Bạn là một Chuyên gia khảo thí và xây dựng ngân hàng câu hỏi cấp cao. Nhiệm vụ của bạn là soạn thảo danh sách câu hỏi dựa trên các yêu cầu cụ thể, đảm bảo tính khoa học, độ chính xác kiến thức và định dạng dữ liệu nghiêm ngặt.
                1. THÔNG TIN ĐẦU VÀO
                - DANH SÁCH YÊU CẦU CẦN TẠO CÂU HỎI: %s
                - SỐ LƯỢNG CÂU HỎI CẦN TẠO: %d
                2. QUY TẮC NỘI DUNG CHUNG
                - Chính xác: Thông tin, số liệu, công thức phải đúng tuyệt đối với kiến thức khoa học/giáo khoa.
                - Phân hóa:
                    + Nhận biết: Nhắc lại kiến thức, định nghĩa.
                    + Thông hiểu: Giải thích, so sánh, phân tích cơ bản.
                    + Vận dụng: Áp dụng công thức/kiến thức vào bài toán/tình huống cụ thể.
                    + Vận dụng cao: Kết hợp đa kiến thức, tư duy logic phức tạp hoặc giải quyết vấn đề mới.
                - Độc lập: Các câu hỏi không được trùng lặp nội dung hoặc cung cấp gợi ý cho nhau.
                3. QUY ĐỊNH LOẠI CÂU HỎI
                - Nhiều lựa chọn (questionType: "Nhiều lựa chọn"):
                    + Phải có đúng 4 phương án. Chỉ 1 phương án đúng.
                    + Các phương án nhiễu phải có tính logic (sai lầm thường gặp của học sinh), không được vô lý.
                - Trả lời ngắn (questionType: "Trả lời ngắn"):
                    + Tập trung vào tính toán định lượng hoặc xác định một đại lượng/khái niệm cụ thể.
                    + Kết quả phải ngắn gọn, duy nhất và xác định. Trường options phải là mảng rỗng [].
                - Đúng sai (questionType: "Đúng sai"):
                    + Mỗi câu hỏi gồm ít nhất 4 mệnh đề (mỗi mệnh đề là một phần tử trong options).
                    + Mỗi mệnh đề phải là một khẳng định độc lập, có thể xác định rõ ràng Đúng hoặc Sai.
                4. HỆ THỐNG PLACEHOLDER
                - Công thức toán học: Mọi ký hiệu, số liệu, công thức toán/lý/hóa BẮT BUỘC phải dùng placeholder <math_n>.
                    + Giá trị trong variables.math là chuỗi LaTeX (không kèm dấu $ hoặc ). Ví dụ: 10 \text{ cm}, \sqrt{x^2+1}.
                - Hình ảnh: Placeholder là <img_n>.
                    + Giá trị trong variables.image là mô tả chi tiết nội dung cần có trong ảnh để họa sĩ vẽ lại được.
                - Ràng buộc: Nếu khai báo biến trong variables thì placeholder BẮT BUỘC phải xuất hiện trong template và ngược lại. Không khai báo thừa.
                5. ĐỊNH DẠNG ĐẦU RA (JSON):
                Trả về DUY NHẤT một đối tượng JSON theo đúng format sau, không thêm bất kỳ text giải thích nào khác:
                {
                  "questions": [
                    {
                        "chapter": "Tên chương tương ứng",
                        "lesson": "Tên bài tương ứng",
                        "exerciseType": "Dạng bài tương ứng",
                        "difficultyLevel": "Mức độ tương ứng",
                        "learningOutcome": "Yêu cầu cần đạt tương ứng",
                        "questionType": "Loại câu hỏi tương ứng",
                       "question": {
                            "template": "Đây là với text thường. Đây là với hình ảnh <img_0>. Đây là với công thức toán học <math_0>, <math_1>",
                            "variables": {
                                "math": {
                                    "math_0": "Công thức LaTeX: 10\\\\ cm",
                                    "math_1": "Công thức LaTeX: 5\\\\ A"
                                },
                                "image": {
                                    "img_0": "Mô tả chi tiết về hình ảnh"
                                }
                            }
                        },
                        "options": [
                            {
                               "template": "Đây là với text thường. Đây là với hình ảnh <img_0>. Đây là với công thức toán học <math_0>, <math_1>",
                                "variables": {
                                    "math": {
                                        "math_0": "Công thức LaTeX: 10\\\\ cm",
                                        "math_1": "Công thức LaTeX: 5\\\\ A"
                                    },
                                    "image": {
                                        "img_0": "Đây là phần chứa mô tả ảnh"
                                    }
                                }
                            }
                            // ... (tối thiểu 4 options cho loại "Nhiều lựa chọn" và "Đúng sai")
                        ]
                    }
                  ]
                }
                """.formatted(specsBuilder.toString(), totalQuestions
        );
    }

}
