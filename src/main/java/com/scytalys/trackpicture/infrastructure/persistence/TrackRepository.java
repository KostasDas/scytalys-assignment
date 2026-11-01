package com.scytalys.trackpicture.infrastructure.persistence;

import com.scytalys.trackpicture.application.TrackUpdate;
import com.scytalys.trackpicture.message.Identity;
import com.scytalys.trackpicture.message.TrackUpdateMessage;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class TrackRepository {

    private static final String INSERT_SQL = """
            INSERT INTO track_updates (
                track_id, observed_at, latitude, longitude, altitude, heading, identity
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (track_id, observed_at) DO NOTHING
            """;

    private static final String HISTORY_SQL = """
            SELECT track_id, observed_at, latitude, longitude, altitude, heading, identity
            FROM track_updates
            WHERE track_id = ?
            """;

    private static final RowMapper<TrackUpdate> TRACK_UPDATE_ROW_MAPPER = (resultSet, rowNum) -> new TrackUpdate(
            resultSet.getString("track_id"),
            resultSet.getTimestamp("observed_at").toInstant(),
            resultSet.getObject("latitude", Double.class),
            resultSet.getObject("longitude", Double.class),
            resultSet.getObject("altitude", Double.class),
            resultSet.getObject("heading", Double.class),
            Identity.valueOf(resultSet.getString("identity"))
    );

    private final JdbcClient jdbcClient;

    public TrackRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int insert(TrackUpdateMessage update) {
        return jdbcClient.sql(INSERT_SQL)
                .params(toSqlParameters(update))
                .update();
    }

    public Optional<TrackUpdate> findLatestByTrackId(String trackId) {
        return jdbcClient.sql(HISTORY_SQL + " ORDER BY observed_at DESC LIMIT 1")
                .param(trackId)
                .query(TRACK_UPDATE_ROW_MAPPER)
                .optional();
    }

    public List<TrackUpdate> findHistoryByTrackId(String trackId, SortDirection sortDirection) {
        String sql = switch (sortDirection) {
            case ASC -> HISTORY_SQL + " ORDER BY observed_at ASC";
            case DESC -> HISTORY_SQL + " ORDER BY observed_at DESC";
        };

        return jdbcClient.sql(sql)
                .param(trackId)
                .query(TRACK_UPDATE_ROW_MAPPER)
                .list();
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
