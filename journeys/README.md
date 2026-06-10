# Agentic journey tests

This directory holds **journey** tests for Mandacaru: XML files of natural-language
`<action>` steps that an AI agent (via the `android-cli` skill) executes and verifies
against the running app on a device.

A journey passes only if every `<action>` succeeds and the app never crashes, freezes,
or exits. Steps that start with "verify" / "check" assert on the current screen state;
all other steps are interactions.

## Prerequisites

- One ARM64 (`arm64-v8a`) device or emulator connected — the bundled Rust `.so` is
  ARM64-only.
- A debug build installed: `./gradlew installDebug`.
- The `android-cli` skill available to the agent (`android layout`, `android screen`,
  `adb shell input`).

## Running a journey

Hand a journey file to the agent and ask it to evaluate it, e.g. "run
`journeys/navigate_tabs.xml`". The agent drives the app with `adb shell input`, inspects
state with `android layout`, and reports a per-action PASSED/FAILED/SKIPPED summary.

Inspect the live UI tree (and confirm tags resolve to ids) with:

```bash
android layout --pretty
```

Note: `android layout` reports the tag under the `resource-id` key (e.g.
`"resource-id":"nav_settings"`).

## Cold-start splash

On a cold start the app shows a ~4-second splash before the Node screen appears. A
"Launch the app" step must wait for the splash to dismiss (poll `android layout` until a
known tag such as `nav_node` appears) before asserting on screen content.

## Identifying the current screen

Navigation is a `HorizontalPager`, not a `NavController`, and adjacent pages stay
composed — so content from the neighbouring screen can be present (off-screen) in the
tree. The reliable signal for "which screen is active" is the **selected nav item**: the
active destination's nav element carries `"state":["selected"]` in the layout dump
(e.g. `nav_blockchain` is selected on the Blockchain screen). Combine that with a
screen-unique element (see tables below) when an extra check is wanted.

Container/screen-root tags are intentionally not used: a Compose layout node carrying
only a `testTag` is not important-for-accessibility and does not surface in
`android layout`. Only nodes that already emit semantics (interactive controls, text,
nav items) appear.

## testTag contract

Compose `testTag`s surface under the `resource-id` key in `android layout` output
because the root sets `testTagsAsResourceId = true` (see `MainActivity.MandacaruRoot`).
Agents should prefer targeting these stable ids over localized `text` or raw `bounds`.

Tags are inline string literals at each call site (no shared constants file, to avoid
merge conflicts across branches). This README is the canonical list — keep it in sync
when adding or renaming a tag.

### Navigation (`MainActivity.kt`)

| resource-id        | element                                  |
|-------------------|------------------------------------------|
| `nav_node`        | Node Info bottom-nav / rail item         |
| `nav_blockchain`  | Blockchain nav item                      |
| `nav_transaction` | Transactions nav item                    |
| `nav_settings`    | Settings nav item                        |

### Node screen (`node/ScreenNode.kt`)

| resource-id              | element                         |
|-------------------------|---------------------------------|
| `node_sync_percentage`  | sync progress percentage        |
| `node_network`          | network value                   |
| `node_peer_count`       | number-of-peers value           |
| `node_difficulty`       | difficulty value                |
| `node_disconnect_peer`  | per-peer disconnect button      |

#### Utreexo paste sheet (`node/UtreexoPasteSheet.kt`)

| resource-id              | element                                  |
|-------------------------|------------------------------------------|
| `input_utreexo_payload` | snapshot payload text field              |
| `button_paste_clipboard`| "Paste from clipboard" button            |
| `button_import_payload` | "Import" submit button                   |

These ids are declared for the instrumented Compose tests. The paste sheet is a
`ModalBottomSheet`, so (per the popup caveat below) its tags do **not** surface as
`resource-id` in `android layout` — in journeys, target its controls by **text**
("Paste from clipboard", "Import") instead.

When a valid accumulator for the current network is on the clipboard, the Node screen
shows a clipboard-import **snackbar** ("Accumulator found on clipboard" + an "Import"
action) on open. The snackbar is part of the Scaffold subtree, so its text and action
**are** targetable by text in `android layout`.

### Blockchain screen (`blockchain/ScreenBlockchain.kt`)

| resource-id                  | element                  |
|-----------------------------|--------------------------|
| `blockchain_block_height`   | current block height     |
| `button_view_latest_block`  | "View latest block"      |
| `input_block`               | block lookup field       |

### Transactions screen (`transaction/ScreenTransaction.kt`)

| resource-id               | element                       |
|--------------------------|-------------------------------|
| `input_txid`             | transaction-id lookup field   |
| `input_rawtx`            | raw-tx broadcast field        |
| `button_broadcast`       | "Broadcast"                   |
| `button_scan_broadcast`  | "Scan to broadcast"           |

### Settings screen (`settings/ScreenSettings.kt`)

| resource-id                  | element                                |
|-----------------------------|----------------------------------------|
| `input_descriptor`          | wallet descriptor field                |
| `button_update_descriptor`  | "Update descriptor"                    |
| `button_scan_descriptor`    | "Scan QR" (opens the descriptor scanner)|
| `input_network`             | network selector field                 |
| `toggle_mobile_data`        | "Also use mobile data" switch          |

`button_scan_descriptor` opens `DescriptorScanSheet` (a `ModalBottomSheet`) and, on a
successful scan, `DescriptorScanConfirmDialog` (an `AlertDialog`). Both render in a
separate window (per the popup caveat below), so their controls — "Paste instead",
"Decode", "Load", "Cancel", the decoded descriptor and its script type — are targeted by
**text**, not `resource-id`.

The Data usage section's `toggle_mobile_data` switch is off by default (Wi-Fi only);
turning it on persists the preference and restarts the app. Expand the "Data usage"
section by text before asserting on it.

Tapping `input_network` opens the network dropdown. Its options are **targeted by text**
(`BITCOIN`, `SIGNET`, `TESTNET`, `REGTEST`, `TESTNET4`) — see the popup caveat below.

## Popups, dropdowns, and dialogs

`testTagsAsResourceId` is set on the app's root composable, but Compose renders dropdowns
(`ExposedDropdownMenu`), dialogs, and bottom sheets in a **separate window** outside that
subtree, so their `testTag`s do **not** surface as `resource-id`. Their `text` and
`content-desc` still appear in `android layout`. Target items inside these popups by their
visible text (e.g. tap the `SIGNET` menu item by text).

## Cross-checking node state

Several journeys verify sync/peer state shown on screen. To confirm independently,
forward the RPC port and query the daemon directly:

```bash
adb forward tcp:8332 tcp:8332
curl -s -X POST http://127.0.0.1:8332 \
  -d '{"jsonrpc":"2.0","method":"getblockchaininfo","params":[],"id":1}'
```

(Port is per-network: 8332 mainnet, 38332 signet, 18332 testnet, 18443 regtest.)
