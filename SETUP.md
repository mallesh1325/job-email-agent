# Job Email Agent - Web Search + AI Draft Update

This package contains new and modified files to add to your existing
`job-email-agent` project. Copy these into your project at the same
relative paths (they'll create new files or overwrite the ones listed
as "modified" below).

## What's new

**New files:**
- `src/main/java/com/jobEmailAgent/model/JobPosting.java`
- `src/main/java/com/jobEmailAgent/service/source/JobSource.java`
- `src/main/java/com/jobEmailAgent/service/source/RemoteOkJobSource.java`
- `src/main/java/com/jobEmailAgent/service/source/ArbeitnowJobSource.java`
- `src/main/java/com/jobEmailAgent/service/source/AdzunaJobSource.java`
- `src/main/java/com/jobEmailAgent/dao/ProcessedJobEntity.java`
- `src/main/java/com/jobEmailAgent/repository/ProcessedJobRepository.java`
- `src/main/java/com/jobEmailAgent/service/DraftEmailService.java`
- `src/main/java/com/jobEmailAgent/service/GmailDraftService.java`
- `src/main/java/com/jobEmailAgent/service/JobAggregatorService.java`
- `migrations/001_create_processed_jobs.sql`

**Modified files (overwrite your existing copies):**
- `src/main/java/com/jobEmailAgent/util/SkillExtractor.java` - much broader skill list
- `src/main/java/com/jobEmailAgent/service/MatchScoringService.java` - added job-posting scoring
- `src/main/java/com/jobEmailAgent/service/GmailAuthService.java` - added `gmail.compose` scope
- `src/main/java/com/jobEmailAgent/service/GmailReaderService.java` - resume-based matching + reply drafts
- `src/main/java/com/jobEmailAgent/runner/EmailPollingScheduler.java` - runs the full pipeline hourly
- `src/main/java/com/jobEmailAgent/runner/JobEmailAgentRunner.java` - simplified (no double-run)
- `src/main/resources/application.properties` - new config block appended at the end

## Setup steps

### 1. Get a Claude API key
Create a key in the Anthropic Console (console.anthropic.com), then set it as
an environment variable - **never commit it to git**:

```bash
# Windows (PowerShell)
setx ANTHROPIC_API_KEY "your-key-here"

# Mac/Linux
export ANTHROPIC_API_KEY="your-key-here"
```

### 2. Re-authorize Gmail (new scope)
The app now requests `gmail.compose` in addition to `gmail.readonly`, so it
can create drafts. You must re-run the OAuth flow once:

1. Stop the app if it's running.
2. Delete the `tokens/` folder in your project root (it holds your old
   read-only token).
3. Start the app again - it will open a browser window asking you to
   re-authorize. Approve it (you'll now see a "compose email drafts" permission
   listed).

### 3. Create the new database table
Run the migration once against your existing SQLite DB:

```bash
sqlite3 job-email-agent.db < migrations/001_create_processed_jobs.sql
```

(If you don't have the `sqlite3` CLI, any SQLite browser tool / DB Browser for
SQLite works too - just execute the SQL in `001_create_processed_jobs.sql`.)

### 4. (Optional) Adzuna job board
RemoteOK and Arbeitnow work out of the box with no keys. If you also want
Adzuna (US-focused aggregator), sign up for a free key at
https://developer.adzuna.com/ and set:

```bash
export ADZUNA_APP_ID="..."
export ADZUNA_APP_KEY="..."
```

If you skip this, Adzuna is silently skipped - no errors.

### 5. Run it
```bash
./mvnw spring-boot:run
```

On startup, and then every hour, the app will:
1. Check Gmail for new job alerts / recruiter emails (matched against your resume)
2. Search RemoteOK + Arbeitnow (+ Adzuna if configured) for "Java Developer",
   "Backend Engineer", and "Java" roles
3. For strong matches (score >= 60 for jobs, >= 50 for recruiter emails),
   generate a tailored draft email via Claude and save it to your **Gmail
   Drafts** folder for you to review and send

## Tuning

In `application.properties`:
- `job.search.keywords` - comma-separated search terms
- `job.match.draft-threshold` - minimum score (0-100) to draft for web jobs
- `email.match.draft-threshold` - minimum score (0-100) to draft replies to recruiter emails
- `anthropic.model` - swap to `claude-sonnet-4-6-...` for higher-quality drafts (more $)

## Notes on web sources

- **RemoteOK** and **Arbeitnow** are free, public APIs - no scraping of
  LinkedIn/Indeed (which would violate their terms of service).
- Drafts for web job postings are saved with an empty "To" field (since
  there's no direct contact) - the draft body includes the application link
  so you can apply via the portal, or copy the text into the site's contact form.
- Drafts for recruiter emails are addressed back to the sender as a reply.
