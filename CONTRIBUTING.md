# How we work

## One-time setup

```bash
git clone <repo-url>
cd legacyloop
git config user.name "Your Name"
git config user.email "you@example.com"
cp .env.example .env
```

Branches that already exist: `main` (demo-ready only) and `develop` (integration). You never
commit to either directly.

## Every day

```bash
git checkout develop
git pull origin develop          # start from what everyone else has pushed

git checkout -b feature/jobs-search
# ... write code ...

git add .
git commit -m "feat(jobs): search jobs by title and location"
git push -u origin feature/jobs-search
```

Then open a pull request into `develop` on GitHub, tag the lead, and wait for one approval.

## Branch names

```
feature/<area>-<what>     feature/auth-login, feature/feed-comments
fix/<area>-<what>         fix/jobs-null-salary
chore/<what>              chore/add-swagger-config
```

Use your slice name from MODULES.md as `<area>` so it is obvious at a glance who owns a branch.

## Commit messages

```
<type>(<area>): <what changed, lower case, no full stop>

feat(auth): issue refresh token on login
fix(resume): reject PDFs larger than 5 MB
refactor(feed): move like counting into PostService
docs(api): document application status transitions
test(jobs): cover expired job filter
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.

## Before you push, always

```bash
cd backend && mvn clean install -DskipTests   # it must compile
cd ../frontend && npm run build               # it must build
```

A push that breaks the build blocks five other people.

## Pulling other people's work

```bash
git checkout develop
git pull origin develop
git checkout feature/your-branch
git merge develop
```

Merge `develop` into your branch, not the other way round. Do it at least once a day — small
merges are painless, week-old ones are not.

## If you hit a conflict

A conflict means two people edited the same lines. Open the file, keep both sides' intent, remove
the `<<<<<<<`, `=======`, `>>>>>>>` markers, then:

```bash
git add <file>
git commit
```

If the conflict is in a file you do not own, message the owner instead of guessing.

## Rules that keep this working

1. Never commit `.env`, `target/`, `node_modules/`, or a `.jar`. `.gitignore` handles it — do not
   force-add them.
2. Never `git push --force` to `develop` or `main`.
3. Never commit an API key, a database password, or a JWT secret. They go in `.env`, and
   `application.yml` reads them as `${GEMINI_API_KEY}`.
4. Never edit a file you do not own (see MODULES.md).
5. Commit small and often. One commit per working piece, not one per day.
6. Delete your branch after it is merged.

## Definition of done for a feature

- Endpoint returns `ApiResponse`, errors thrown as `ApiException` with an `ErrorCode`
- Request DTO validated with `@Valid` and Jakarta validation annotations
- Role rule on the controller method with `@PreAuthorize`
- No entity returned directly from a controller
- Listed in `docs/API.md`
- At least one test
- Screen renders the loading, empty, and error states, not just the happy path
