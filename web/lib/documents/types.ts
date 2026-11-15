export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export type DocumentItem = {
  id: string;
  filename: string;
  sizeBytes: number;
  pageCount: number | null;
  chunkCount: number;
  status: DocumentStatus;
  errorMessage: string | null;
  createdAt: string;
};

export type UploadState =
  | {
      error?: string;
      success?: string;
    }
  | undefined;
