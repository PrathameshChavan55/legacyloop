#!/usr/bin/env bash
# Local development: infrastructure in Docker, services and the web app on the host.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Starting MySQL, MongoDB and RabbitMQ..."
docker compose up -d mysql mongodb rabbitmq

echo "Building the backend..."
(cd backend && mvn -q clean package -DskipTests)

mkdir -p logs
for service in user career social; do
  java -jar "backend/${service}-service/target/${service}-service-1.0.0.jar" \
    > "logs/${service}-service.log" 2>&1 &
  echo "  ${service}-service started (log: logs/${service}-service.log)"
done

trap 'kill $(jobs -p) 2>/dev/null' EXIT

echo "Starting the web app on http://localhost:5173"
(cd frontend && npm install --silent && npm run dev)
