import { QuestionContent } from "@/domain/entities/question.entity";
import ReferencedContent from "./ReferencedContent";

export default function MultipleChoiceContent(props: QuestionContent) {
  return <ReferencedContent {...props} />;
}
