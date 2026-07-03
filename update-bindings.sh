#!/usr/bin/env bash
set -euo pipefail

# Update UniFFI bindings for Mandacaru Android app
#
# This script:
# 1. Cross-compiles floresta-mandacaru-ffi (with bitcoinkernel) for Android ARM64
# 2. Generates Kotlin bindings from the compiled library (uniffi 0.31 --library)
# 3. Copies the .so library and Kotlin bindings into the mandacaru app
#
# Prerequisites:
# - Android NDK 27+ (ANDROID_NDK_ROOT or ~/Android/Sdk/ndk/<version>)
# - Boost dev headers + CMake config (libboost-all-dev on Debian/Ubuntu), needed
#   by bitcoinkernel's bundled Bitcoin Core build.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FFI_DIR="$(cd "$SCRIPT_DIR/../floresta-mandacaru-ffi" && pwd)"
MANDACARU_DIR="$SCRIPT_DIR"

# Android NDK setup
NDK_VERSION="${NDK_VERSION:-27.0.12077973}"
ANDROID_SDK="${ANDROID_HOME:-${HOME}/Android/Sdk}"
NDK="${ANDROID_NDK_ROOT:-${ANDROID_SDK}/ndk/${NDK_VERSION}}"
TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/linux-x86_64"
TARGET="aarch64-linux-android"
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

# Export cross-compilation environment
export CC_aarch64_linux_android="${TOOLCHAIN}/bin/aarch64-linux-android${API_LEVEL}-clang"
export CXX_aarch64_linux_android="${TOOLCHAIN}/bin/aarch64-linux-android${API_LEVEL}-clang++"
export AR_aarch64_linux_android="${TOOLCHAIN}/bin/llvm-ar"
export CC="aarch64-linux-android${API_LEVEL}-clang"
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

# CRITICAL: bitcoinkernel's build needs `-L native=<sysroot libc dir>` so the
# cdylib dynamically links Bionic libc (otherwise getauxval/init_have_lse_atomics
# SIGSEGVs at startup). That env var OVERRIDES `.cargo/config.toml` target
# rustflags, so the 16 KB page-size flag (Google Play / Android 15+) MUST be
# merged in here too, or it silently regresses.
NDK_LIB_DIR="${TOOLCHAIN}/sysroot/usr/lib/${TARGET}/${API_LEVEL}"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="aarch64-linux-android${API_LEVEL}-clang"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_RUSTFLAGS="-L native=${NDK_LIB_DIR} -C link-arg=-Wl,-z,max-page-size=16384"

echo "==> Building floresta-mandacaru-ffi (bitcoinkernel) for ${TARGET} (API ${API_LEVEL}, ${PROFILE})..."
cd "$FFI_DIR"
cargo build --lib --profile "$PROFILE" --target "$TARGET"

SO_PATH="target/${TARGET}/${PROFILE_DIR}/libflorestad_ffi.so"

echo "==> Generating Kotlin bindings (uniffi 0.31 --library)..."
cargo run --bin uniffi-bindgen generate --library "$SO_PATH" --language kotlin --out-dir generated/kotlin/ --no-format

echo "==> Copying native library to mandacaru..."
JNILIBS_DIR="${MANDACARU_DIR}/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNILIBS_DIR"
# cdylib_name = florestad_ffi in uniffi.toml, so the generated bindings load
# `libflorestad_ffi.so` directly — no rename.
rm -f "${JNILIBS_DIR}/libuniffi_floresta.so"
cp "$SO_PATH" "${JNILIBS_DIR}/libflorestad_ffi.so"

echo "==> Copying Kotlin bindings to mandacaru..."
KOTLIN_DIR="${MANDACARU_DIR}/app/src/main/java/com/florestad"
mkdir -p "$KOTLIN_DIR"
# package_name = com.florestad in uniffi.toml, so no sed rewrite is needed.
cp generated/kotlin/com/florestad/floresta.kt "${KOTLIN_DIR}/florestad.kt"

echo "==> Verifying 16 KB alignment + libc linkage..."
READELF="$(command -v llvm-readelf || echo "${TOOLCHAIN}/bin/llvm-readelf")"
"$READELF" -lW "${JNILIBS_DIR}/libflorestad_ffi.so" | awk '/LOAD/{print "    LOAD Align", $NF}'
"$READELF" -d "${JNILIBS_DIR}/libflorestad_ffi.so" | grep -E "NEEDED.*lib(c|dl|m)\.so" | sed 's/^/    /'

echo "==> Done!"
echo "    Library: ${JNILIBS_DIR}/libflorestad_ffi.so ($(du -h "${JNILIBS_DIR}/libflorestad_ffi.so" | cut -f1))"
echo "    Bindings: ${KOTLIN_DIR}/florestad.kt"
