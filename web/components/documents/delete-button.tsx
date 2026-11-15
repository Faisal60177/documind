"use client";

import { useFormStatus } from "react-dom";
import { Button } from "@/components/ui/button";

export function DeleteButton({ filename }: { filename: string }) {
  const { pending } = useFormStatus();

  return (
    <Button
      type="submit"
      variant="outline"
      size="sm"
      disabled={pending}
      onClick={(event) => {
        if (!window.confirm(`Delete ${filename}? This cannot be undone.`)) {
          event.preventDefault();
        }
      }}
    >
      {pending ? "Deleting…" : "Delete"}
    </Button>
  );
}
