# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mandacaru is an Android application that runs a lightweight Bitcoin validation node powered by Utreexo and Floresta. It bridges Rust-based Bitcoin node implementation with a modern Kotlin/Compose Android UI.

## Build & Development Commands

### Building the Project
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.github.jvsena42.mandacaru.SpecificTestClass"
```

### Code Quality
```bash
# Clean build
./gradlew clean

# Check for updates
./gradlew dependencyUpdates
```

### Development Setup
- Requires Android 10 (API 29) minimum
- ARM64 device only (arm64-v8a) - the Rust library is compiled for ARM64 only
- Java 11 language target
- Android Studio with Compose support recommended

## Architecture

### Layer Structure (Clean Architecture + MVVM)

```
presentation/           # UI layer with Jetpack Compose
├── ui/screens/        # Screen-specific ViewModels and Composables
│   ├── main/          # MainActivity and navigation
│   ├── node/          # Node status screen
│   ├── search/        # Transaction search screen
│   └── settings/      # Settings and configuration
├── ui/components/     # Reusable Compose components
└── utils/             # UI utilities (EventFlow, notifications)

domain/                # Business logic layer
├── floresta/         # Floresta daemon integration
│   ├── FlorestaDaemon.kt        # Daemon lifecycle interface
│   ├── FlorestaDaemonImpl.kt    # Rust/FFI integration
│   ├── FlorestaService.kt       # Android foreground service
│   └── FlorestaRpcImpl.kt       # RPC client implementation
└── model/            # Domain models and RPC DTOs

