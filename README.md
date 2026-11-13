# DocuMind

Spring Boot + Next.js RAG application.

## Structure

- `backend/` : Spring Boot API
- `web/` : Next.js frontend
- `docker-compose.yml` : local PostgreSQL with pgvector

## Local setup

1. Copy `.env.example` to `.env`
2. Start the database: `docker compose up -d`
3. Run the backend, then the frontend
