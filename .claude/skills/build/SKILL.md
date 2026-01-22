---
name: build
description: Build frontend and/or backend applications
allowed-tools: Bash
---

# Build Project

Build the application(s) for development or production.

## Arguments
- No args: Build all
- `frontend` or `fe`: Build frontend only
- `backend` or `be`: Build backend only
- `docker`: Build Docker images
- `prod`: Production build

Argument provided: $ARGUMENTS

## Commands

### Frontend Development Build
```bash
cd frontend && npm run build
```

### Backend Build
```bash
cd backend && ./mvnw clean package -DskipTests
```

### Backend Build with Tests
```bash
cd backend && ./mvnw clean package
```

### Docker Build
```bash
docker-compose -f docker/docker-compose.yml build
```

### Start Development Environment
```bash
docker-compose -f docker/docker-compose.yml up -d
```

## Steps
1. Determine what to build based on arguments
2. Check if dependencies are installed
3. Run the appropriate build command
4. Report build status and any errors
