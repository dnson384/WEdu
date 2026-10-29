import { ContentDraft, contentReferences, latexError, previewContent } from "@/presentation/utils/questionContentEditor";
import ReferencedContent from "@/presentation/components/Exam/ReferencedContent";

interface Props {
  label: string;
  content: ContentDraft;
  disabled: boolean;
  onTemplate: (value: string) => void;
  onMath: (key: string, value: string) => void;
  onDescription: (key: string, value: string) => void;
  onImage: (key: string, file: File | null) => void;
  onInsert: (kind: "math" | "img") => void;
}

export default function QuestionContentEditor({ label, content, disabled, onTemplate, onMath, onDescription, onImage, onInsert }: Props) {
  return <fieldset disabled={disabled} className="min-w-0 space-y-4 rounded-xl border border-gray-200 p-4">
    <legend className="px-2 font-semibold text-gray-900">{label}</legend>
    <label className="block text-sm font-medium text-gray-700">Nội dung (giữ tham chiếu dạng {"<math_0>"}, {"<img_0>"})
      <textarea required maxLength={50000} rows={4} value={content.template} onChange={(event) => onTemplate(event.target.value)} className="mt-2 w-full rounded-xl border border-gray-300 p-3" />
    </label>
    <div className="flex flex-wrap gap-3 text-sm font-medium text-blue-600">
      <button type="button" onClick={() => onInsert("math")}>+ Chèn công thức</button>
      <button type="button" onClick={() => onInsert("img")}>+ Chèn ảnh</button>
    </div>
    {contentReferences(content.template, "math").map((key) => {
      const error = latexError(content.math[key] ?? "");
      return <label key={key} className="block rounded-xl bg-blue-50/50 p-3 text-sm text-gray-700">
        <span className="font-medium">{`<${key}>`} · Công thức LaTeX</span>
        <textarea required maxLength={10000} rows={2} value={content.math[key] ?? ""} onChange={(event) => onMath(key, event.target.value)} aria-invalid={!!error} placeholder="Ví dụ: \\frac{1}{2}mv^2" className="mt-2 w-full rounded-lg border border-gray-300 bg-white p-3 font-mono" />
        {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
      </label>;
    })}
    {contentReferences(content.template, "img").map((key) => <div key={key} className="space-y-3 rounded-xl bg-amber-50/50 p-3 text-sm text-gray-700">
      <label className="block"><span className="font-medium">{`<${key}>`} · Mô tả ảnh</span>
        <textarea maxLength={10000} rows={3} value={content.image[key]?.description ?? ""} onChange={(event) => onDescription(key, event.target.value)} placeholder={content.image[key]?.originalUrl ? "Ảnh đã có trên S3; mô tả trước đó đã được thay bằng key ảnh." : "Mô tả nội dung cần có trong hình ảnh"} className="mt-2 w-full rounded-lg border border-gray-300 bg-white p-3" />
      </label>
      <label className="block"><span>{content.image[key]?.originalUrl ? "Thay ảnh (tùy chọn)" : "Tải ảnh thay cho mô tả"} · JPEG/PNG, tối đa 5 MiB</span>
        <input type="file" accept="image/jpeg,image/png" required={!content.image[key]?.originalUrl && !content.image[key]?.file} onChange={(event) => { onImage(key, event.target.files?.[0] ?? null); event.target.value = ""; }} className="mt-2 block w-full text-sm" />
      </label>
      {content.image[key]?.file && <div className="flex flex-wrap gap-3"><span>{content.image[key].file?.name}</span><button type="button" onClick={() => onImage(key, null)} className="text-blue-600 underline">Bỏ ảnh vừa chọn</button></div>}
      <p className="text-xs text-gray-500">Lưu thành công sẽ thay mô tả bằng key ảnh trên S3. Mô tả không được lưu riêng.</p>
    </div>)}
    <div className="rounded-xl border border-dashed border-gray-200 bg-gray-50 p-4">
      <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Xem trước</p>
      <div className="whitespace-pre-wrap break-words text-sm leading-relaxed text-gray-900"><ReferencedContent {...previewContent(content)} /></div>
    </div>
  </fieldset>;
}
