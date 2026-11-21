export type AnswerStatus = "ANSWERED" | "NOT_FOUND" | "NO_DOCUMENTS";

export type AskSource = {
  number: number;
  documentId: string;
  filename: string;
  page: number;
  pageEnd: number;
  snippet: string;
  score: number;
};

export type AskResponse = {
  status: AnswerStatus;
  answer: string | null;
  sources: AskSource[];
};

export type AskItem = AskResponse & {
  id: string;
  question: string;
};

export type AskState =
  | {
      items: AskItem[];
      question?: string;
      error?: string;
    }
  | undefined;
