#!/usr/bin/env python3
"""Repackage the exact Samsung base with patched DEX and pinned base models; signing is separate."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

BASE_FILES = ('libintegrated_shared_object.so', 'es_us.dict', 'en_us.dict')
DEX_FILES = ('classes.dex', 'classes2.dex', 'classes3.dex')

def build(stock, payload, classes3, output, expected, pins, license_file, notices):
    if hashlib.sha256(stock.read_bytes()).hexdigest() != expected:
        raise ValueError('Unsupported Samsung APK; base checksum differs')
    models = {}
    for name in BASE_FILES:
        source = payload / name
        if not source.is_file() or source.is_symlink():
            raise ValueError('Missing regular base component: ' + name)
        models[name] = source.read_bytes()
        if hashlib.sha256(models[name]).hexdigest() != pins[name]:
            raise ValueError('Unexpected or personal base component: ' + name)
    with zipfile.ZipFile(payload / 'mod.zip') as patched:
        dex = {name: patched.read(name) for name in DEX_FILES}
    dex['classes3.dex'] = classes3.read_bytes()
    if not all(data.startswith(b'dex\n') for data in dex.values()):
        raise ValueError('Invalid patched DEX payload')
    entries = dict(dex)
    entries['lib/arm64-v8a/libintegrated_shared_object.so'] = models['libintegrated_shared_object.so']
    for name in ('es_us.dict', 'en_us.dict'):
        entries['assets/honeydelight/' + name] = models[name]
    entries['assets/honeydelight/base.properties'] = ''.join(name + '=' + pins[name] + '\n' for name in ('es_us.dict', 'en_us.dict')).encode()
    entries['assets/honeydelight/LICENSE'] = license_file.read_bytes()
    entries['assets/honeydelight/THIRD_PARTY_NOTICES.md'] = notices.read_bytes()
    entries['assets/honeydelight/build.json'] = json.dumps({
        'project': 'HoneyDelight', 'version': '0.1.0-alpha', 'variant': 'full-apk-experimental',
        'samsung_base_sha256': expected, 'contains_user_training': False,
        'samsung_signature_preserved': False, 'runtime_validated': False,
    }, indent=2).encode()
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(stock) as original, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        names = original.namelist()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate stock APK entries')
        for item in original.infolist():
            name = item.filename
            if name in entries or name.startswith('assets/honeydelight/'):
                continue
            if re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', name, re.I):
                continue
            archive.writestr(item, original.read(name))
        for name, data in entries.items():
            compression = zipfile.ZIP_STORED if name.startswith('lib/') else zipfile.ZIP_DEFLATED
            archive.writestr(name, data, compress_type=compression)
    return output

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--stock', type=Path, required=True)
    parser.add_argument('--payload-dir', type=Path, required=True)
    parser.add_argument('--classes3', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    expected = json.loads((root / 'patches/compatibility.json').read_text())['samsung_apk_sha256']
    pins = json.loads((root / 'patches/base-components.json').read_text())
    result = build(args.stock, args.payload_dir, args.classes3, args.output, expected, pins, root / 'LICENSE', root / 'THIRD_PARTY_NOTICES.md')
    print('Unsigned experimental full APK:', result)
