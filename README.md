# Mandacaru 🌵

A lightweight Bitcoin validator node for Android, powered by [Utreexo](https://dci.mit.edu/utreexo) and [Floresta](https://github.com/vinteumorg/Floresta).

Run a validating Bitcoin node directly on your phone with minimal storage requirements thanks to Utreexo's compact accumulator design. Mandacaru uses `assumeutreexo` by default — see [the note below](#a-note-on-validation-assumeutreexo-is-enabled-by-default) for what that means for the trust model.

## Features

- ⚡ **Lightweight**: Uses Utreexo to dramatically reduce storage requirements
- 🔒 **Self-sovereign**: Validate Bitcoin transactions directly on your device
- 🌐 **Multi-network**: Support for Bitcoin Mainnet, Testnet, Testnet4, Signet, and Regtest
- 🔍 **Transaction & Broadcast**: Search transactions and broadcast raw transactions
- 🧱 **Blockchain Explorer**: Search blocks by height or hash, view headers and chain status
- 👥 **P2P Networking**: Connect, disconnect, and ping Bitcoin peers
- 💼 **Wallet Integration**: Load descriptors and track your Bitcoin addresses
- 🔌 **Electrum Server**: Built-in Electrum server for wallet pairing
- 📊 **Real-time Sync**: Monitor blockchain synchronization progress
- 🩺 **Diagnostics**: Monitor node uptime and memory usage
- 🎨 **Modern UI**: Beautiful Material Design 3 interface with dark/light themes

## Screenshots

<div align="center">

### Node Information
<table>
  <tr>
    <td><img src="screenshots/node_light.png" alt="Node Screen Light" width="250"/></td>
    <td><img src="screenshots/node_dark.png" alt="Node Screen Dark" width="250"/></td>
  </tr>
  <tr>
    <td align="center"><em>Light Theme</em></td>
    <td align="center"><em>Dark Theme</em></td>
  </tr>
</table>

### Transactions
<table>
  <tr>
    <td><img src="screenshots/transactions_light.png" alt="Transactions Screen Light" width="250"/></td>
    <td><img src="screenshots/transactions_dark.png" alt="Transactions Screen Dark" width="250"/></td>
  </tr>
  <tr>
    <td align="center"><em>Light Theme</em></td>
    <td align="center"><em>Dark Theme</em></td>
  </tr>
</table>

### Blockchain
<table>
  <tr>
    <td><img src="screenshots/blockchain_light.png" alt="Blockchain Screen Light" width="250"/></td>
    <td><img src="screenshots/blockchain_dark.png" alt="Blockchain Screen Dark" width="250"/></td>
  </tr>
  <tr>
    <td align="center"><em>Light Theme</em></td>
    <td align="center"><em>Dark Theme</em></td>
  </tr>
</table>

### Settings & Configuration
<table>
  <tr>
    <td><img src="screenshots/settings_light.png" alt="Settings Screen Light" width="250"/></td>
    <td><img src="screenshots/settings_dark.png" alt="Settings Screen Dark" width="250"/></td>
  </tr>
  <tr>
    <td align="center"><em>Light Theme</em></td>
    <td align="center"><em>Dark Theme</em></td>
  </tr>
</table>

</div>

## What is Utreexo?

Utreexo is a dynamic hash-based accumulator that allows Bitcoin nodes to validate the blockchain without storing the full UTXO set. This reduces storage requirements from tens of gigabytes to just a few megabytes, making it practical to run a validating node on mobile devices.

## A note on validation: `assumeutreexo` is enabled by default

Mandacaru ships with `assumeutreexo` turned on. At startup the node trusts a hardcoded Utreexo state snapshot (the accumulator roots at a specific block height) and begins full validation from that point forward — verifying every new block, transaction, signature, and consensus rule from the snapshot height onward.

What this means in practice:
- **Fast startup**: the node skips re-validating ancient history and is usable in minutes instead of days.
- **Full validation going forward**: from the snapshot height on, Mandacaru is a fully validating Bitcoin node — no trusted third party for new blocks.
- **A trust assumption about pre-snapshot history**: you are trusting that the bundled snapshot matches Bitcoin's true historical chain state. This is the same trade-off as Bitcoin Core's `assumeutxo` and `assumevalid`.

### Even with `assumeutreexo`, a bad accumulator is detectable

Under Utreexo, every transaction input in every new block must come with an inclusion proof against the current accumulator state. If the bundled snapshot were wrong — wrong roots, wrong height, doctored to hide or invent UTXOs — those proofs simply would not verify as honest peers relay real blocks: signatures would check out but inclusion proofs would fail, and Mandacaru would reject the chain rather than follow it.

In other words, a bad snapshot doesn't silently corrupt the node's view of Bitcoin; it makes the node unable to follow the real chain at all. The worst plausible failure mode is a stuck or refusing node, not a node that quietly accepts invalid history. This is what makes the `assumeutreexo` trade-off reasonable in practice: you accept faster startup in exchange for a trust assumption that is self-checking against the live network.

If you want zero trust assumptions, a from-genesis IBD (initial block download) without `assumeutreexo` is not currently exposed in the UI.

## Installation

### Requirements
- Android 10 (API 29) or higher
- ARM64 device (`arm64-v8a`) or x86_64 (`x86_64`, e.g. emulators)
- Internet connection

### Releases
Download the latest APK from [GitHub Releases](https://github.com/jvsena42/mandacaru/releases).

> ℹ️ The APK ships native libraries for **`arm64-v8a`** (physical devices) and **`x86_64`** (emulators/CI). 32-bit ABIs (`x86`, `armeabi-v7a`) are not supported.

### Obtainium
[Obtainium](https://github.com/ImranR98/Obtainium) installs and auto-updates apps directly from their GitHub releases — no Play Store or F-Droid required.

**One tap:** with Obtainium installed, open this link on your device to pre-fill the app:

```
obtainium://app/{"id":"com.github.jvsena42.mandacaru","url":"https://github.com/jvsena42/mandacaru","author":"jvsena42","name":"Mandacaru"}
```

**Manual:** in Obtainium, tap **Add App**, paste `https://github.com/jvsena42/mandacaru` into the *App Source URL* field, then tap **Add**.

### From Source
1. Clone the repository:
```bash
git clone https://github.com/jvsena42/mandacaru.git
cd mandacaru
```

2. Build the project:
```bash
./gradlew assembleDebug
```

3. Install on your device:
```bash
./gradlew installDebug
```

## Usage

### Getting Started
1. Launch the app and enable notifications when prompted
2. Add a wallet descriptor to track your addresses (in Settings > Descriptors)
3. Copy the Electrum server address (in Settings), then configure your wallet to use it as an Electrum server

### Node Info Screen
Monitor your node's status:
- **Sync Progress**: Current blockchain synchronization percentage
- **Network Info**: Connected network, peer count, and difficulty
- **Peers** (expandable): View connected peers with their country flag, connect to new nodes, disconnect, or ping
- **Diagnostics** (expandable): Node uptime and memory usage

### Transaction Screen
Search and broadcast transactions:
- Enter a transaction ID (txid) to look up transaction details
- Broadcast a raw transaction to the network
- View complete transaction details and confirmations

### Blockchain Screen
Explore the blockchain:
- **Block Search**: Look up blocks by height or hash
- **Chain Status**: View block count, best block hash, and validated blocks
- **Block Headers**: View detailed block header information

### Settings Screen
Configure your node:
- **Electrum Address**: Copy the local Electrum server address to pair with your wallet
- **Descriptors**: Add or list wallet descriptors to track your addresses
- **Network**: Switch between Bitcoin networks (requires app restart)
- **Node**: Connect directly to specific Bitcoin nodes
- **About**: App version and project information
- **Donate**: Support the project via Lightning

## Architecture

Built with modern Android development practices:
- **Kotlin**: 100% Kotlin codebase
- **Jetpack Compose**: Declarative UI framework
- **Material Design 3**: Modern, adaptive design system
- **HorizontalPager + BottomNavigationBar**: Swipeable screen navigation
- **MVVM Architecture**: Clean separation of concerns
- **Coroutines & Flow**: Async operations and reactive streams
- **DataStore**: Persistent preferences
- **Koin**: Lightweight dependency injection
- **OkHttp**: Network communication
- **JSON-RPC**: Bitcoin Core compatible RPC interface

## Peer geolocation data

The country flag next to each peer is resolved **entirely on-device** — a peer's IP address is
never sent to any geolocation service. The app downloads the
[DB-IP Lite Country](https://db-ip.com/db/download/ip-to-country-lite) database (~4 MB, over WiFi,
refreshed at most once a month) and reads it locally with
[maxmind-db](https://github.com/maxmind/MaxMind-DB-Reader-java). Until that download completes,
peers simply show no flag.

IP Geolocation by [DB-IP](https://db-ip.com), licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

## Related Projects

- [Floresta Wallet](https://github.com/jvsena42/floresta_app) - A wallet client running Floresta
- [Floresta Core](https://github.com/vinteumorg/Floresta) - The underlying Floresta implementation

## Support the Project

If you find this project useful, consider supporting development:

**Lightning**: `jvsena42@blink.sv`

## License

This project is open source. Please check the repository for license details.

## Acknowledgments

- Built on top of [Floresta](https://github.com/vinteumorg/Floresta)
- Implements [Utreexo](https://dci.mit.edu/utreexo) accumulator design
- Inspired by the Bitcoin community's commitment to decentralization

---

Made with ⚡ for the Bitcoin network
