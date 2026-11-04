#!/usr/bin/env python3
"""Apply integration hunks by class descriptor, independent of apktool's filename casing."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('decoded', type=Path)
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
target = args.decoded.resolve()
index = json.loads((root / 'patches/classes.json').read_text())
lookup = {}
for folder in ('smali', 'smali_classes2'):
    for file in (target / folder).rglob('*.smali'):
        match = re.search(r'^\.class[^\n]*?\s(L[^\s;]+;)', file.read_text(), re.M)
        if match:
            lookup[match[1]] = file.relative_to(target).as_posix()
missing = set(index.values()) - lookup.keys()
if missing:
    raise SystemExit('Missing target classes: ' + ', '.join(sorted(missing)))
lines = []
for line in (root / 'patches/samsung-keyboard-5.9.30.97.patch').read_text().splitlines(keepends=True):
    if line.startswith(('--- a/', '+++ b/')):
        prefix = line[:6]
        key = line[6:].rstrip('\n')
        line = prefix + lookup[index[key]] + '\n'
    lines.append(line)
patch = ''.join(lines)
env = dict(os.environ, GIT_CEILING_DIRECTORIES=str(target.parent))
subprocess.run(['git', 'apply', '--check', '-'], input=patch, text=True, cwd=target, env=env, check=True)
if not args.check:
    subprocess.run(['git', 'apply', '-'], input=patch, text=True, cwd=target, env=env, check=True)
print(('Validated' if args.check else 'Applied') + ' hooks for ' + str(len(index)) + ' classes.')
