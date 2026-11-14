"use server";

import { redirect } from "next/navigation";
import { loginUser, logoutUser, registerUser } from "./backend";
import { createSession, deleteSession, getRefreshToken } from "./session";
import type { FormState, ProblemDetail } from "./types";

function failure(problem: ProblemDetail, email: string): FormState {
  return {
    email,
    fieldErrors: problem.errors,
    error: problem.errors
      ? undefined
      : (problem.detail ?? "Something went wrong. Please try again."),
  };
}

export async function registerAction(
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const confirmPassword = String(formData.get("confirmPassword") ?? "");

  if (password !== confirmPassword) {
    return {
      email,
      fieldErrors: { confirmPassword: "Passwords do not match" },
    };
  }

  const result = await registerUser(email, password);
  if (!result.ok) return failure(result.problem, email);

  await createSession(result.data);
  redirect("/dashboard");
}

export async function loginAction(
  _previous: FormState,
  formData: FormData,
): Promise<FormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  const result = await loginUser(email, password);
  if (!result.ok) return failure(result.problem, email);

  await createSession(result.data);
  redirect("/dashboard");
}

export async function logoutAction() {
  const refreshToken = await getRefreshToken();
  if (refreshToken) await logoutUser(refreshToken);
  await deleteSession();
  redirect("/login");
}
