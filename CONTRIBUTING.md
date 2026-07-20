# Contributing to Mandacaru 🌵

This guide is for **external contributors** and describes the basic practices expected of
any pull request. It is written for humans, and is also intended to be usable by automated
code-review bots in the future.

> **A note on maintenance.** Mandacaru is a personal project — I maintain it alone, on my
> free time. I review and land contributions whenever I can, so **reviews may take a long
> time**. That's expected; thank you for your patience. For anything non-trivial, please
> open an issue to discuss it *before* you build it — it saves everyone's time and avoids
> work that can't be merged.

## Before you start

- Build and run the app once so your environment is set up:
  ```bash
  ./gradlew assembleDebug     # build a debug APK
  ./gradlew installDebug      # install it on a connected device/emulator
  ```
- Requirements: Android 10 (API 29) minimum, an **arm64-v8a** device or an **x86_64**
  emulator. 32-bit ABIs are intentionally not built.
- For bugs and features, check the existing issues first and open one using the templates
  in [`.github/ISSUE_TEMPLATE`](.github/ISSUE_TEMPLATE) if none matches. For anything beyond
  a small fix, discuss the approach in an issue before writing code.

## Commits — atomic and conventional

Write **atomic commits**: one logical change per commit. Keep a change and its supporting
edits together (e.g. a new UI element and the journey/testTag docs that cover it), and keep
unrelated refactors in their own commits.

Use [Conventional Commits](https://www.conventionalcommits.org/): a `type: subject` line,
lowercase, in the imperative mood. The types used in this repo are:

`feat` · `fix` · `chore` · `refactor` · `docs` · `test` · `style` · `ci`

**Good examples** (real commits from this project):

```
feat: add an advanced setting to turn peer country flags off
fix: NPE when viewing the latest block header
docs: update journeys for the descriptor share sheet
```

**Avoid**: vague subjects like `fix: stuff`, or bundling an unrelated refactor into a
feature commit.

## Tests — cover business logic

Any change to business logic must come with unit tests. Test **ViewModels and pure
logic**, using **fakes rather than mocks** — the project favours JUnit4 with
`kotlinx-coroutines-test`.

- Tests live under `app/src/test/`, mirroring the main source package tree.
- Reuse the shared test doubles, e.g. [`fakes/FakeFlorestaRpc.kt`](app/src/test/java/com/github/jvsena42/mandacaru/fakes/FakeFlorestaRpc.kt).
- A representative ViewModel test to model yours on:
  [`TransactionViewModelTest`](app/src/test/java/com/github/jvsena42/mandacaru/presentation/ui/screens/transaction/TransactionViewModelTest.kt).
- Pure-logic tests set the bar too: `DescriptorUtilsTest`, `VersionComparatorTest`,
  `NetworkPortMappingTest`.

Run the suite:

```bash
./gradlew test
```

## UI changes — update journeys and testTags

The app ships **agentic UI journeys** in [`journeys/`](journeys/): XML files of
natural-language steps an AI agent drives against a running build.
[`journeys/README.md`](journeys/README.md) is the canonical **testTag contract**.

When you add or rename UI that an agent must drive:

1. Add an inline `testTag` string literal at the call site (tags are inline literals, not a
   shared constants file — this avoids cross-branch merge conflicts).
2. Document that tag in the matching screen table in [`journeys/README.md`](journeys/README.md).
3. Add or update the relevant `journeys/*.xml` file.

A journey step looks like this (from `journeys/navigate_tabs.xml`):

```xml
<action>Tap the Blockchain navigation item (nav_blockchain)</action>
<action>Verify the Blockchain navigation item (nav_blockchain) is selected</action>
```

Real precedent: the feature commit `feat: replace descriptor copy with a share sheet` was
paired with `docs: update journeys for the descriptor share sheet`.

Two gotchas worth knowing:

- A `testTag` only surfaces on a node that already emits semantics (interactive controls,
  text, nav items). A plain container `Box` with only a tag will not appear — so target the
  interactive child instead.
- Popups, dropdowns, dialogs and bottom sheets render in a separate window, so their
  `testTag`s do **not** surface — target those items by their visible text.

## Screenshots / videos for UI changes

Any PR that changes the UI **must** include before/after screenshots or a short screen
recording in the PR description. This makes visual review possible without checking out the
branch, and helps document the change. If a PR has no UI impact, write `N/A`.

## Self-review and local checks before requesting review

Before you request a review, **review your own diff** and make sure the following pass
locally (CI enforces them too):

```bash
./gradlew detekt        # static analysis  — config: config/detekt/detekt.yml
./gradlew lintDebug     # Android Lint      — config: app/lint.xml
./gradlew test          # unit tests
```

If a rule fires on generated code or on an intentional project decision, prefer **tuning
the config** over disabling the rule globally.

A couple of Kotlin conventions contributors trip on (see
[`AGENTS.md`](AGENTS.md) → *Kotlin Conventions* for the full list):

- Prefer `runCatching { ... }` over `try`/`catch`; inside `suspend` functions and coroutine
  builders use `runSuspendCatching { ... }` (plain `runCatching` swallows
  `CancellationException`).
- Import types and use the short name rather than fully-qualified names inline.

## Where to open your pull request

Open PRs against this repository's default branch (**`main`**). Keep each PR scoped to a
single concern so it can be reviewed and landed independently.

## Further reading

[`AGENTS.md`](AGENTS.md) holds the deeper architecture and build-chain guidance for this
project (`CLAUDE.md`, `GEMINI.md`, and the other agent entry points are symlinks to it). If
you use an AI coding assistant, point it there.
