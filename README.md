# DocuMind

![backend](https://github.com/Faisal60177/documind/actions/workflows/backend.yml/badge.svg)
![web](https://github.com/Faisal60177/documind/actions/workflows/web.yml/badge.svg)

Upload PDFs and ask questions in your own language. Spring Boot + Next.js RAG application.

## Structure

- `backend/` Spring Boot API (Java 25, Maven)
- `web/` Next.js frontend (App Router, TypeScript)
- `docker-compose.yml` local PostgreSQL with pgvector

## Local setup

1. Copy `.env.example` to `.env`
2. Start the database: `docker compose up -d`
3. Backend: `cd backend`, then `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
4. Web: `cd web`, copy `.env.example` to `.env.local`, then `npm install` and `npm run dev`
5. Open http://localhost:3000, where the status page should show API, Database and pgvector as UP
