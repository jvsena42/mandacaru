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

Inspect the live UI tree (and confirm tags resolve to `resourceId`s) with:

```bash
android layout --pretty
```

## testTag contract

Compose `testTag`s surface as `resourceId` in `android layout` output because the root
sets `testTagsAsResourceId = true` (see `MainActivity.MandacaruRoot`). Agents should
prefer targeting these stable ids over localized `text` or raw `bounds`.

Tags are inline string literals at each call site (no shared constants file, to avoid
merge conflicts across branches). This README is the canonical list — keep it in sync
when adding or renaming a tag.

### Navigation (`MainActivity.kt`)

| resourceId        | element                                  |
|-------------------|------------------------------------------|
| `nav_node`        | Node Info bottom-nav / rail item         |
| `nav_blockchain`  | Blockchain nav item                      |
| `nav_transaction` | Transactions nav item                    |
| `nav_settings`    | Settings nav item                        |

### Screen roots

| resourceId           | screen           |
|----------------------|------------------|
| `screen_node`        | Node             |
| `screen_blockchain`  | Blockchain       |
| `screen_transaction` | Transactions     |
| `screen_settings`    | Settings         |

### Node screen (`node/ScreenNode.kt`)

| resourceId              | element                         |
|-------------------------|---------------------------------|
| `node_sync_percentage`  | sync progress percentage        |
| `node_network`          | network value                   |
| `node_peer_count`       | number-of-peers value           |
| `node_difficulty`       | difficulty value                |
| `node_disconnect_peer`  | per-peer disconnect button      |

### Blockchain screen (`blockchain/ScreenBlockchain.kt`)

| resourceId                  | element                  |
|-----------------------------|--------------------------|
| `blockchain_block_height`   | current block height     |
| `button_view_latest_block`  | "View latest block"      |
| `input_block`               | block lookup field       |

### Transactions screen (`transaction/ScreenTransaction.kt`)

| resourceId               | element                       |
|--------------------------|-------------------------------|
| `input_txid`             | transaction-id lookup field   |
| `input_rawtx`            | raw-tx broadcast field        |
| `button_broadcast`       | "Broadcast"                   |
| `button_scan_broadcast`  | "Scan to broadcast"           |

### Settings screen (`settings/ScreenSettings.kt`)

| resourceId                  | element                                |
|-----------------------------|----------------------------------------|
| `input_descriptor`          | wallet descriptor field                |
| `button_update_descriptor`  | "Update descriptor"                    |
| `input_network`             | network selector field                 |
| `network_option_<NAME>`     | dropdown option per `Network` enum     |

`Network` enum names: `BITCOIN`, `SIGNET`, `TESTNET`, `REGTEST`, `TESTNET4` — so the
options are `network_option_BITCOIN`, `network_option_SIGNET`, etc.

## Cross-checking node state

Several journeys verify sync/peer state shown on screen. To confirm independently,
forward the RPC port and query the daemon directly:

```bash
adb forward tcp:8332 tcp:8332
curl -s -X POST http://127.0.0.1:8332 \
  -d '{"jsonrpc":"2.0","method":"getblockchaininfo","params":[],"id":1}'
```

(Port is per-network: 8332 mainnet, 38332 signet, 18332 testnet, 18443 regtest.)
