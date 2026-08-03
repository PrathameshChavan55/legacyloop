# LegacyLoop Backend

AI-Powered Professional Networking & Career Development Platform

## Services

| Service | Port | DB |
|---------|------|----|
| eureka-server | 8761 | — |
| config-server | 8888 | — |
| api-gateway | 8080 | — |
| auth-service | 8081 | MySQL |
| core-service | 8082 | MySQL |
| feed-service | 8083 | MongoDB |
| payment-service | 8084 | MySQL |

## Quick Start

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose up -d
```
