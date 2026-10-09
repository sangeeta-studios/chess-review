#!/usr/bin/env bash
# Builds the Stockfish UCI engine for Android with the NDK and installs it into
# app/src/main/jniLibs/<abi>/libstockfish.so (packaged as a "native library" so it
# is extracted to nativeLibraryDir, the only executable location on Android 10+).
set -euo pipefail

SF_TAG="${SF_TAG:-sf_17.1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${WORK:-$ROOT/build/stockfish}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
NDK="${ANDROID_NDK_HOME:-$(ls -d "$SDK"/ndk/* | sort -V | tail -1)}"
HOST_TAG="$(uname -s | tr '[:upper:]' '[:lower:]')-x86_64"
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG/bin"
export PATH="$TOOLCHAIN:$PATH"
unset CPPFLAGS CXXFLAGS LDFLAGS

if [ ! -d "$WORK/src" ]; then
  git clone --depth 1 --branch "$SF_TAG" "${SF_REPO:-https://github.com/sangeeta-studios/Stockfish.git}" "$WORK"
fi

build() {
  local abi="$1" arch="$2"
  echo "==> Building $abi ($arch)"
  make -C "$WORK/src" clean >/dev/null
  # 16 KB page alignment is required by Google Play for native code.
  make -C "$WORK/src" -j"$(sysctl -n hw.ncpu 2>/dev/null || nproc)" build \
    KERNEL=Linux OS=Android ARCH="$arch" COMP=ndk EXTRALDFLAGS="-Wl,-z,max-page-size=16384"
  mkdir -p "$ROOT/app/src/main/jniLibs/$abi"
  llvm-strip "$WORK/src/stockfish" -o "$ROOT/app/src/main/jniLibs/$abi/libstockfish.so"
}

build arm64-v8a armv8
build x86_64 x86-64-sse41-popcnt
echo "Done."
