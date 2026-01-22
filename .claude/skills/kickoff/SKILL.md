---
name: kickoff
description: Orchestrate agents to kick off work on an epic or feature
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Task
---

# Kickoff Epic/Feature

Orchestrate the agent team to start work on an epic or feature in the correct sequence.

## Arguments
- Epic name or feature description
- Example: `/kickoff Epic 1: Authentication`
- Example: `/kickoff user registration feature`

Argument provided: $ARGUMENTS

## Agent Sequence

Execute in this order, with each agent building on the previous:

### 1. Product Manager (@pm)
**Task:** Create user stories with acceptance criteria
**Output:** `docs/requirements/user-stories/<epic>.md`

Ask PM agent to:
- Break down the epic into user stories
- Define clear acceptance criteria for each story
- Prioritize using MoSCoW method
- Estimate story points

### 1.5 Create GitHub Issues
**Task:** Convert user stories to GitHub Issues
**Output:** GitHub Issues with labels and assignments

After PM creates user stories:
- Parse user stories from the markdown file
- For each user story, create a GitHub Issue using `gh issue create`
- Apply labels:
  - Epic label: `epic:<epic-name>` (e.g., `epic:auth`)
  - Priority: `priority:high`, `priority:medium`, `priority:low`
  - Agent assignment: `agent:backend`, `agent:frontend`, `agent:database`, etc.
- Include acceptance criteria in issue body
- Track issue numbers for reference

Example:
```bash
gh issue create \
  --title "User Registration" \
  --body "$(cat <<'EOF'
As a new user, I want to register an account, so that I can use the application.

## Acceptance Criteria
- [ ] User can enter email and password
- [ ] Password meets security requirements
- [ ] Email is validated and unique
- [ ] User receives confirmation

Refs: Epic 1 - Authentication
EOF
)" \
  --label "epic:auth,priority:high,agent:backend"
```

### 2. Architect (@architect)
**Task:** Design API contracts and technical approach
**Output:** `docs/api/<epic>-endpoints.yaml` or update `openapi.yaml`

Ask Architect agent to:
- Design REST endpoints based on user stories
- Reference GitHub Issue numbers in API spec
- Define request/response DTOs
- Document error codes and edge cases
- Create ADR if architectural decisions needed

### 3. Database (@database)
**Task:** Design schema and create migrations
**Output:** `backend/src/main/resources/db/migration/V*__<description>.sql`

Ask Database agent to:
- Design tables and relationships
- Create Flyway migration scripts
- Define indexes for query performance
- Create JPA entity classes

### 4. Backend (@backend)
**Task:** Implement API endpoints
**Output:** Controllers, Services, DTOs in `backend/src/main/java/`

Ask Backend agent to:
- Implement controllers and services
- Add validation and error handling
- Implement security (if auth-related)
- Write unit tests

### 5. Frontend (@frontend)
**Task:** Implement UI components
**Output:** Components and pages in `frontend/src/`

Ask Frontend agent to:
- Create React components
- Implement forms with validation
- Integrate with backend APIs
- Handle loading/error states

### 6. QA (@qa)
**Task:** Create test coverage
**Output:** Tests in `backend/src/test/` and `frontend/src/**/*.test.tsx`

Ask QA agent to:
- Write integration tests for APIs
- Write component tests
- Create E2E test for critical flow
- Verify acceptance criteria met

## Execution Flow

For the provided argument "$ARGUMENTS":

1. **Check prerequisites:**
   - Does `docs/` directory exist? Create if not.
   - Does `backend/` exist? Note if scaffolding needed.
   - Does `frontend/` exist? Note if scaffolding needed.

2. **Start with PM:**
   ```
   Using the @pm agent: Create detailed user stories for "$ARGUMENTS".
   Include acceptance criteria and story point estimates.
   Save to docs/requirements/user-stories/
   ```

3. **Create GitHub Issues:**
   After PM completes:
   - Parse the user stories file
   - Extract individual stories
   - Determine epic label from argument
   - Create GitHub Issue for each story using `gh issue create`
   - Apply labels: epic, priority, agent assignment
   - Capture issue numbers for tracking

4. **Progress through agents:**
   After each agent completes, summarize what was created and hand off to the next agent with context.
   Include GitHub Issue references in commits and documentation.

4. **Track progress:**
   Create a checklist showing completion status for each agent.

## Output Format

Provide a summary after each agent:

```
## Kickoff Progress: $ARGUMENTS

### PM - User Stories
- [x] Created X user stories
- [x] Defined acceptance criteria
- [x] Saved to: docs/requirements/...

### GitHub Issues Created
- [x] Issue #1: User Registration (priority:high, agent:backend)
- [x] Issue #2: User Login (priority:high, agent:backend)
- [x] Issue #3: Password Reset (priority:medium, agent:backend)
- [x] Issue #4: Login Form UI (priority:high, agent:frontend)

### Architect - API Design
- [ ] Designed X endpoints
- [ ] Created OpenAPI spec
- [ ] Saved to: docs/api/...

### Database - Schema
- [ ] Created migration V*
- [ ] Defined X entities
- [ ] Saved to: backend/src/...

### Backend - Implementation
- [ ] Implemented controllers
- [ ] Added services
- [ ] Added tests

### Frontend - UI
- [ ] Created components
- [ ] Integrated APIs
- [ ] Added tests

### QA - Verification
- [ ] Integration tests passing
- [ ] E2E test created
- [ ] Acceptance criteria verified
```

## Notes

- If backend/frontend projects don't exist yet, the first kickoff should scaffold them
- Each agent should commit their work before handing off using `/commit`
- Commits should reference GitHub Issue numbers (e.g., `Refs: #42`)
- Use `/commit` skill for consistent commit messages
- Agents should close issues in their PRs/commits when work is complete (e.g., `Closes #42`)
- If blocked, note the blocker and continue with next agent where possible
- Issue labels help identify which agent is responsible:
  - `agent:backend` - Backend Developer
  - `agent:frontend` - Frontend Developer
  - `agent:database` - Database Architect
  - `agent:devops` - DevOps Engineer
  - `agent:qa` - QA Specialist
  - `agent:architect` - Architect
