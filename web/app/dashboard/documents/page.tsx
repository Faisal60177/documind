import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { DocumentList } from "@/components/documents/document-list";
import { DocumentsPoller } from "@/components/documents/documents-poller";
import { UploadForm } from "@/components/documents/upload-form";
import { buttonVariants } from "@/components/ui/button";
import { MAX_UPLOAD_MB } from "@/lib/documents/config";
import { getDocuments } from "@/lib/documents/data";

export const metadata: Metadata = { title: "Documents · DocuMind" };

async function Documents() {
  const documents = await getDocuments();

  if (!documents) {
    return (
      <p role="alert" className="text-destructive text-sm">
        Your documents could not be loaded. Please refresh the page.
      </p>
    );
  }

  return (
    <>
      <DocumentsPoller
        active={documents.some((document) => document.status === "PROCESSING")}
      />
      <DocumentList documents={documents} />
    </>
  );
}

export default function DocumentsPage() {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-col gap-8 p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Documents</h1>
          <p className="text-muted-foreground">
            Upload PDFs to make them searchable.
          </p>
        </div>
        <Link
          href="/dashboard"
          className={buttonVariants({ variant: "outline" })}
        >
          Back
        </Link>
      </div>
      <UploadForm maxMb={MAX_UPLOAD_MB} />
      <Suspense
        fallback={<p className="text-muted-foreground">Loading documents…</p>}
      >
        <Documents />
      </Suspense>
    </main>
  );
}
