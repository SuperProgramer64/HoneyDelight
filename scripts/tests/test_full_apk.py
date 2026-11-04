import hashlib
import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location('full_apk', Path(__file__).resolve().parents[1] / 'build-full-apk.py')

class FullApkTests(unittest.TestCase):
    def test_only_base_payload_is_added_and_stock_resources_are_preserved(self):
        builder = importlib.util.module_from_spec(spec); spec.loader.exec_module(builder)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); stock = root / 'stock.apk'; payload = root / 'payload'; payload.mkdir()
            with zipfile.ZipFile(stock, 'w') as z:
                for name, data in {'AndroidManifest.xml': b'fixture manifest', 'resources.arsc': b'fixture table',
                                   'res/CaseSensitive.xml': b'fixture resource', 'classes.dex': b'old dex',
                                   'META-INF/CERT.RSA': b'old signature', 'META-INF/kotlin.version': b'fixture metadata'}.items(): z.writestr(name, data)
            with zipfile.ZipFile(payload / 'mod.zip', 'w') as z:
                for name in ('classes.dex', 'classes2.dex', 'classes3.dex'): z.writestr(name, b'dex\n035\0fixture')
            pins = {}
            for name in ('libintegrated_shared_object.so', 'es_us.dict', 'en_us.dict'):
                data = ('fixture base ' + name).encode(); (payload / name).write_bytes(data)
                pins[name] = hashlib.sha256(data).hexdigest()
            (payload / 'UserHistory.dict').write_bytes(b'PRIVATE_HISTORY_NOT_FOR_PUBLICATION')
            classes3 = root / 'classes3.dex'; classes3.write_bytes(b'dex\n035\0merged bridge')
            license_file = root / 'LICENSE'; license_file.write_text('fixture original-code license')
            notices = root / 'THIRD_PARTY_NOTICES.md'; notices.write_text('fixture notice')
            output = root / 'full.apk'
            builder.build(stock, payload, classes3, output, hashlib.sha256(stock.read_bytes()).hexdigest(), pins, license_file, notices)
            self.assertTrue(output.is_file(), "The full APK must be generated")
            with zipfile.ZipFile(output) as z:
                self.assertEqual(z.read('res/CaseSensitive.xml'), b'fixture resource')
                self.assertEqual(z.read('AndroidManifest.xml'), b'fixture manifest')
                self.assertEqual(z.read('classes3.dex'), classes3.read_bytes())
                self.assertNotIn('META-INF/CERT.RSA', z.namelist())
                self.assertIn('META-INF/kotlin.version', z.namelist())
                self.assertIn('assets/honeydelight/base.properties', z.namelist())
                self.assertFalse(any('UserHistory' in name for name in z.namelist()))
                self.assertEqual(z.getinfo('lib/arm64-v8a/libintegrated_shared_object.so').compress_type, zipfile.ZIP_STORED)
                self.assertFalse(any(b'PRIVATE_HISTORY' in z.read(name) for name in z.namelist()))
            (payload / 'en_us.dict').write_bytes(b'PRIVATE_HISTORY')
            with self.assertRaises(ValueError): builder.build(stock, payload, classes3, root / 'bad.apk', hashlib.sha256(stock.read_bytes()).hexdigest(), pins, license_file, notices)
            with self.assertRaises(ValueError): builder.build(stock, payload, classes3, root / 'bad.apk', '0' * 64, pins, license_file, notices)

if __name__ == '__main__': unittest.main()
