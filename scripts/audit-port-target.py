#!/usr/bin/env python3
"""Read-only symbol/context comparison. A matching symbol is not a runtime compatibility claim."""
import argparse
from collections import Counter, defaultdict, deque
import json
import os
from pathlib import Path
import re
import subprocess


def scan(decoded):
    index = defaultdict(list)
    for folder in sorted(decoded.glob('smali*')):
        if not folder.is_dir() or not re.fullmatch(r'smali(?:_classes\d+)?', folder.name):
            continue
        for file in folder.rglob('*.smali'):
            if file.is_symlink():
                continue
            with file.open(errors='replace') as stream:
                header = stream.read(8192)
            match = re.search(r'^\.class[^\n]*?\s(L[^\s;]+;)', header, re.M)
            if not match:
                continue
            index[match[1]].append({'file': file})
    return index


def class_details(info):
    if 'members' not in info:
        text = info['file'].read_text(errors='replace')
        info['parents'] = re.findall(r'^\.(?:super|implements) (L[^\s;]+;)', text, re.M)
        methods = set(re.findall(r'^\.method[^\n]*?\s([^\s(]+\([^\n]*\)[^\s]+)\s*$', text, re.M))
        fields = set(re.findall(r'^\.field[^\n]*?\s([^\s:]+:[^\s=]+)', text, re.M))
        info['members'] = methods | fields
    return info


def member_status(index, reference):
    owner, member = reference.split('->', 1)
    if owner not in index:
        return 'class_missing'
    queue = deque([owner]); visited = set(); external = False
    while queue:
        cls = queue.popleft()
        if cls in visited:
            continue
        visited.add(cls)
        entries = index.get(cls, [])
        if len(entries) > 1:
            return 'ambiguous_class'
        if not entries:
            # Object declares no instance fields or application-specific methods.
            if cls == 'Ljava/lang/Object;' and member not in {
                    'clone()Ljava/lang/Object;', 'equals(Ljava/lang/Object;)Z', 'finalize()V',
                    'getClass()Ljava/lang/Class;', 'hashCode()I', 'notify()V', 'notifyAll()V',
                    'toString()Ljava/lang/String;', 'wait()V', 'wait(J)V', 'wait(JI)V'}:
                continue
            external = True
            continue
        info = class_details(entries[0])
        if member in info['members']:
            return 'declared' if cls == owner else 'inherited'
        if member.startswith(('<init>(', '<clinit>(')):
            continue
        queue.extend(info['parents'])
    return 'external_ancestor' if external else 'missing_member'


def patch_blocks(patch):
    result = {}
    for part in re.split(r'(?=^--- a/)', patch, flags=re.M):
        match = re.match(r'--- a/([^\n]+)\n\+\+\+ b/([^\n]+)\n', part)
        if match:
            result[match[1]] = part
    return result


def inspect(decoded, specification, patch, references):
    decoded = Path(decoded).resolve()
    index = scan(decoded)
    blocks = patch_blocks(patch)
    hooks = []
    for source, descriptor in specification.items():
        item = {'class': descriptor, 'status': 'class_missing'}
        entries = index.get(descriptor, [])
        if len(entries) > 1:
            item['status'] = 'ambiguous_class'
        elif len(entries) == 1:
            relative = entries[0]['file'].relative_to(decoded).as_posix()
            item['dex_directory'] = relative.split('/')[0]
            if source not in blocks:
                item['status'] = 'patch_missing'
            else:
                block = blocks[source]
                block = re.sub(r'^--- a/[^\n]+', '--- a/' + relative, block, count=1, flags=re.M)
                block = re.sub(r'^\+\+\+ b/[^\n]+', '+++ b/' + relative, block, count=1, flags=re.M)
                env = dict(os.environ, GIT_CEILING_DIRECTORIES=str(decoded.parent))
                result = subprocess.run(['git', 'apply', '--check', '--whitespace=nowarn', '-'],
                                        input=block, text=True, cwd=decoded, env=env, capture_output=True)
                item['status'] = 'context_matches' if result.returncode == 0 else 'context_differs'
        hooks.append(item)
    members = [{'reference': ref, 'status': member_status(index, ref)} for ref in references]
    return {'runtime_validated': False, 'scope': 'static symbols and read-only patch contexts; reflection, access flags and execution are not verified',
            'dex_directories': sorted({i['file'].relative_to(decoded).parts[0] for entries in index.values() for i in entries}),
            'class_count': len(index), 'hooks': hooks, 'members': members}


def bridge_references(decoded):
    refs = set()
    for file in Path(decoded).glob('smali*/**/*.smali'):
        refs.update(re.findall(r'(L(?:com/microsoft/fluency/[^;]+|Qi/c);->[^\s,]+)', file.read_text(errors='replace')))
    return sorted(refs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--decoded', required=True, type=Path)
    parser.add_argument('--bridge-decoded', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if not args.decoded.is_dir() or not any(args.decoded.glob('smali*')):
        parser.error('Expected an APK decoded locally with apktool -r')
    root = Path(__file__).resolve().parents[1]
    refs = bridge_references(args.bridge_decoded) if args.bridge_decoded else []
    result = inspect(args.decoded, json.loads((root/'patches/classes.json').read_text()),
                     (root/'patches/samsung-keyboard-5.9.30.97.patch').read_text(), refs)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'runtime_validated': False, 'hooks': dict(Counter(h['status'] for h in result['hooks'])),
                      'members': dict(Counter(m['status'] for m in result['members']))}))

if __name__ == '__main__':
    main()
