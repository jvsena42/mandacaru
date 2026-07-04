#!/usr/bin/env bash
set -euo pipefail

# Update UniFFI bindings for Mandacaru Android app
#
# This script:
# 1. Cross-compiles floresta-mandacaru-ffi (with bitcoinkernel) for Android
#    arm64-v8a (aarch64-linux-android) and x86_64 (x86_64-linux-android, emulator)
# 2. Generates Kotlin bindings from the compiled library (uniffi 0.31 --library)
# 3. Copies each .so library into the matching jniLibs ABI dir, plus the Kotlin
#    bindings, into the mandacaru app
#
# Prerequisites:
# - Android NDK 27+ (ANDROID_NDK_ROOT or ~/Android/Sdk/ndk/<version>)
# - Boost dev headers + CMake config (libboost-all-dev on Debian/Ubuntu), needed
#   by bitcoinkernel's bundled Bitcoin Core build.
# - rustup targets aarch64-linux-android and x86_64-linux-android (added below).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FFI_DIR="$(cd "$SCRIPT_DIR/../floresta-mandacaru-ffi" && pwd)"
MANDACARU_DIR="$SCRIPT_DIR"

# Android NDK setup
NDK_VERSION="${NDK_VERSION:-27.0.12077973}"
ANDROID_SDK="${ANDROID_HOME:-${HOME}/Android/Sdk}"
NDK="${ANDROID_NDK_ROOT:-${ANDROID_SDK}/ndk/${NDK_VERSION}}"
TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${ANDROID_MIN_SDK:-29}"
PROFILE="release-smaller"
PROFILE_DIR="release-smaller"

# Validate NDK exists
if [ ! -d "$NDK" ]; then
    echo "Error: Android NDK not found at $NDK"
    echo "Set ANDROID_NDK_ROOT or ANDROID_HOME, or install NDK $NDK_VERSION"
    exit 1
fi

# Validate FFI crate exists
if [ ! -f "$FFI_DIR/Cargo.toml" ]; then
    echo "Error: floresta-mandacaru-ffi not found at $FFI_DIR"
    exit 1
fi

# Boost config location (headers + BoostConfig.cmake). bitcoinkernel's Bitcoin
# Core CMake build uses find_package(Boost) in config mode, so the CMake package
# dir must be discoverable, not just the headers.
BOOST_CMAKE_DIR="${BOOST_CMAKE_DIR:-$(dirname "$(find /usr/lib -name BoostConfig.cmake 2>/dev/null | head -1)")}"
if [ -z "$BOOST_CMAKE_DIR" ] || [ ! -f "$BOOST_CMAKE_DIR/BoostConfig.cmake" ]; then
    echo "Error: BoostConfig.cmake not found. Install Boost dev headers (e.g. libboost-all-dev)."
    exit 1
fi

# Common cross-compilation environment (arch-independent)
export ANDROID_NDK_ROOT="$NDK"
# libbitcoinkernel-sys (android_support) reads ANDROID_NDK_HOME to locate the
# NDK CMake toolchain file.
export ANDROID_NDK_HOME="$NDK"
export PATH="${TOOLCHAIN}/bin:$PATH"
# Make Boost's CMake config discoverable to the NDK-toolchain'd Bitcoin Core build.
export Boost_DIR="$BOOST_CMAKE_DIR"
export CMAKE_PREFIX_PATH="$(dirname "$BOOST_CMAKE_DIR"):${CMAKE_PREFIX_PATH:-}"

# The Android NDK toolchain restricts find_package to the sysroot; symlink the
# host Boost headers in so the compiler can reach them. `-n` so re-runs don't
# recurse into an existing symlink-to-dir.
ln -sfn /usr/include/boost "${TOOLCHAIN}/sysroot/usr/include/boost"

rustup target add aarch64-linux-android x86_64-linux-android >/dev/null

cd "$FFI_DIR"

