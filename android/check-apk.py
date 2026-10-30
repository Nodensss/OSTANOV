#!/usr/bin/env python3
"""Verify the offline APK, merged manifest and unchanged shared asset after a debug build."""
from pathlib import Path
import hashlib
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parent
apk = root / "app/build/outputs/apk/debug/app-debug.apk"
merged = list((root / "app/build/intermediates/merged_manifests/debug").rglob("AndroidManifest.xml"))
assert merged, "Build the debug APK first: merged manifest missing"
for manifest in [root / "app/src/main/AndroidManifest.xml", *merged]:
    permissions = [node.get("{http://schemas.android.com/apk/res/android}name") for node in ET.parse(manifest).getroot() if node.tag.startswith("uses-permission")]
    assert "android.permission.INTERNET" not in permissions, f"INTERNET permission in {manifest}"
assert apk.is_file(), "Debug APK missing"
size = apk.stat().st_size
assert size <= 25_000_000, f"APK exceeds 25 MB: {size} bytes"
with zipfile.ZipFile(apk) as archive:
    asset = archive.read("assets/stop-algorithm.json")
assert asset == (root.parent / "data/stop-algorithm.json").read_bytes(), "Shared JSON asset differs"
print(f"OK: APK {size} bytes ({size / 1_000_000:.2f} MB), no INTERNET in source/merged manifests")
print(f"OK: packaged JSON matches source, SHA256 {hashlib.sha256(asset).hexdigest()}")
