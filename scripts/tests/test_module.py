import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import shutil
import unittest
import zipfile

SCRIPT = Path(__file__).resolve().parents[1] / 'build-module.py'
spec = importlib.util.spec_from_file_location('module_builder', SCRIPT)
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
SOURCE = SCRIPT.parent.parent

class ModuleTests(unittest.TestCase):
    def test_payload_excludes_trained_data(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copytree(SOURCE / 'module', root / 'module')
            (root / 'module/tools.dex').write_bytes(b'fixture installer tool')
            (root / 'module/ai_token').write_text('PRIVATE_TEMPLATE_TOKEN_MUST_NOT_SHIP')
            (root / 'patches').mkdir()
            (root / 'patches/compatibility.json').write_text(json.dumps({'samsung_apk_sha256': 'a' * 64}))
            (root / 'payload').mkdir()
            pins = {}
            for name in builder.FILES:
                data = ('base-' + name).encode()
                (root / 'payload' / name).write_bytes(data)
                if name.endswith(('.dict', '.so')):
                    pins[name] = hashlib.sha256(data).hexdigest()
            (root / 'patches/base-components.json').write_text(json.dumps(pins))
            (root / 'payload/UserHistory.dict').write_text('PRIVATE_HISTORY_MUST_NOT_SHIP')
            (root / 'payload/ai_token').write_text('PRIVATE_TOKEN_MUST_NOT_SHIP')
            output = builder.build(root, root / 'payload', 'demo/device:16/build/user/release-keys', root / 'out.zip')
            with zipfile.ZipFile(output) as archive:
                self.assertNotIn('payload/UserHistory.dict', archive.namelist())
                self.assertNotIn('payload/ai_token', archive.namelist())
                for name in archive.namelist():
                    self.assertNotIn(b'PRIVATE_', archive.read(name))
            (root / 'payload/es_us.dict').write_text('PRIVATE_HISTORY_MUST_NOT_SHIP')
            with self.assertRaises(ValueError):
                builder.build(root, root / 'payload', 'demo', root / 'bad.zip')

    def test_compatibility_rejects_wrong_device_before_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            apk = root / 'base.apk'; apk.write_bytes(b'fixture APK')
            (root / 'payload').mkdir()
            (root / 'payload/file').write_bytes(b'fixture base')
            (root / 'payload/SHA256SUMS').write_text(hashlib.sha256(b'fixture base').hexdigest() + '  file\n')
            (root / 'compat.prop').write_text('apk_sha256=' + hashlib.sha256(b'fixture APK').hexdigest() + '\nfingerprint=test/device\nabi=arm64-v8a\nsdk=36\n')
            command = ['sh', '-c', '. "$1"; HD_MODULE=$2; hd_validate "$3" "$4" arm64-v8a 36', 'sh', str(SOURCE / 'module/lib/compat.sh'), str(root), str(apk)]
            valid = subprocess.run(command + ['test/device'], capture_output=True)
            self.assertEqual(valid.returncode, 0, valid.stderr)
            wrong = subprocess.run(command + ['other/device'], capture_output=True)
            self.assertNotEqual(wrong.returncode, 0)
            apk.write_bytes(b'changed APK')
            wrong = subprocess.run(command + ['test/device'], capture_output=True)
            self.assertNotEqual(wrong.returncode, 0)

if __name__ == '__main__':
    unittest.main()
