package com.documind.health;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
class HealthController {

    private final JdbcTemplate jdbc;

    HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    HealthResponse health() {
        String database = "DOWN";
        String pgvector = "DOWN";
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            database = "UP";
            Integer ext = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
            if (ext != null && ext > 0) {
                pgvector = "UP";
            }
        } catch (DataAccessException ignored) {
            // reported as DOWN in the response
        }
        return new HealthResponse("UP", database, pgvector);
    }
}
