import { DeleteButton } from "@/components/documents/delete-button";
import { deleteDocumentAction } from "@/lib/documents/actions";
import type { DocumentItem, DocumentStatus } from "@/lib/documents/types";

const dateFormat = new Intl.DateTimeFormat("en-GB", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "UTC",
});

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const STATUS_STYLES: Record<
  DocumentStatus,
  { label: string; className: string }
> = {
  PROCESSING: {
    label: "Processing",
    className: "animate-pulse bg-amber-100 text-amber-800",
  },
  READY: { label: "Ready", className: "bg-green-100 text-green-800" },
  FAILED: { label: "Failed", className: "bg-red-100 text-red-800" },
};

function StatusBadge({ status }: { status: DocumentStatus }) {
  const style = STATUS_STYLES[status];
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${style.className}`}
    >
      {style.label}
    </span>
  );
}

function details(document: DocumentItem) {
  const parts = [formatSize(document.sizeBytes)];
  if (document.pageCount !== null) {
    parts.push(
      `${document.pageCount} ${document.pageCount === 1 ? "page" : "pages"}`,
    );
  }
  if (document.status === "READY") {
    parts.push(
      `${document.chunkCount} ${document.chunkCount === 1 ? "chunk" : "chunks"}`,
    );
  }
  parts.push(`${dateFormat.format(new Date(document.createdAt))} UTC`);
  return parts.join(" · ");
}

export function DocumentList({ documents }: { documents: DocumentItem[] }) {
  if (documents.length === 0) {
    return (
      <p className="text-muted-foreground rounded-lg border border-dashed p-8 text-center text-sm">
        No documents yet. Upload a PDF to get started.
      </p>
    );
  }

  return (
    <ul className="divide-y rounded-lg border">
      {documents.map((document) => (
        <li
          key={document.id}
          className="flex flex-wrap items-center justify-between gap-3 p-4"
        >
          <div className="min-w-0 flex-1">
            <p className="truncate font-medium">{document.filename}</p>
            <p className="text-muted-foreground text-sm">{details(document)}</p>
            {document.status === "FAILED" && document.errorMessage ? (
              <p className="text-destructive mt-1 text-sm">
                {document.errorMessage}
              </p>
            ) : null}
          </div>
          <StatusBadge status={document.status} />
          <form action={deleteDocumentAction}>
            <input type="hidden" name="id" value={document.id} />
            <DeleteButton filename={document.filename} />
          </form>
        </li>
      ))}
    </ul>
  );
}
