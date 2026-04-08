Update UniFFI bindings for the Mandacaru Android app.

Run the `update-bindings.sh` script from the mandacaru repo root. This script:

1. Cross-compiles `floresta-mandacaru-ffi` for Android ARM64 (aarch64-linux-android)
2. Generates Kotlin bindings from the UDL interface definition
3. Copies the native `.so` library to `app/src/main/jniLibs/arm64-v8a/libuniffi_floresta.so`
4. Copies the Kotlin bindings to `app/src/main/java/com/florestad/florestad.kt` (with correct package name)

Execute: `bash ./update-bindings.sh`

After the script completes, report what changed (file sizes, any compilation warnings) and whether the mandacaru Android project builds successfully with `./gradlew assembleDebug`.