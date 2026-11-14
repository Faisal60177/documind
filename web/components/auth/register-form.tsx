"use client";

import Link from "next/link";
import { useActionState } from "react";
import { TextField } from "@/components/auth/text-field";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { registerAction } from "@/lib/auth/actions";

export function RegisterForm() {
  const [state, action, pending] = useActionState(registerAction, undefined);

  return (
    <form action={action} className="w-full max-w-sm">
      <Card>
        <CardHeader>
          <CardTitle>Create your account</CardTitle>
          <CardDescription>
            Upload PDFs and ask questions about them.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <TextField
            id="email"
            name="email"
            label="Email"
            type="email"
            autoComplete="email"
            defaultValue={state?.email}
            error={state?.fieldErrors?.email}
          />
          <TextField
            id="password"
            name="password"
            label="Password (8 or more characters)"
            type="password"
            autoComplete="new-password"
            error={state?.fieldErrors?.password}
          />
          <TextField
            id="confirmPassword"
            name="confirmPassword"
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            error={state?.fieldErrors?.confirmPassword}
          />
          {state?.error ? (
            <p role="alert" className="text-destructive text-sm">
              {state.error}
            </p>
          ) : null}
        </CardContent>
        <CardFooter className="flex-col gap-3">
          <Button type="submit" className="w-full" disabled={pending}>
            {pending ? "Creating account…" : "Create account"}
          </Button>
          <p className="text-muted-foreground text-sm">
            Already registered?{" "}
            <Link href="/login" className="underline">
              Sign in
            </Link>
          </p>
        </CardFooter>
      </Card>
    </form>
  );
}
