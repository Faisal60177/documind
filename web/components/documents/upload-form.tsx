"use client";

import { useActionState, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { uploadDocumentAction } from "@/lib/documents/actions";

export function UploadForm({ maxMb }: { maxMb: number }) {
  const [state, action, pending] = useActionState(
    uploadDocumentAction,
    undefined,
  );
  const [fileError, setFileError] = useState<string>();

  function checkFile(file: File | undefined) {
    if (!file) return setFileError(undefined);
    if (file.type !== "application/pdf") {
      return setFileError("Only PDF files are supported.");
    }
    if (file.size > maxMb * 1024 * 1024) {
      return setFileError(`This file is larger than the ${maxMb} MB limit.`);
    }
    setFileError(undefined);
  }

  return (
    <form action={action} className="w-full">
      <Card>
        <CardHeader>
          <CardTitle>Upload a PDF</CardTitle>
          <CardDescription>
            PDF only, up to {maxMb} MB. Use non-sensitive documents while the
            app runs on a free AI tier.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-3">
          <Label htmlFor="file" className="sr-only">
            PDF file
          </Label>
          <Input
            id="file"
            name="file"
            type="file"
            accept="application/pdf"
            required
            onChange={(event) => checkFile(event.target.files?.[0])}
          />
          {fileError ? (
            <p role="alert" className="text-destructive text-sm">
              {fileError}
            </p>
          ) : null}
          {state?.error ? (
            <p role="alert" className="text-destructive text-sm">
              {state.error}
            </p>
          ) : null}
          {state?.success ? (
            <p role="status" className="text-sm text-green-600">
              {state.success}
            </p>
          ) : null}
        </CardContent>
        <CardFooter>
          <Button type="submit" disabled={pending || Boolean(fileError)}>
            {pending ? "Uploading…" : "Upload"}
          </Button>
        </CardFooter>
      </Card>
    </form>
  );
}
