---
name: release
description: Bump the version, write the changelog, and push a release tag that CD builds, signs, and publishes
disable-model-invocation: true
argument-hint: "<version> (e.g. v0.2.0)"
---

Release process for Mandacaru. Version: $ARGUMENTS

Building, signing, and publishing run in CD (`.github/workflows/release.yml`), triggered by pushing a `v*` tag. That workflow checks that the tag is `vX.Y.Z`, matches `appVersionName`, and points at a commit on `main`. It then runs `./gradlew test`, builds the signed APK, checks that it is signed with the release certificate, and creates the GitHub release with the APK attached. The release notes come from the tag annotation body. This skill handles only the parts that must happen locally: the version-bump commit (`main` is protected, so CI cannot push to it) and the human-approved changelog.

## Steps

1. **Validate version argument**: Ensure a version was provided (e.g. `v0.2.0`). It must start with `v` followed by semver. Abort if missing or malformed.

2. **Pre-flight checks**:
   - Ensure working tree is clean (`git status`). Abort if there are uncommitted changes.
   - Ensure you are on the `main` branch and up to date with `origin/main`.
   - Ensure the tag does not already exist locally or on `origin`.

3. **Bump version**:
   - Extract the numeric version (strip the `v` prefix, e.g. `v0.2.0` -> `0.2.0`).
   - Update the `appVersionName` top-level `val` in `app/build.gradle.kts` to the new numeric version. This single `val` feeds both `defaultConfig.versionName` and the APK output name, so it is the only place to change.
   - Increment `versionCode` by 1 in `app/build.gradle.kts`.
   - Commit the version bump: `chore: bump version to <version>`.

4. **Generate changelog**:
   - Find the previous tag: `git describe --tags --abbrev=0 HEAD~1` (if no previous tag exists, use all commits).
   - List commits since the previous tag: `git log <previous_tag>..HEAD --oneline --no-merges`.
   - Write a short changelog as a bullet-point list summarizing the user-facing changes (group related commits, skip chore/CI-only commits, keep each bullet to one sentence in English).
   - Show the changelog to the user for approval before proceeding.

5. **Tag and push** (ask the user for confirmation first):
   - Create an annotated tag whose subject is the title and whose body is the approved changelog: `git tag -a <version> -m "Release <version>" -m "<changelog>"`. CD publishes the body verbatim as the release notes.
   - Push the commit, then the tag: `git push origin main && git push origin <version>`.

6. **Watch CD**:
   - Find the run: `gh run list --workflow release.yml --limit 1`, then `gh run watch <run-id> --exit-status`.
   - On success, print the release URL (`gh release view <version> --json url -q .url`).
   - On failure, show the failing step's log (`gh run view <run-id> --log-failed`). Do not delete or move the pushed tag without asking the user. To retry after a fix that needs no new commit, use `gh run rerun <run-id>`.

## Important

- Abort immediately if any step fails.
- Never build or upload the release APK locally. CD is the only publisher, so every release is built from the tagged commit and checked against the release certificate.
