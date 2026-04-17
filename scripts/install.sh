#!/bin/bash
set -e

# mckli installation script
# This script downloads the latest JVM distribution of mckli and installs it.

REPO="asubb/mckli" # Adjust if the repo name is different
INSTALL_DIR="/usr/local/bin"
APP_NAME="mckli"

# Check for Java 17+
if ! command -v java >/dev/null 2>&1; then
  echo "Error: Java is not installed. Please install Java 17 or higher."
  exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
  echo "Error: Java version 17 or higher is required. Current version: $JAVA_VERSION"
  exit 1
fi

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Linux*)     OS_NAME=linux;;
    Darwin*)    OS_NAME=macos;;
    *)          echo "Unsupported OS: ${OS}"; exit 1;;
esac

echo "Installing ${APP_NAME} on ${OS_NAME}..."

# Get latest release tag
LATEST_RELEASE_URL="https://github.com/${REPO}/releases/latest"
TAG=$(curl -sL -o /dev/null -w %{url_effective} ${LATEST_RELEASE_URL} | rev | cut -d'/' -f1 | rev)

if [ -z "$TAG" ] || [ "$TAG" == "latest" ]; then
  echo "Error: Could not determine the latest release tag."
  exit 1
fi

echo "Latest release: ${TAG}"

# Download URL for the distribution
VERSION=${TAG#v}
EXTENSION="tar"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${TAG}/mckli-${VERSION}.tar"

echo "Downloading from ${DOWNLOAD_URL}..."
TEMP_DIR=$(mktemp -d)
curl -sL "${DOWNLOAD_URL}" -o "${TEMP_DIR}/mckli.${EXTENSION}"

if [ ! -s "${TEMP_DIR}/mckli.${EXTENSION}" ]; then
  echo "Error: Downloaded file is empty. Check if the release contains mckli-${VERSION}.${EXTENSION}"
  exit 1
fi

# Extract
tar -xf "${TEMP_DIR}/mckli.tar" -C "${TEMP_DIR}"
EXTRACTED_DIR="${TEMP_DIR}/mckli-${VERSION}"

# Install
# The distribution has bin/mckli and lib/*.jar
LIB_DEST="/usr/local/lib/${APP_NAME}"
sudo mkdir -p "${LIB_DEST}"
sudo cp -r "${EXTRACTED_DIR}/lib" "${LIB_DEST}/"
sudo cp -r "${EXTRACTED_DIR}/bin" "${LIB_DEST}/"

# Ensure the binary is executable
sudo chmod +x "${LIB_DEST}/bin/${APP_NAME}"

# Symlink
sudo ln -sf "${LIB_DEST}/bin/${APP_NAME}" "${INSTALL_DIR}/${APP_NAME}"

echo "Cleaning up..."
rm -rf "${TEMP_DIR}"

echo "Installation complete! You can now run '${APP_NAME}'."
