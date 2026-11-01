package com.scytalys.trackpicture.infrastructure.persistence;

import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class TrackRepository {

    private static final String INSERT_SQL = """
            INSERT INTO track_updates (
                track_id, observed_at, latitude, longitude, altitude, heading, identity
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (track_id, observed_at) DO NOTHING
            """;

    private final JdbcClient jdbcClient;

    public TrackRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
    }

    public int insert(TrackUpdateMessage update) {
        return jdbcClient.sql(INSERT_SQL)
                .params(toSqlParameters(update))
                .update();
    }

    private static Object[] toSqlParameters(TrackUpdateMessage update) {
        return new Object[]{
                update.trackId(),
                Timestamp.from(update.timestamp()),
                update.latitude(),
                update.longitude(),
                update.altitude(),
                update.heading(),
                update.identity().name()
        };
    }
}
