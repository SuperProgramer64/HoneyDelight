import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'apply-hooks.py'

class HookResolutionTest(unittest.TestCase):
    def test_descriptor_resolves_renamed_file_and_rejects_second_apply(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'scripts').mkdir()
            (root / 'patches').mkdir()
            (root / 'decoded/smali').mkdir(parents=True)
            shutil.copy2(SCRIPT, root / 'scripts/apply-hooks.py')
            (root / 'patches/classes.json').write_text(json.dumps({'smali/Case.smali': 'Ldemo/c;'}))
            target = root / 'decoded/smali/case-renamed.smali'
            target.write_text('.class Ldemo/c;\n# original\n')
            (root / 'patches/samsung-keyboard-5.9.30.97.patch').write_text(
                '--- a/smali/Case.smali\n+++ b/smali/Case.smali\n@@ -1,2 +1,3 @@\n .class Ldemo/c;\n # original\n+# hook\n')
            command = ['python3', str(root / 'scripts/apply-hooks.py'), str(root / 'decoded')]
            checked = subprocess.run(command + ['--check'], capture_output=True, text=True)
            self.assertEqual(checked.returncode, 0, checked.stderr)
            self.assertNotIn('# hook', target.read_text())
            applied = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(applied.returncode, 0, applied.stderr)
            self.assertEqual(target.read_text(), '.class Ldemo/c;\n# original\n# hook\n')
            repeated = subprocess.run(command, capture_output=True, text=True)
            self.assertNotEqual(repeated.returncode, 0)
            self.assertEqual(target.read_text().count('# hook'), 1)

if __name__ == '__main__':
    unittest.main()