# Build the FFI cdylib for one Android target and copy it into the app's jniLibs.
#   $1 = rust target triple   (e.g. aarch64-linux-android)
#   $2 = NDK clang prefix      (e.g. aarch64-linux-android — the compiler basename minus API+"-clang")
#   $3 = jniLibs ABI dir name  (e.g. arm64-v8a)
#   $4 = extra RUSTFLAGS       (e.g. the 16 KB page-size flag; empty for x86_64)
build_target() {
    local target="$1" clang_prefix="$2" abi_dir="$3" extra_rustflags="$4"
    local cc="${clang_prefix}${API_LEVEL}-clang"
    local cxx="${clang_prefix}${API_LEVEL}-clang++"
    # cargo env vars want the target triple upper-cased with '-' -> '_'.
    local tenv
    tenv="$(echo "$target" | tr '[:lower:]-' '[:upper:]_')"

    # CRITICAL: bitcoinkernel's build needs `-L native=<sysroot libc dir>` so the
    # cdylib dynamically links Bionic libc (otherwise getauxval/init_have_lse_atomics
    # SIGSEGVs at startup on arm64). That env var OVERRIDES `.cargo/config.toml`
    # target rustflags, so any config-level flag (e.g. the arm64 16 KB page-size
    # flag for Google Play / Android 15+) MUST be merged in here too via
    # $extra_rustflags, or it silently regresses.
    local ndk_lib_dir="${TOOLCHAIN}/sysroot/usr/lib/${target}/${API_LEVEL}"

    export CC_${tenv}="$cc"
    export CXX_${tenv}="$cxx"
    export AR_${tenv}="${TOOLCHAIN}/bin/llvm-ar"
    export CC="$cc"
    export CARGO_TARGET_${tenv}_LINKER="$cc"
    export CARGO_TARGET_${tenv}_RUSTFLAGS="-L native=${ndk_lib_dir} ${extra_rustflags}"

    echo "==> Building floresta-mandacaru-ffi (bitcoinkernel) for ${target} (API ${API_LEVEL}, ${PROFILE})..."
    cargo build --lib --profile "$PROFILE" --target "$target"

    local so_path="target/${target}/${PROFILE_DIR}/libflorestad_ffi.so"
    local jnilibs_dir="${MANDACARU_DIR}/app/src/main/jniLibs/${abi_dir}"
    echo "==> Copying native library to mandacaru (${abi_dir})..."
    mkdir -p "$jnilibs_dir"
    # cdylib_name = florestad_ffi in uniffi.toml, so the generated bindings load
    # `libflorestad_ffi.so` directly — no rename.
    rm -f "${jnilibs_dir}/libuniffi_floresta.so"
    cp "$so_path" "${jnilibs_dir}/libflorestad_ffi.so"
}

# arm64-v8a: real devices. Needs the 16 KB page-size flag (Android 15+).
build_target aarch64-linux-android aarch64-linux-android arm64-v8a \
    "-C link-arg=-Wl,-z,max-page-size=16384"

# x86_64: emulators / CI. 16 KB flag is arm64-only (x86_64 uses 4 KB pages), so omit it.
build_target x86_64-linux-android x86_64-linux-android x86_64 ""

# Kotlin bindings are architecture-independent; generate once from either .so.
echo "==> Generating Kotlin bindings (uniffi 0.31 --library)..."
cargo run --bin uniffi-bindgen generate \
    --library "target/aarch64-linux-android/${PROFILE_DIR}/libflorestad_ffi.so" \
    --language kotlin --out-dir generated/kotlin/ --no-format

echo "==> Copying Kotlin bindings to mandacaru..."
KOTLIN_DIR="${MANDACARU_DIR}/app/src/main/java/com/florestad"
mkdir -p "$KOTLIN_DIR"
# package_name = com.florestad in uniffi.toml, so no sed rewrite is needed.
cp generated/kotlin/com/florestad/floresta.kt "${KOTLIN_DIR}/florestad.kt"

echo "==> Verifying alignment + libc linkage..."
READELF="$(command -v llvm-readelf || echo "${TOOLCHAIN}/bin/llvm-readelf")"
for abi_dir in arm64-v8a x86_64; do
    so="${MANDACARU_DIR}/app/src/main/jniLibs/${abi_dir}/libflorestad_ffi.so"
    echo "  ${abi_dir}:"
    "$READELF" -lW "$so" | awk '/LOAD/{print "    LOAD Align", $NF}'
    "$READELF" -d "$so" | grep -E "NEEDED.*lib(c|dl|m)\.so" | sed 's/^/    /'
done

echo "==> Done!"
for abi_dir in arm64-v8a x86_64; do
    so="${MANDACARU_DIR}/app/src/main/jniLibs/${abi_dir}/libflorestad_ffi.so"
    echo "    Library (${abi_dir}): ${so} ($(du -h "$so" | cut -f1))"
done
echo "    Bindings: ${KOTLIN_DIR}/florestad.kt"
