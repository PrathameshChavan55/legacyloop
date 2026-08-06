## What this adds

<!-- one or two lines -->

## Module

<!-- your slice from MODULES.md, e.g. Jobs & applications -->

## Checklist

- [ ] `mvn clean install -DskipTests` passes
- [ ] `npm run build` passes (if the frontend changed)
- [ ] Endpoints return `ApiResponse` and throw `ApiException` on failure
- [ ] Request DTOs validated, role rule on the controller method
- [ ] No entity returned straight from a controller
- [ ] `docs/API.md` updated
- [ ] No `.env`, key, password or `target/` in the diff
- [ ] I only changed files my module owns (or agreed the change with the owner)

## How to test it

<!-- the exact curl or the click path -->
