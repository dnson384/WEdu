"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { ModeratorService } from "@/presentation/services/ModeratorService";

export const previewDocument = `<!doctype html><html><head><meta charset="utf-8">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data:; font-src data:; base-uri 'none'; form-action 'none'">
<style>body{margin:0;background:#f3f4f6} .docx-wrapper{padding:16px!important} a{pointer-events:none}</style>
</head><body></body></html>`;

export default function useQuestionFilePreview(fileKey: string) {
  const frameRef = useRef<HTMLIFrameElement>(null);
  const service = useMemo(() => new ModeratorService(), []);
  const [attempt, setAttempt] = useState(0);
  const [frameReady, setFrameReady] = useState(false);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!frameReady) return;
    const controller = new AbortController();
    const document = frameRef.current?.contentDocument;
    if (!document) return;
    // Render off-screen so a cancelled render cannot overwrite a newer preview.
    const container = document.createElement("div");
    const styles = document.createElement("div");
    async function load() {
      try {
        const [data, { renderAsync }] = await Promise.all([
          service.getQuestionFileContent(fileKey, controller.signal),
          import("docx-preview"),
        ]);
        if (controller.signal.aborted) return;
        await renderAsync(data, container, styles, {
          useBase64URL: true,
          renderAltChunks: false,
          renderChanges: false,
          renderComments: false,
        });
        if (controller.signal.aborted) return;
        container.querySelectorAll("a").forEach((link) => link.removeAttribute("href"));
        document!.body.replaceChildren(styles, container);
        setStatus("ready");
      } catch (cause) {
        if (controller.signal.aborted) return;
        let message = "Không thể xem trước tệp DOCX. Bạn có thể thử lại hoặc tải file gốc.";
        if (isAxiosError(cause) && cause.response?.data instanceof ArrayBuffer) {
          try {
            const response = JSON.parse(new TextDecoder().decode(cause.response.data));
            if (typeof response.message === "string") message = response.message;
          } catch { /* Keep the generic message for non-JSON responses. */ }
        }
        setError(message);
        setStatus("error");
      }
    }
    void load();
    return () => {
      controller.abort();
      document.body.replaceChildren();
    };
  }, [fileKey, service, frameReady, attempt]);

  return {
    frameRef, status, error,
    handleFrameLoad: () => setFrameReady(true),
    handleRetry: () => {
      setStatus("loading");
      setError("");
      setAttempt((value) => value + 1);
    },
  };
}
