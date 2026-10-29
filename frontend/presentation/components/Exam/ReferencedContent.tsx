import Image from "next/image";
import katex from "katex";
import "katex/dist/katex.min.css";
import { QuestionContent } from "@/domain/entities/question.entity";
import { isQuestionImageUrl } from "@/presentation/utils/questionContentEditor";

interface Props extends QuestionContent {
  imageClassName?: string;
}

export default function ReferencedContent({ template, variables, imageClassName = "w-auto h-auto max-h-80 max-w-full" }: Props) {
  return <>{template.split(/(<\w+>)/g).map((part, index) => {
    const key = part.slice(1, -1);
    if (part.startsWith("<") && part.endsWith(">")) {
      const image = variables?.image?.[key];
      if (image) {
        return isQuestionImageUrl(image)
          ? <Image key={index} src={image} alt={`Hình minh họa ${key}`} width={600} height={400} sizes="100vw" className={imageClassName} />
          : <span key={index} className="mx-1 inline-block rounded-lg border border-amber-200 bg-amber-50 p-2 text-sm text-amber-900">{part} · Mô tả ảnh: {image}</span>;
      }
      const math = variables?.math?.[key];
      if (math) return <span key={index} dangerouslySetInnerHTML={{ __html: katex.renderToString(math, { throwOnError: false, trust: false, maxExpand: 1000 }) }} />;
      return <span key={index} className="text-red-500">{part}</span>;
    }
    return <span key={index}>{part}</span>;
  })}</>;
}
