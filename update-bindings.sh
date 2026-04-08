#!/usr/bin/env bash
set -euo pipefail

# Update UniFFI bindings for Mandacaru Android app
#
# This script:
# 1. Cross-compiles floresta-mandacaru-ffi for Android ARM64
# 2. Generates Kotlin bindings from the UDL file
# 3. Copies the .so library and Kotlin bindings into the mandacaru app

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

# Export cross-compilation environment
export CC_aarch64_linux_android="${TOOLCHAIN}/bin/aarch64-linux-android${API_LEVEL}-clang"
export CXX_aarch64_linux_android="${TOOLCHAIN}/bin/aarch64-linux-android${API_LEVEL}-clang++"
export AR_aarch64_linux_android="${TOOLCHAIN}/bin/llvm-ar"
export ANDROID_NDK_ROOT="$NDK"

echo "==> Building floresta-mandacaru-ffi for ${TARGET} (API ${API_LEVEL})..."
cd "$FFI_DIR"
cargo build --lib --release --target "$TARGET"

echo "==> Generating Kotlin bindings..."
cargo run --bin uniffi-bindgen generate --language kotlin src/floresta.udl --out-dir generated/kotlin/

echo "==> Copying native library to mandacaru..."
JNILIBS_DIR="${MANDACARU_DIR}/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNILIBS_DIR"
cp "target/${TARGET}/release/libflorestad_ffi.so" "${JNILIBS_DIR}/libuniffi_floresta.so"

echo "==> Copying Kotlin bindings to mandacaru..."
KOTLIN_DIR="${MANDACARU_DIR}/app/src/main/java/com/florestad"
mkdir -p "$KOTLIN_DIR"
sed 's/^package uniffi\.floresta;/package com.florestad;/' \
    generated/kotlin/uniffi/floresta/floresta.kt \
    > "${KOTLIN_DIR}/florestad.kt"

echo "==> Done!"
echo "    Library: ${JNILIBS_DIR}/libuniffi_floresta.so ($(du -h "${JNILIBS_DIR}/libuniffi_floresta.so" | cut -f1))"
echo "    Bindings: ${KOTLIN_DIR}/florestad.kt"
