"use client";

import { useActionState } from "react";
import { AnswerCard } from "@/components/ask/answer-card";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { askAction } from "@/lib/ask/actions";

export function AskForm() {
  const [state, action, pending] = useActionState(askAction, undefined);

  return (
    <div className="flex flex-col gap-6">
      <form action={action}>
        <Card>
          <CardHeader>
            <CardTitle>Ask your documents</CardTitle>
            <CardDescription>
              Ask in English, Bangla or another language. Answers use only your
              uploaded PDFs and cite their sources. Press Ctrl+Enter to send.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-2">
            <Label htmlFor="question" className="sr-only">
              Question
            </Label>
            <Textarea
              id="question"
              name="question"
              rows={3}
              maxLength={1000}
              required
              defaultValue={state?.question}
              placeholder="What does the document say about…?"
              onKeyDown={(event) => {
                if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
                  event.currentTarget.form?.requestSubmit();
                }
              }}
            />
            {state?.error ? (
              <p role="alert" className="text-destructive text-sm">
                {state.error}
              </p>
            ) : null}
          </CardContent>
          <CardFooter>
            <Button type="submit" disabled={pending}>
              {pending ? "Searching your documents…" : "Ask"}
            </Button>
          </CardFooter>
        </Card>
      </form>

      {state?.items.map((item) => (
        <AnswerCard key={item.id} item={item} />
      ))}
    </div>
  );
}
