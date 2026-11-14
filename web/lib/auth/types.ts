export type AuthUser = {
  id: string;
  email: string;
  role: string;
  createdAt: string;
};

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  user: AuthUser;
};

export type ProblemDetail = {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
};

export type BackendResult<T> =
  { ok: true; data: T } | { ok: false; status: number; problem: ProblemDetail };

export type FormState =
  | {
      email?: string;
      error?: string;
      fieldErrors?: Record<string, string>;
    }
  | undefined;
