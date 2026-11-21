"use server";

import { redirect } from "next/navigation";
import { getAccessToken } from "@/lib/auth/session";
import { askQuestion } from "./backend";
import type { AskItem, AskState } from "./types";

const MAX_QUESTION_LENGTH = 1000;

export async function askAction(
  previous: AskState,
  formData: FormData,
): Promise<AskState> {
  const items = previous?.items ?? [];
  const question = String(formData.get("question") ?? "").trim();

  if (!question) return { items, error: "Type a question first." };
  if (question.length > MAX_QUESTION_LENGTH) {
    return {
      items,
      question,
      error: `The question must be at most ${MAX_QUESTION_LENGTH} characters.`,
    };
  }

  const accessToken = await getAccessToken();
  if (!accessToken) redirect("/login");

  const result = await askQuestion(accessToken, question);
  if (!result.ok) {
    if (result.status === 401) redirect("/login");
    return {
      items,
      question,
      error:
        result.problem.errors?.question ??
        result.problem.detail ??
        "Something went wrong. Please try again.",
    };
  }

  const item: AskItem = { id: crypto.randomUUID(), question, ...result.data };
  return { items: [item, ...items] };
}
