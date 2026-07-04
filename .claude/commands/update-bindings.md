Update UniFFI bindings for the Mandacaru Android app.

Run the `update-bindings.sh` script from the mandacaru repo root. This script:

1. Cross-compiles `floresta-mandacaru-ffi` for Android `arm64-v8a` (aarch64-linux-android, physical devices) and `x86_64` (x86_64-linux-android, emulators/CI)
2. Generates Kotlin bindings from the compiled library (uniffi 0.31 `--library` mode; bindings are architecture-independent, generated once)
3. Copies each native `libflorestad_ffi.so` to its matching `app/src/main/jniLibs/<abi>/` dir (`arm64-v8a`, `x86_64`)
4. Copies the Kotlin bindings to `app/src/main/java/com/florestad/florestad.kt` (with correct package name)

Execute: `bash ./update-bindings.sh`

After the script completes, report what changed (file sizes, any compilation warnings) and whether the mandacaru Android project builds successfully with `./gradlew assembleDebug`.