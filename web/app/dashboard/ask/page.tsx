import type { Metadata } from "next";
import Link from "next/link";
import { AskForm } from "@/components/ask/ask-form";
import { buttonVariants } from "@/components/ui/button";

export const metadata: Metadata = { title: "Ask · DocuMind" };

export default function AskPage() {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-col gap-8 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Ask</h1>
          <p className="text-muted-foreground">
            Get answers grounded in your own documents.
          </p>
        </div>
        <Link
          href="/dashboard"
          className={buttonVariants({ variant: "outline" })}
        >
          Back
        </Link>
      </div>
      <AskForm />
    </main>
  );
}
