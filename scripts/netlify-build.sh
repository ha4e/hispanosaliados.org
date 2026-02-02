#!/usr/bin/env bash
# Install Babashka on Netlify (Linux) and run the site build.
# Netlify's default image has Node and make but not bb.
# Install to $HOME/bin (standard user-local binaries) to avoid conflicting with repo's bb/ directory.
# Netlify does not prescribe a location for user-installed binaries; /opt/build-bin is for image tools only.
set -e
BB_VERSION="${BABASHKA_VERSION:-1.12.214}"
BB_ARCH="linux-amd64"
BB_URL="https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-${BB_ARCH}.tar.gz"
BB_TARBALL="$(pwd)/bb.tar.gz"
BB_EXTRACT="$(mktemp -d)"
mkdir -p "$HOME/bin"
echo "Installing Babashka ${BB_VERSION}..."
curl -sL "$BB_URL" -o "$BB_TARBALL"
tar xzf "$BB_TARBALL" -C "$BB_EXTRACT"
BB_BIN="$(find "$BB_EXTRACT" -name bb -type f | head -1)"
cp "$BB_BIN" "$HOME/bin/bb"
chmod +x "$HOME/bin/bb"
rm -rf "$BB_EXTRACT" "$BB_TARBALL"
export PATH="$HOME/bin:$PATH"
bb --version
echo "Running make build..."
make build
