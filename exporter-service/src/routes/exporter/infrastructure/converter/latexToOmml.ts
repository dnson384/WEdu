import { mathjax } from "mathjax-full/js/mathjax.js";
import { TeX } from "mathjax-full/js/input/tex.js";
import { liteAdaptor } from "mathjax-full/js/adaptors/liteAdaptor.js";
import { RegisterHTMLHandler } from "mathjax-full/js/handlers/html.js";
import { SerializedMmlVisitor } from "mathjax-full/js/core/MmlTree/SerializedMmlVisitor.js";
import { mml2omml } from "mathml2omml";

const adaptor = liteAdaptor();
RegisterHTMLHandler(adaptor);
const tex = new TeX({ packages: ["base", "ams"] });
const visitor = new SerializedMmlVisitor();

export function latexToOmml(latex: string): string {
  const html = mathjax.document("", {
    InputJax: tex,
  });
  const node = html.convert(latex, { display: false });

  const mathmlString = visitor.visitTree(node);

  let omml = mml2omml(mathmlString);
  return omml;
}
