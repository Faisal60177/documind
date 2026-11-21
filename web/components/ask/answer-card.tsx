import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import type { AskItem, AskSource } from "@/lib/ask/types";

function pageLabel(source: AskSource) {
  return source.page === source.pageEnd
    ? `p. ${source.page}`
    : `pp. ${source.page}–${source.pageEnd}`;
}

/** Renders the answer text, turning [n] markers into small links to the matching source. */
function AnswerText({ item }: { item: AskItem }) {
  const parts = (item.answer ?? "").split(/(\[\d+\])/g);
  return (
    <p className="leading-relaxed whitespace-pre-wrap">
      {parts.map((part, index) => {
        const match = /^\[(\d+)\]$/.exec(part);
        if (!match) return <span key={index}>{part}</span>;
        return (
          <a
            key={index}
            href={`#${item.id}-source-${match[1]}`}
            className="bg-primary/10 text-primary hover:bg-primary/20 mx-0.5 inline-flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 align-middle text-xs font-medium no-underline"
          >
            {match[1]}
          </a>
        );
      })}
    </p>
  );
}

function Sources({ item }: { item: AskItem }) {
  return (
    <div className="grid gap-2">
      <p className="text-muted-foreground text-sm font-medium">Sources</p>
      <ul className="grid gap-2">
        {item.sources.map((source) => (
          <li key={source.number} id={`${item.id}-source-${source.number}`}>
            <details className="rounded-lg border px-3 py-2">
              <summary className="flex cursor-pointer items-center gap-2 text-sm">
                <span className="bg-primary/10 text-primary inline-flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 text-xs font-medium">
                  {source.number}
                </span>
                <span className="min-w-0 flex-1 truncate font-medium">
                  {source.filename}
                </span>
                <span className="text-muted-foreground shrink-0">
                  {pageLabel(source)}
                </span>
              </summary>
              <blockquote className="text-muted-foreground mt-2 border-l-2 pl-3 text-sm whitespace-pre-wrap">
                {source.snippet}
              </blockquote>
            </details>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function AnswerCard({ item }: { item: AskItem }) {
  return (
    <Card>
      <CardHeader>
        <CardDescription>Question</CardDescription>
        <CardTitle className="text-base font-medium whitespace-pre-wrap">
          {item.question}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4">
        {item.status === "ANSWERED" ? <AnswerText item={item} /> : null}

        {item.status === "NOT_FOUND" ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-amber-900">
            <p className="text-sm font-medium">Not found in your documents</p>
            <p className="mt-1 whitespace-pre-wrap">{item.answer}</p>
          </div>
        ) : null}

        {item.status === "NO_DOCUMENTS" ? (
          <p className="text-muted-foreground">
            You have no searchable documents yet. Upload a PDF and wait until it
            shows Ready, then ask again.{" "}
            <Link href="/dashboard/documents" className="underline">
              Go to Documents
            </Link>
          </p>
        ) : null}

        {item.sources.length > 0 ? <Sources item={item} /> : null}
      </CardContent>
    </Card>
  );
}
