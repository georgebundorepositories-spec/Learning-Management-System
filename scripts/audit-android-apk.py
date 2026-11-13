#!/usr/bin/env python3
"""Lightweight build gate for ZimSchool APKs.

This deliberately does not replace Android security testing or production
signature verification. It prevents accidental packaging of known privileged
credential formats and checks that the APK archive is structurally readable.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from zipfile import BadZipFile, ZipFile

MAX_APK_BYTES = 25 * 1024 * 1024
MAX_EXPANDED_BYTES = 200 * 1024 * 1024
# High-confidence credential formats only. Do not print secret values.
FORBIDDEN = (
    b"sb_secret_",
    b"-----BEGIN PRIVATE KEY-----",
    b"-----BEGIN RSA PRIVATE KEY-----",
    b"SUPABASE_SERVICE_ROLE_KEY=",
)
# The public key is intentional and must not be reported as sensitive.
EXPECTED_PUBLIC = b"sb_publishable_"


def check_apk(path: Path) -> None:
    if not path.is_file():
        raise ValueError("APK file is missing")
    if path.stat().st_size > MAX_APK_BYTES:
        raise ValueError("APK exceeds the 25 MiB test release size budget")
    with ZipFile(path) as archive:
        members = archive.infolist()
        names = {member.filename for member in members}
        if not {"AndroidManifest.xml", "classes.dex"} <= names:
            raise ValueError("APK is missing its Android manifest or primary DEX")
        if sum(member.file_size for member in members) > MAX_EXPANDED_BYTES:
            raise ValueError("APK expansion exceeds the audit size limit")
        bad_name = archive.testzip()
        if bad_name:
            raise ValueError("APK archive CRC verification failed")
        found_publishable = False
        for member in members:
            if member.is_dir():
                continue
            # Scan compiled bytecode, manifest and shipped text/assets.
            if not (
                member.filename.endswith((".dex", ".xml", ".json", ".properties"))
                or member.filename.startswith("assets/")
            ):
                continue
            payload = archive.read(member)
            if any(pattern in payload for pattern in FORBIDDEN):
                raise ValueError("Privileged credential format detected in APK")
            if EXPECTED_PUBLIC in payload:
                found_publishable = True
        if not found_publishable:
            raise ValueError("Expected Supabase publishable key is absent")
    print(f"PASS: APK structure, ZIP integrity and privileged-key format scan: {path.name}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    try:
        check_apk(args.apk)
    except (BadZipFile, ValueError, OSError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(1) from exc
