package com.fckedu.exam_creation.importer.infrastructure.pandoc.helper;

import com.fckedu.exam_creation.importer.dto.parsed.NewQuestionImporterDTO;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class ParaExtractor {
    public static void execute(
            JsonNode content,
            NewQuestionImporterDTO resBase,
            Map<String, String> uploadedMediaMap
    ) {
        if (content == null || !content.isArray() || content.isEmpty()) return;


        JsonNode firstElement = content.get(0);
        String type = firstElement.has("t") ? firstElement.get("t").asString() : "";

        // Xử lý ảnh
        if ("Image".equals(type)) {
            Map<String, String> imageMap = resBase.getQuestion().getVariables().getImage();
            int curVarIndex = imageMap.size();

            String currentTemplate = resBase.getQuestion().getTemplate();
            resBase.getQuestion().setTemplate(currentTemplate + " \n<img_" + curVarIndex + ">\n ");

            String rawSrc = firstElement.get("c").get(2).get(0).asString();

            // Tạo file với tên mới là UUID
            String fileName = java.nio.file.Paths.get(rawSrc).getFileName().toString();
            String key = "media/" + fileName;

            String s3Key = uploadedMediaMap.get(key);

            if (s3Key != null) {
                imageMap.put("img_" + curVarIndex, s3Key);
            } else {
                System.err.println("Không tìm thấy ảnh trên S3 trong Map cho key: " + rawSrc);
            }
        }
        // Xử lý text & công thức
        else {
            StringBuilder templateBuilder = new StringBuilder(resBase.getQuestion().getTemplate());
            Map<String, String> mathMap = resBase.getQuestion().getVariables().getMath();

            for (JsonNode raw : content) {
                String t = raw.has("t") ? raw.get("t").asString() : "";

                if ("Str".equals(t)) {
                    templateBuilder.append(raw.get("c").asString());
                } else if ("Space".equals(t)) {
                    templateBuilder.append(" ");
                } else if ("LineBreak".equals(t)) {
                    templateBuilder.append(" \n");
                } else if ("Math".equals(t)) {
                    int curVarIndex = mathMap.size();
                    templateBuilder.append("<math_").append(curVarIndex).append(">");

                    String mathContext = raw.get("c").get(1).asString();
                    mathMap.put("math_" + curVarIndex, mathContext);
                }
            }

            resBase.getQuestion().setTemplate(templateBuilder.toString());
        }
    }

    private static @NonNull String getString(Path destPath) {
        String destPathStr = destPath.toString();
        int staticIndex = destPathStr.indexOf("static");
        String relativePart = "";

        if (staticIndex != -1) {
            relativePart = destPathStr.substring(staticIndex + "static".length() + 1);
        }

        // Chuẩn hóa gạch chéo
        String normalizedPath = relativePart.replace(File.separator, "/");

        return "/static/" + normalizedPath;
    }
}
