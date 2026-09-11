# GitHub Actions build

This fork is configured for the repository's current `main` branch.

## What changed

- `push` on `main` triggers CI
- `pull_request` triggers CI
- `workflow_dispatch` enables the **Run workflow** button
- JDK 21
- Gradle cache via `gradle/actions/setup-gradle@v4`
- Unit tests
- `assembleDebug`
- Debug APK uploaded as `echo-meeting-notes-debug`

## Uploading to GitHub

Replace:

`.github/workflows/ci.yml`

with the version in this archive, commit, and push to `main`.

Then open:

Actions -> CI

A run should start automatically. You can also use:

Actions -> CI -> Run workflow

When it succeeds, download:

`Artifacts -> echo-meeting-notes-debug`
