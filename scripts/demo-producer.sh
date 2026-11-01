#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
TOPIC="${TRACK_UPDATES_TOPIC:-track-updates}"

docker compose --env-file "$ENV_FILE" exec -T kafka \
    /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 \
    --topic "$TOPIC" \
    --property parse.key=true \
    --property key.separator='|' <<'MESSAGES'
uav-1|{"trackId":"uav-1","timestamp":"2026-11-01T10:00:00Z","latitude":35.000,"longitude":24.000,"altitude":5000,"heading":90,"identity":"FRIEND"}
uav-1|{"trackId":"uav-1","timestamp":"2026-11-01T10:00:10Z","latitude":35.001,"longitude":24.001,"altitude":5010,"heading":91,"identity":"FRIEND"}
uav-1|{"trackId":"uav-1","timestamp":"2026-11-01T10:00:10Z","latitude":35.001,"longitude":24.001,"altitude":5010,"heading":91,"identity":"FRIEND"}
uav-1|{"trackId":"uav-1","timestamp":"2026-11-01T09:59:50Z","latitude":34.999,"longitude":23.999,"altitude":4990,"heading":89,"identity":"FRIEND"}
aircraft-7|{"trackId":"aircraft-7","timestamp":"2026-11-01T10:00:00Z","latitude":36.000,"longitude":25.000,"altitude":9000,"heading":180,"identity":"NEUTRAL"}
aircraft-7|{"trackId":"aircraft-7","timestamp":"2026-11-01T10:00:10Z","latitude":36.002,"longitude":25.002,"altitude":9010,"heading":181,"identity":"NEUTRAL"}
missile-3|{"trackId":"missile-3","timestamp":"2026-11-01T10:00:00Z","latitude":37.000,"longitude":26.000,"altitude":12000,"heading":270,"identity":"HOSTILE"}
missile-3|{"trackId":"missile-3","timestamp":"2026-11-01T10:00:10Z","latitude":37.003,"longitude":26.003,"altitude":12010,"heading":271,"identity":"HOSTILE"}
aircraft-7|{"trackId":"aircraft-7","timestamp":"2026-11-01T10:00:20Z","latitude":95.0,"longitude":25.004,"altitude":9020,"heading":182,"identity":"NEUTRAL"}
MESSAGES
