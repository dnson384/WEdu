import { QuestionContent as Content } from "@/domain/entities/question.entity";
import ReferencedContent from "./ReferencedContent";

interface Props extends Content {
  order: number;
}

export default function QuestionContent({ order, template, variables }: Props) {
  return <div className="leading-relaxed text-justify font-medium mb-1">
    <span className="font-bold">Câu {order}. </span>
    <ReferencedContent template={template} variables={variables} imageClassName="w-auto mx-auto h-auto min-h-40 max-w-75 object-contain" />
  </div>;
}
