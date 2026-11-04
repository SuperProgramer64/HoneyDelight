#!/usr/bin/env python3
"""Match dex2oat input CRC entries to the unchanged, signed Samsung base APK."""
import argparse
from pathlib import Path
import struct
import zipfile

def patched(original_apk, input_zip, data):
    with zipfile.ZipFile(original_apk) as stock, zipfile.ZipFile(input_zip) as modified:
        for name in ('classes.dex', 'classes2.dex', 'classes3.dex'):
            source = modified.getinfo(name).CRC
            target = stock.getinfo(name).CRC
            if source == target:
                continue
            needle = struct.pack('<I', source)
            if data.count(needle) != 1:
                raise ValueError('Unexpected number of checksum entries for ' + name)
            data = data.replace(needle, struct.pack('<I', target))
    return data

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('original_apk', type=Path)
    parser.add_argument('input_zip', type=Path)
    parser.add_argument('artifact', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    data = patched(args.original_apk, args.input_zip, args.artifact.read_bytes())
    args.output.write_bytes(data)
