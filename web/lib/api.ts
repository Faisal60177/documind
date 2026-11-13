import "server-only";

export type HealthStatus = {
  api: string;
  database: string;
  pgvector: string;
};

export async function getHealth(): Promise<HealthStatus | null> {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) return null;

  try {
    const res = await fetch(`${baseUrl}/api/v1/health`, { cache: "no-store" });
    if (!res.ok) return null;
    return (await res.json()) as HealthStatus;
  } catch {
    return null;
  }
}
