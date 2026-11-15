// Keep in sync with MAX_UPLOAD_SIZE on the API. On Vercel, request bodies are capped at about 4.5 MB.
export const MAX_UPLOAD_MB = Number(process.env.MAX_UPLOAD_MB ?? "4");
