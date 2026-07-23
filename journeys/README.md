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
| `node_peer_flag`        | per-peer country flag (see below) |

`node_peer_flag` repeats once per peer, but **only for peers whose IP resolves to a country** —
it is absent for private/LAN, onion and unknown peers, and absent for *every* peer until the
GeoIP database has downloaded (see "Peer country flags" below). Do not treat a missing flag as a
failure. It surfaces in `android layout` because it carries a `contentDescription` (the localized
country name, e.g. "Ukraine"), which is also what TalkBack announces.

##### Peer country flags

The flags come from a DB-IP database the app downloads on first launch (~4 MB, WiFi-gated,
refreshed at most monthly), not from anything bundled in the APK. On a fresh install expect **no
flags at all** until that download lands; they then appear on the next 10-second poll without a
restart. To check whether the database is present:

```bash
adb shell run-as com.github.jvsena42.mandacaru ls -l files/dbip-country.mmdb   # ~8.2 MB when installed
```

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
| `button_share_descriptor`   | a loaded descriptor row — tap to open the share sheet |
| `tab_descriptor`            | "Descriptor" tab inside the share sheet (see popup caveat) |
| `tab_extended_key`         | "Extended key" tab inside the share sheet (see popup caveat) |
| `button_copy_descriptor`    | Copy button on the share sheet's Descriptor tab |
| `button_copy_extended_key`  | Copy button on the share sheet's Extended-key tab |
| `input_network`             | network selector field — inside the Network section, only when advanced features are on |
| `toggle_mobile_data`        | "Also use mobile data" switch          |
| `toggle_peer_flags`         | "Peer country flags" switch — inside its own expandable section, only when advanced features are on |
| `toggle_advanced_features`  | "Advanced features" switch (gates the Network, Peer country flags and Developer Tools sections) |
| `button_view_logs`          | "View logs" (opens the full-screen log viewer) |
| `button_export_logs`        | "Export" (share the full debug.log) — inside Developer Tools |

`button_share_descriptor` is applied to each loaded descriptor row, so the tag repeats once
per descriptor — target the first when more than one is present. Tapping a row opens
`DescriptorShareSheet` (a `ModalBottomSheet`) with two tabs: **Descriptor** (default — the full
descriptor for modern wallets) and **Extended key** (the SLIP-132 `zpub`/`ypub`/`xpub` Electrum
expects). Each tab shows a QR, the full key text, and a Copy button. Because the sheet renders in a
separate window (per the popup caveat below), switch tabs **by text** ("Descriptor" / "Extended
key"); the `tab_*` / `button_copy_*` tags are for instrumented Compose tests. For a multisig or
taproot descriptor the Extended-key tab shows a "not available" notice instead of a key. Copying
shows a "Descriptor copied to clipboard" / "Extended public key copied to clipboard" snackbar.

`button_scan_descriptor` opens `DescriptorScanSheet` (a `ModalBottomSheet`) and, on a
successful scan, `DescriptorScanConfirmDialog` (an `AlertDialog`). Both render in a
separate window (per the popup caveat below), so their controls — "Paste instead",
"Decode", "Load", "Cancel", the decoded descriptor and its script type — are targeted by
**text**, not `resource-id`.

The Data usage section's `toggle_mobile_data` switch is off by default (Wi-Fi only);
turning it on persists the preference and restarts the app. Expand the "Data usage"
section by text before asserting on it.

`toggle_peer_flags` lives in the collapsible **"Peer country flags"** section, which sits directly
above "Node" and only renders when `toggle_advanced_features` is on. So: turn advanced features on
first, then expand "Peer country flags" by text before the switch surfaces. Unlike the other two
switches it is **on** by default. Turning it off stops the monthly database download and removes
`node_peer_flag` from every peer row within one 10-second poll, with no restart.

`toggle_advanced_features` defaults to **on in debug builds and off in release builds**, so on the
debug build journeys run against, the gated sections are visible without touching it — but a
journey that depends on them should still confirm the switch is on (it persists across restarts,
so a previous run may have turned it off). The **Network**, **Peer country flags** and **Developer
Tools** sections only render while the toggle is on. Expand
"Developer Tools" by text, then tap `button_view_logs` to open `ScreenDeveloperLogs` — a
full-screen Nav3 destination in the root subtree, so its tags surface normally (the popup
caveat does **not** apply). There: `button_back_logs` returns to Settings, `button_copy_logs`
copies the displayed tail, and the share action reuses `button_export_logs`. Each log line
is colored by level (ERROR/WARN/INFO/DEBUG/TRACE).

The **Network** section only renders while `toggle_advanced_features` is on, so turn advanced
features on first, then expand "Network" by text before `input_network` surfaces. Tapping
`input_network` opens the network dropdown. Its options are **targeted by text**
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
