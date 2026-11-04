#!/usr/bin/env python3
"""Build a device-specific local module from an explicit, non-personal payload allowlist."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile

FILES = ('mod.zip', 'libintegrated_shared_object.so', 'es_us.dict', 'en_us.dict')

def build(root, payload, fingerprint, output):
    if not re.fullmatch(r'[A-Za-z0-9_./:,+-]+', fingerprint):
        raise ValueError('Invalid Android fingerprint')
    pins = json.loads((root / 'patches/base-components.json').read_text())
    data = {}
    for name in FILES:
        file = payload / name
        if not file.is_file() or file.is_symlink():
            raise ValueError('Missing regular payload file: ' + name)
        data[name] = file.read_bytes()
        if name in pins and hashlib.sha256(data[name]).hexdigest() != pins[name]:
            raise ValueError('Unexpected base component; personal models are not accepted: ' + name)
    apk = json.loads((root / 'patches/compatibility.json').read_text())['samsung_apk_sha256']
    properties = f'apk_sha256={apk}\nfingerprint={fingerprint}\nabi=arm64-v8a\nsdk=36\n'
    checksums = ''.join(hashlib.sha256(data[n]).hexdigest() + '  ' + n + '\n' for n in FILES)
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        module_files = ('module.prop', 'customize.sh', 'service.sh', 'control.sh', 'uninstall.sh',
                        'lib/compat.sh', 'lib/compile.sh', 'tools.dex', 'skip_mount', 'META-INF/com/google/android/update-binary',
                        'META-INF/com/google/android/updater-script')
        for name in module_files:
            source = root / 'module' / name
            if not source.is_file() or source.is_symlink():
                raise ValueError('Missing module source: ' + name)
            archive.writestr(name, source.read_bytes())
        archive.writestr('compat.prop', properties)
        archive.writestr('payload/SHA256SUMS', checksums)
        for name in FILES:
            archive.writestr('payload/' + name, data[name])
    return output

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--payload-dir', type=Path, required=True)
    parser.add_argument('--fingerprint-file', type=Path, required=True)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--output', type=Path)
    group.add_argument('--output-dir', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    fingerprint = args.fingerprint_file.read_text().strip()
    apk = json.loads((root / 'patches/compatibility.json').read_text())['samsung_apk_sha256']
    compatibility_id = hashlib.sha256((fingerprint + '\n' + apk).encode()).hexdigest()[:12]
    output = args.output or args.output_dir / ('HoneyDelight-0.1.0-alpha-SM-S936B-' + compatibility_id + '.zip')
    built = build(root, args.payload_dir, fingerprint, output)
    print('Local device-specific module:', built)
