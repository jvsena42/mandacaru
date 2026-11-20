# Agentic journey tests

This directory holds **journey** tests for Mandacaru: XML files of natural-language
`<action>` steps that an AI agent (via the `android-cli` skill) executes and verifies
against the running app on a device.

A journey passes only if every `<action>` succeeds and the app never crashes, freezes,
or exits. Steps that start with "verify" / "check" assert on the current screen state;
all other steps are interactions.

## Prerequisites

- One ARM64 (`arm64-v8a`) device or x86_64 emulator connected — the Rust `.so` is
  built for both ABIs (32-bit ABIs are not).
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

## Typing long text

`adb shell input text` with a long payload can be corrupted when Gboard's stylus
handwriting prompt pops up over the field. Disable it for the run with
`adb shell settings put secure stylus_handwriting_enabled 0` (restore with `1`).

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
| `snackbar_enable_notifications` | message text of the "Enable notifications…" snackbar (see below) |

`nav_settings` carries a `BadgedBox` dot when **either** an app update is unseen **or** no wallet
descriptor is loaded. The badge is decoration on the same node, so it does not change the
resource-id — do not assert on it.

`snackbar_enable_notifications` is hosted by the root `Scaffold`, so it is visible on **every**
tab, not just Node. It is **conditional**: it appears whenever notifications are disabled and the
user has not dismissed it, and it disappears by itself the moment the permission is granted — by
the system dialog or from the settings page — with no timeout otherwise. Its **message changes
after a denial**: it opens as the "Enable notifications to see when the node is running"
invitation and escalates to the warning "Notifications are disabled, wallets can't access your
Electrum server", so match on the resource-id rather than the text. The app never opens the
permission dialog on its own; it opens only when the snackbar's action is tapped. That action
reads "Enable" until a request is denied with no rationale left (Android's automatic "don't ask
again", after two denials), at which point it becomes "Open settings" and launches the app's
system settings page. Its ✕ dismisses it for the rest of the process. Target the action by its
"Enable"/"Open settings" text and the ✕ by the "Dismiss" content-description. On Android 12 and
below there is no runtime permission, so it appears only if notifications were turned off in
system settings. While it is showing, `snackbar_add_descriptor` is deliberately suppressed so the
two prompts cannot overlap. Reset it for a journey with
`adb shell pm revoke com.github.jvsena42.mandacaru android.permission.POST_NOTIFICATIONS`.

### Node screen (`node/ScreenNode.kt`)

| resource-id              | element                         |
|-------------------------|---------------------------------|
| `node_sync_percentage`  | sync progress percentage        |
| `node_sync_remaining`   | "N blocks/filters to go" under the progress bar, during block and filter sync only |
| `node_network`          | network value                   |
| `node_peer_count`       | number-of-peers value           |
| `node_difficulty`       | difficulty value                |
| `node_disconnect_peer`  | per-peer disconnect button      |
| `node_peer_flag`        | per-peer country flag (see below) |
| `node_peer_onion`       | per-peer 🧅 onion emoji, shown in place of the flag for `.onion` peers (content-description "Onion peer, connected over Tor") |
| `node_tor_restart_required` | title of the card shown when the running node's proxy differs from the saved Tor setting (a restart was missed) |
| `button_restart_for_tor` | "Restart now" button on that card |
| `node_tor_unreachable`  | title of the error card shown while Tor is on and no SOCKS5 proxy answers at the configured address |
| `snackbar_add_descriptor` | message text of the "Add a wallet descriptor…" snackbar (see below) |

`snackbar_add_descriptor` is **conditional**: it appears only once the daemon's RPC server has
answered `listdescriptors` twice with an empty wallet, so expect it to be **absent for the first
few seconds after the splash**, and absent entirely once any descriptor is loaded. It sits above
the bottom navigation bar and persists until acted on or dismissed — it has no timeout. Its "Add
descriptor" action switches to the Settings tab with the Descriptors section already expanded and
scrolled into view, so `input_descriptor` is on screen without any further scrolling; its ✕
dismisses it for the rest of the process (it returns on the next launch while no descriptor is
loaded). Target the action and the ✕ by their "Add descriptor" text and "Dismiss"
content-description.

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
| `button_add_descriptor`  | "Add descriptor" in the no-wallet empty state (see below) |

`button_add_descriptor` follows the same conditionality as `snackbar_add_descriptor`: it replaces
the grey "Only transactions from your loaded wallet descriptors can be found" hint at the bottom
of the lookup card while no descriptor is loaded, and reverts to that hint once one is. It
navigates to Settings exactly like the Node banner.

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
| `input_electrum_port`       | Electrum server port field (empty = network default) — inside the Network section, only when advanced features are on |
| `button_apply_electrum_port`| saves the Electrum port and restarts the app; disabled until the field differs from the saved value |
| `toggle_mobile_data`        | "Also use mobile data" switch          |
| `radio_tor_off`             | Tor mode "Off" — inside the Tor section, shown when advanced features are on or a Tor mode is saved; takes effect on apply |
| `radio_tor_onion_peers`     | Tor mode "Onion peers over Tor": only .onion peers go through the proxy, other peers connect directly |
| `radio_tor_all`             | Tor mode "Everything over Tor": every peer connection goes through the proxy |
| `input_tor_proxy`           | Tor SOCKS5 proxy field (`IPv4:port`, empty = `127.0.0.1:9050`) — visible while a Tor mode other than Off is selected |
| `button_apply_tor`          | probes the proxy when enabling, saves and restarts the app; disabled until the Tor settings differ from the saved ones |
| `toggle_peer_flags`         | "Peer country flags" switch — inside its own expandable section, only when advanced features are on |
| `toggle_advanced_features`  | "Advanced features" switch (gates the Network, Tor, Peer country flags and Developer Tools sections; Tor stays visible while it is on) |
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
COOKIE=$(adb shell run-as com.github.jvsena42.mandacaru cat files/.cookie)
curl -s -u "$COOKIE" -X POST http://127.0.0.1:8332 \
  -d '{"jsonrpc":"2.0","method":"getblockchaininfo","params":[],"id":1}'
```

The RPC server requires authentication: the node writes a fresh `user:password` cookie
to `.cookie` in its data dir on every start and deletes it on shutdown, so re-read it
after a restart. Without it every call returns HTTP 401.

(Port and data dir are per-network: 8332 and `files/` on mainnet; 38332 and
`files/signet/` on signet; 18332 and `files/testnet/` on testnet; 18443 and
`files/regtest/` on regtest.)
