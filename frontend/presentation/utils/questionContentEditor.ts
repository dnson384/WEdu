import katex from "katex";
import { QuestionContent } from "@/domain/entities/question.entity";

export interface ImageDraft {
  description: string;
  originalUrl: string;
  file: File | null;
  previewUrl: string;
}

export interface ContentDraft {
  template: string;
  math: Record<string, string>;
  image: Record<string, ImageDraft>;
}

export function isQuestionImageUrl(value: string) {
  return value.startsWith("blob:") || /^https:\/\/[^/]+\.s3[.-][a-z0-9-]+\.amazonaws\.com\//i.test(value);
}

export function contentReferences(template: string, kind: "math" | "img") {
  return Array.from(new Set(Array.from(template.matchAll(new RegExp(`<(${kind}_[0-9]+)>`, "g")), (match) => match[1])));
}

export function createContentDraft(content: QuestionContent): ContentDraft {
  return {
    template: content.template,
    math: { ...content.variables?.math },
    image: Object.fromEntries(Object.entries(content.variables?.image ?? {}).map(([key, value]) => [key, {
      description: isQuestionImageUrl(value) ? "" : value,
      originalUrl: isQuestionImageUrl(value) ? value : "",
      file: null,
      previewUrl: "",
    }])),
  };
}

export function latexError(value: string) {
  if (!value.trim()) return "Vui lòng nhập công thức LaTeX.";
  if (value.length > 10000) return "Công thức tối đa 10000 ký tự.";
  try {
    katex.renderToString(value, { throwOnError: true, trust: false, maxExpand: 1000 });
    return "";
  } catch {
    return "Công thức LaTeX chưa hợp lệ.";
  }
}

export function contentError(content: ContentDraft) {
  if (!content.template.trim()) return "Nội dung không được để trống.";
  if (content.template.length > 50000) return "Nội dung tối đa 50000 ký tự.";
  if (/<(?:math|img)_/.test(content.template.replace(/<(?:math|img)_[0-9]+>/g, ""))) {
    return "Tham chiếu phải có dạng <math_0> hoặc <img_0>.";
  }
  for (const key of contentReferences(content.template, "math")) {
    const error = latexError(content.math[key] ?? "");
    if (error) return `${key}: ${error}`;
  }
  for (const key of contentReferences(content.template, "img")) {
    if (!content.image[key]?.file && !content.image[key]?.originalUrl) return `${key}: Vui lòng tải ảnh thay cho mô tả.`;
  }
  return "";
}

export function previewContent(content: ContentDraft): QuestionContent {
  return {
    template: content.template,
    variables: {
      math: content.math,
      image: Object.fromEntries(contentReferences(content.template, "img").map((key) => [key,
        content.image[key]?.previewUrl || content.image[key]?.originalUrl || content.image[key]?.description || "Chưa có ảnh",
      ])),
    },
  };
}

export function serializeContent(content: ContentDraft): QuestionContent {
  return {
    template: content.template,
    variables: {
      math: Object.fromEntries(contentReferences(content.template, "math").map((key) => [key, content.math[key]])),
      image: Object.fromEntries(contentReferences(content.template, "img").map((key) => [key, content.image[key]?.description ?? ""])),
    },
  };
}
