import { Suspense } from "react";
import { getHealth } from "@/lib/api";
import { connection } from "next/server";

function StatusRow({ label, value }: { label: string; value: string }) {
  const up = value === "UP";
  return (
    <div className="flex items-center justify-between rounded-lg border px-4 py-3">
      <span className="font-medium">{label}</span>
      <span className={up ? "text-green-600" : "text-red-600"}>{value}</span>
    </div>
  );
}

async function HealthRows() {
  await connection();
  const health = await getHealth();

  return (
    <div className="flex flex-col gap-3">
      <StatusRow label="API" value={health?.api ?? "DOWN"} />
      <StatusRow label="Database" value={health?.database ?? "DOWN"} />
      <StatusRow label="pgvector" value={health?.pgvector ?? "DOWN"} />
    </div>
  );
}

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center gap-6 p-6">
      <div>
        <h1 className="text-3xl font-bold">DocuMind</h1>
        <p className="text-muted-foreground">System status</p>
      </div>
      <Suspense fallback={<p className="text-muted-foreground">Checking…</p>}>
        <HealthRows />
      </Suspense>
    </main>
  );
}
