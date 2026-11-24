"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { PendingQuestionEntity } from "@/domain/entities/pendingQuestion.entity";
import { ModeratorService } from "@/presentation/services/ModeratorService";
import { ContentDraft, ImageDraft, contentError, contentReferences, createContentDraft, serializeContent } from "@/presentation/utils/questionContentEditor";

const emptyImage = (): ImageDraft => ({ description: "", originalUrl: "", file: null, previewUrl: "" });
const service = new ModeratorService();

export default function usePendingQuestionEdit(question: PendingQuestionEntity, onSaved: () => void) {
  const [version] = useState(question.updatedAt);
  const [reviewRevision] = useState(question.reviewRevision);
  const [contents, setContents] = useState<Record<string, ContentDraft>>(() => ({
    question: createContentDraft(question.question),
    ...Object.fromEntries(question.options.map((content, index) => [`options.${index}`, createContentDraft(content)])),
  }));
  const [error, setError] = useState("");
  const urls = useRef(new Set<string>());
  const saving = useRef(false);
  useEffect(() => {
    const allocated = urls.current;
    return () => { allocated.forEach((url) => URL.revokeObjectURL(url)); allocated.clear(); };
  }, []);

  const update = (section: string, transform: (content: ContentDraft) => ContentDraft) => {
    if (saving.current) return;
    setContents((previous) => ({ ...previous, [section]: transform(previous[section]) }));
    setError("");
  };
  const mutation = useMutation({
    mutationFn: (data: FormData) => service.editPendingQuestion(question.id, data),
    retry: false,
  });

  const submit = async () => {
    if (saving.current) return;
    for (const [section, content] of Object.entries(contents)) {
      const problem = contentError(content);
      if (problem) { setError(`${section === "question" ? "Câu hỏi" : `Phương án ${Number(section.split(".")[1]) + 1}`}: ${problem}`); return; }
    }
    const data = new FormData();
    let fileCount = 0;
    for (const [section, content] of Object.entries(contents)) {
      for (const key of contentReferences(content.template, "img")) {
        const file = content.image[key]?.file;
        if (file) { data.set(`${section}.${key}`, file); fileCount++; }
      }
    }
    if (fileCount > 10) { setError("Chỉ tải tối đa 10 ảnh trong một lần lưu."); return; }
    data.set("payload", new Blob([JSON.stringify({
      updatedAt: version,
      reviewRevision,
      question: serializeContent(contents.question),
      options: question.options.map((_, index) => serializeContent(contents[`options.${index}`])),
    })], { type: "application/json" }), "payload.json");
    saving.current = true;
    setError("");
    try {
      await mutation.mutateAsync(data);
    } catch (failure) {
      setError(isAxiosError<{ message?: string }>(failure) ? failure.response?.data?.message ?? "Không thể lưu câu hỏi. Vui lòng thử lại." : "Không thể lưu câu hỏi. Vui lòng thử lại.");
      return;
    } finally {
      saving.current = false;
    }
    onSaved();
  };

  return {
    contents, error, isSaving: mutation.isPending, submit,
    setTemplate: (section: string, template: string) => update(section, (content) => ({ ...content, template })),
    setMath: (section: string, key: string, value: string) => update(section, (content) => ({ ...content, math: { ...content.math, [key]: value } })),
    setDescription: (section: string, key: string, description: string) => update(section, (content) => ({ ...content, image: { ...content.image, [key]: { ...(content.image[key] ?? emptyImage()), description } } })),
    selectImage: (section: string, key: string, file: File | null) => {
      if (saving.current) return;
      if (file && (!['image/jpeg', 'image/png'].includes(file.type) || file.size === 0 || file.size > 5 * 1024 * 1024)) {
        setError("Chọn ảnh JPEG/PNG không rỗng, tối đa 5 MiB."); return;
      }
      const previewUrl = file ? URL.createObjectURL(file) : "";
      if (previewUrl) urls.current.add(previewUrl);
      const oldUrl = contents[section].image[key]?.previewUrl;
      if (oldUrl) { URL.revokeObjectURL(oldUrl); urls.current.delete(oldUrl); }
      update(section, (content) => ({ ...content, image: { ...content.image, [key]: { ...(content.image[key] ?? emptyImage()), file, previewUrl } } }));
    },
    insertReference: (section: string, kind: "math" | "img") => update(section, (content) => {
      const used = new Set([...contentReferences(content.template, kind), ...Object.keys(kind === "math" ? content.math : content.image)]);
      let index = 0;
      while (used.has(`${kind}_${index}`)) index++;
      return { ...content, template: `${content.template}${content.template ? " " : ""}<${kind}_${index}>` };
    }),
  };
}
