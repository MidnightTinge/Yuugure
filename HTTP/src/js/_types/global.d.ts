type Nullable<T> = T | null;

type RouterResponse<A = any, B = any, C = any, D = any, E = any, F = any, G = any> = {
  status: string;
  code: number;
  messages: string[];
  data: Record<string, (A | B | C | D | E | F | G)[]>;
};
