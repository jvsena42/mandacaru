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

6. **Create GitHub release**:
   - Use `gh release create <version>` with the signed APK attached.
   - Title: `Mandacaru <version>`.
   - Generate release notes automatically using `--generate-notes`.
   - Mark as latest release.

7. **Summary**: Print the release URL and confirm success.

## Important

- Abort immediately if any step fails.
- Ask the user for confirmation before pushing the tag and creating the release.
- Never skip tests or signing verification.