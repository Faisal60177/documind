import "server-only";

import { redirect } from "next/navigation";
import { getAccessToken } from "@/lib/auth/session";
import { listDocuments } from "./backend";

/** Returns the signed-in user's documents, or null if they could not be loaded. */
export async function getDocuments() {
  const accessToken = await getAccessToken();
  if (!accessToken) redirect("/login");

  const result = await listDocuments(accessToken);
  if (!result.ok) {
    if (result.status === 401) redirect("/login");
    return null;
  }
  return result.data;
}