data/                 # Data layer
├── FlorestaRpc.kt           # RPC interface
└── PreferencesDataSource.kt  # Preferences abstraction
```

### Key Architectural Patterns

- **MVVM**: ViewModels expose `StateFlow<UiState>` consumed by Compose UI
- **Repository Pattern**: `FlorestaRpc` interface abstracts RPC communication
- **Dependency Injection**: Koin manages singleton instances (configured in `MandacaruApplication.kt`)
- **Coroutines + Flow**: All async operations use `suspend` functions and `Flow<Result<T>>`
- **Foreground Service**: `FlorestaService` keeps the Bitcoin node alive in background

## Rust/Floresta Integration

### FFI Bridge (UniFFI)

The app communicates with Rust code via UniFFI-generated bindings:

- **Native Library**: `libuniffi_floresta.so` (ARM64 only)
- **Kotlin Bindings**: `com.florestad.florestad.kt` (auto-generated)
- **Main Classes**:
  - `Florestad`: Rust daemon wrapper with `start()`/`stop()` methods
  - `Config`: Configuration object passed to Rust (network, dataDir, etc.)
  - `Network`: Enum for Bitcoin/Testnet/Signet/Regtest

### Daemon Lifecycle

1. `FlorestaService.onStartCommand()` triggers `FlorestaDaemon.start()`
2. `FlorestaDaemonImpl.start()` reads network preference, creates `Config`, calls FFI
3. Rust daemon initializes Bitcoin node with Utreexo, binds RPC server to `127.0.0.1:<port>`
4. Android app communicates via JSON-RPC over HTTP

### Configuration by Network

Network preferences stored in SharedPreferences:
- **CURRENT_NETWORK**: "Bitcoin", "Testnet", "Signet", or "Regtest"
- **CURRENT_RPC_PORT**: Automatically set based on network (see `Constants.kt`)

Port mappings (defined in `domain/model/Constants.kt`):
- Bitcoin: 8332
- Testnet: 18332
- Signet: 38332
- Regtest: 18443

## RPC System

### JSON-RPC 2.0 Implementation

The app uses OkHttp3 to make HTTP POST requests to the Rust daemon's localhost RPC server.

**Request Format**:
```json
{
  "jsonrpc": "2.0",
  "method": "getblockchaininfo",
  "params": [],
  "id": 1
}
```

### Available RPC Methods (see `domain/model/florestaRPC/RpcMethods.kt`)

- `getblockchaininfo`: Blockchain sync status, height, Utreexo forest state
- `getpeerinfo`: Connected peer list
- `gettransaction`: Transaction lookup by txid
- `loaddescriptor`: Add wallet descriptor for address tracking
- `listdescriptors`: List loaded descriptors
- `addnode`: Connect to specific Bitcoin node
- `rescan`: Rescan blockchain for wallet transactions
- `stop`: Gracefully stop the node

### RPC Response Models

Located in `domain/model/florestaRPC/response/`:
- `GetBlockchainInfoResponse`: Contains Utreexo-specific fields (`leafCount`, `rootCount`, `rootHashes`)
- `GetTransactionResponse`: Full transaction details with inputs/outputs
- `GetPeerInfoResponse`: Peer connection info
- `AddNodeResponse`: Node connection status

### Error Handling

RPC methods return `Flow<Result<T>>`. Check for `Result.isFailure` to handle errors:
```kotlin
florestaRpc.getTransaction(txId).collect { result ->
    result.onSuccess { tx -> /* handle success */ }
    result.onFailure { error -> /* handle error */ }
}
```

## State Management

### ViewModel Pattern

Each screen has a ViewModel with:
- `private val _uiState = MutableStateFlow(UiState())`
- `val uiState = _uiState.asStateFlow()` (read-only exposure)
- Action handlers that update state via `_uiState.update { ... }`

### UI Observation

Compose screens observe state:
```kotlin
val state by viewModel.uiState.collectAsState()
```

### Background Polling

`NodeViewModel` implements a 10-second polling loop (`getInLoop()`) to refresh blockchain info while the screen is active.

## Important Implementation Notes

### Network Changes Require App Restart

When the user changes network in Settings:
1. New network is saved to SharedPreferences
2. `SettingsEvents.OnNetworkChanged` is broadcast
3. `MainActivity` receives event and calls `restartApplication()`
4. App restarts with new `Config` passed to Rust daemon

### Foreground Service Notifications

- Service notification is mandatory (Android O+)
- Channel: "floresta_service_channel"
- Notification actions: Open app, Stop service
- POST_NOTIFICATIONS permission required for Android 13+

### Transaction Search Validation

The search field validates txid format (64-character hex string) before making RPC calls. Debouncing (500ms) prevents excessive API requests.

### Descriptor Format

When loading wallet descriptors via Settings, use standard Bitcoin descriptor format (e.g., `wpkh([fingerprint/path]xpub...)`). The Rust daemon validates and derives addresses.

## Testing Considerations

### Unit Tests
- Test ViewModels with fake repositories
- Mock `FlorestaRpc` interface for predictable responses
- Use Koin test utilities to inject test dependencies

### Instrumented Tests
- Requires Android emulator or physical device
- Service tests need proper permission setup
- RPC tests require running Rust daemon

## Common Development Scenarios

### Adding a New RPC Method

1. Add enum to `RpcMethods.kt`
2. Create response model in `domain/model/florestaRPC/response/`
3. Add method to `FlorestaRpc` interface
4. Implement in `FlorestaRpcImpl.sendJsonRpcRequest()`
5. Call from ViewModel, update UI state

### Adding a New Screen

1. Create package under `presentation/ui/screens/`
2. Create ViewModel with `UiState` data class
3. Create Composable screen function
4. Add route to `MainActivity` NavHost
5. Register ViewModel in Koin `presentationModule`

### Modifying Rust Configuration

1. Update `Config` data class in `FlorestaDaemonImpl.kt`
2. Ensure matching fields exist in Rust `Config` struct
3. Rebuild UniFFI bindings (handled by Rust build)
4. Update `start()` initialization logic

## Dependencies

Key libraries (see `gradle/libs.versions.toml`):
- **Compose BOM**: 2025.09.01 (Material Design 3)
- **Navigation Compose**: 2.9.5
- **Koin BOM**: 4.1.1 (DI framework)
- **OkHttp**: 5.1.0 (HTTP client)
- **Gson**: 2.11.0 (JSON serialization)
- **JNA**: 5.14.0 (FFI bridge)

## Pull request policy

When publishing changes that touch the Mandacaru build chain — `Floresta-mandacaru`, `floresta-mandacaru-ffi`, or `mandacaru` — open PRs **against the fork's own default branch** (`jvsena42/<repo>:master` or `:main`). Never open PRs against an upstream maintainer repo (e.g. `vinteumorg/Floresta`) without explicit per-task authorization from the user — "if necessary" or similar conditional phrasing does not count as authorization.

The personal forks carry Mandacaru-specific patches and the user reviews/lands changes there first; upstream PRs are a separate, deliberate decision.

## Related Resources

- [Floresta Core](https://github.com/vinteumorg/Floresta) - Underlying Rust implementation
- [Utreexo](https://dci.mit.edu/utreexo) - Accumulator design
- [UniFFI](https://mozilla.github.io/uniffi-rs/) - Rust FFI bindings generator