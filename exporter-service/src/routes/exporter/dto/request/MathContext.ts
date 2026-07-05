export interface MathContext {
  map: Record<string, string>;
  counter: number;
}

export interface MathOptionContext {
  [questionId: string]: MathContext;
}
