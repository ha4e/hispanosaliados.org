#!/usr/bin/env bash
# Install Babashka on Netlify (Linux) and run the site build.
# Netlify's default image has Node and make but not bb.
set -e
BB_VERSION="${BABASHKA_VERSION:-1.12.214}"
BB_ARCH="linux-amd64"
BB_URL="https://github.com/babashka/babashka/releases/download/v${BB_VERSION}/babashka-${BB_VERSION}-${BB_ARCH}.tar.gz"
echo "Installing Babashka ${BB_VERSION}..."
curl -sL "$BB_URL" -o bb.tar.gz
tar xzf bb.tar.gz
BB_DIR="$(find . -maxdepth 2 -name bb -type f | head -1)"
BB_DIR="$(dirname "$BB_DIR")"
chmod +x "$BB_DIR/bb"
export PATH="$PWD/$BB_DIR:$PATH"
bb --version
echo "Running make build..."
make build
