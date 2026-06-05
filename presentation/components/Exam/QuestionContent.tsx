import Image from "next/image";
import katex from "katex";
import "katex/dist/katex.min.css";

interface Data {
  order: number;
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export default function QuestionContent({ order, template, variables }: Data) {
  const parts = template.split(/(<\w+>)/g);

  return (
    <div className="leading-relaxed text-justify font-medium mb-1">
      <span className="font-bold">Câu {order}. </span>
      {parts.map((part, index) => {
        if (part.startsWith("<") && part.endsWith(">")) {
          const key = part.slice(1, -1);

          if (variables && variables.image && variables.image[key]) {
            const imgUrl = `/api/image${variables.image[key]}`;
            return (
              <span key={index}>
                <Image
                  src={imgUrl}
                  alt={key}
                  width={0}
                  height={0}
                  sizes="100vw"
                  className="w-auto mx-auto h-auto min-h-40 max-w-75 object-contain"
                />
              </span>
            );
          }

          if (variables && variables.math && variables.math[key]) {
            const mathHtml = katex.renderToString(variables.math[key], {
              throwOnError: false,
            });
            return (
              <span
                key={index}
                dangerouslySetInnerHTML={{ __html: mathHtml }}
              />
            );
          }

          return (
            <span key={index} className="text-red-500">
              {part}
            </span>
          );
        }

        return <span key={index}>{part}</span>;
      })}
    </div>
  );
}
