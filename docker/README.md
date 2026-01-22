# Docker Development Environment

Local development setup using Docker Compose.

## Services

| Service | Port | Description |
|---------|------|-------------|
| db | 5432 | PostgreSQL 15 database |
| pgadmin | 5050 | Database management UI |
| backend | 8080 | Spring Boot API (profile: full) |
| frontend | 3000 | React/Vite dev server (profile: full) |

## Quick Start

### Database Only (recommended for local development)
```bash
cd docker
docker-compose up -d db pgadmin
```

Then run backend/frontend locally with your IDE for better debugging.

### Full Stack
```bash
cd docker
docker-compose --profile full up -d
```

## Access

- **API:** http://localhost:8080
- **Frontend:** http://localhost:3000
- **pgAdmin:** http://localhost:5050
  - Email: `admin@pfwa.local`
  - Password: `admin`

### Connecting pgAdmin to Database
1. Open http://localhost:5050
2. Right-click "Servers" → "Register" → "Server"
3. General tab: Name = `pfwa-local`
4. Connection tab:
   - Host: `db`
   - Port: `5432`
   - Database: `pfwa`
   - Username: `pfwa`
   - Password: `pfwa_dev_password`

## Commands

```bash
# Start services
docker-compose up -d db pgadmin

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Reset database (delete volume)
docker-compose down -v

# Rebuild images
docker-compose build --no-cache
```

## Environment Variables

Copy `.env.example` to `.env` to customize:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|----------|---------|-------------|
| POSTGRES_DB | pfwa | Database name |
| POSTGRES_USER | pfwa | Database user |
| POSTGRES_PASSWORD | pfwa_dev_password | Database password |
| JWT_SECRET | dev-secret-key... | JWT signing key |
| VITE_API_URL | http://localhost:8080/api | API URL for frontend |
