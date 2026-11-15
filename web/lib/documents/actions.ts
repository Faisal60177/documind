"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getAccessToken } from "@/lib/auth/session";
import { deleteDocument, uploadDocument } from "./backend";
import { MAX_UPLOAD_MB } from "./config";
import type { UploadState } from "./types";

export async function uploadDocumentAction(
  _previous: UploadState,
  formData: FormData,
): Promise<UploadState> {
  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { error: "Choose a PDF file to upload." };
  }
  if (file.type !== "application/pdf") {
    return { error: "Only PDF files are supported." };
  }
  if (file.size > MAX_UPLOAD_MB * 1024 * 1024) {
    return { error: `This file is larger than the ${MAX_UPLOAD_MB} MB limit.` };
  }

  const accessToken = await getAccessToken();
  if (!accessToken) redirect("/login");

  const result = await uploadDocument(accessToken, file);
  if (!result.ok) {
    if (result.status === 401) redirect("/login");
    return {
      error: result.problem.detail ?? "Upload failed. Please try again.",
    };
  }

  revalidatePath("/dashboard/documents");
  return { success: `${file.name} was uploaded and is being processed.` };
}

export async function deleteDocumentAction(formData: FormData) {
  const id = String(formData.get("id") ?? "");
  const accessToken = await getAccessToken();
  if (!accessToken) redirect("/login");

  if (id) {
    const result = await deleteDocument(accessToken, id);
    if (!result.ok && result.status === 401) redirect("/login");
  }
  revalidatePath("/dashboard/documents");
}
