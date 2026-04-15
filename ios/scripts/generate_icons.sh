#!/usr/bin/env zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ASSET_DIR="$ROOT_DIR/GasPricesiOS/Assets.xcassets/AppIcon.appiconset"
BASE_ICON="$ASSET_DIR/Icon-Marketing-1024@1x.png"

mkdir -p "$ASSET_DIR"

swift "$ROOT_DIR/scripts/generate_base_icon.swift" "$BASE_ICON"

while read -r pixels file; do
  sips -z "$pixels" "$pixels" "$BASE_ICON" --out "$ASSET_DIR/$file" >/dev/null
done <<'EOF'
40 Icon-iPhone-20@2x.png
60 Icon-iPhone-20@3x.png
58 Icon-iPhone-29@2x.png
87 Icon-iPhone-29@3x.png
80 Icon-iPhone-40@2x.png
120 Icon-iPhone-40@3x.png
120 Icon-iPhone-60@2x.png
180 Icon-iPhone-60@3x.png
20 Icon-iPad-20@1x.png
40 Icon-iPad-20@2x.png
29 Icon-iPad-29@1x.png
58 Icon-iPad-29@2x.png
40 Icon-iPad-40@1x.png
80 Icon-iPad-40@2x.png
76 Icon-iPad-76@1x.png
152 Icon-iPad-76@2x.png
167 Icon-iPad-83.5@2x.png
EOF

ls -1 "$ASSET_DIR"