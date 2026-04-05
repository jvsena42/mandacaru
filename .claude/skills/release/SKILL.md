---
name: release
description: Build signed APK, create git tag, and publish GitHub release
disable-model-invocation: true
argument-hint: "<version> (e.g. v0.2.0)"
---

Release process for Mandacaru. Version: $ARGUMENTS

## Steps

1. **Validate version argument**: Ensure a version was provided (e.g. `v0.2.0`). It must start with `v` followed by semver. Abort if missing or malformed.

2. **Pre-flight checks**:
   - Ensure working tree is clean (`git status`). Abort if there are uncommitted changes.
   - Ensure you are on the `main` branch.
   - Run tests: `./gradlew test`. Abort if tests fail.

3. **Bump version**:
   - Extract the numeric version (strip the `v` prefix, e.g. `v0.2.0` -> `0.2.0`).
   - Update `versionName` in `app/build.gradle.kts` to the new version.
   - Increment `versionCode` by 1 in `app/build.gradle.kts`.
   - Commit the version bump: `chore: bump version to <version>`.

4. **Build signed APK**:
   - Run `./gradlew clean assembleRelease`.
   - Verify the APK exists at `app/build/outputs/apk/release/app-release.apk`.

5. **Create git tag**:
   - Create annotated tag: `git tag -a <version> -m "Release <version>"`.
   - Push the tag: `git push origin <version>`.
   - Push the commit: `git push origin main`.

6. **Generate changelog**:
   - Find the previous tag: `git describe --tags --abbrev=0 HEAD~1` (if no previous tag exists, use all commits).
   - List commits since the previous tag: `git log <previous_tag>..HEAD --oneline --no-merges`.
   - Write a short changelog as a bullet-point list summarizing the user-facing changes (group related commits, skip chore/CI-only commits, keep each bullet to one sentence in English).
   - Show the changelog to the user for approval before proceeding.

7. **Create GitHub release**:
   - Use `gh release create <version>` with the signed APK attached.
   - Title: `Mandacaru <version>`.
   - Use the approved changelog as the release body (pass via `--notes`).
   - Mark as latest release.

8. **Summary**: Print the release URL and confirm success.

## Important

- Abort immediately if any step fails.
- Ask the user for confirmation before pushing the tag and creating the release.
- Never skip tests or signing verification.