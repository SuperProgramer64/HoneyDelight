from pathlib import Path
import subprocess, tempfile, unittest, zipfile, struct
ROOT=Path(__file__).resolve().parents[1]

class CrcPatchTest(unittest.TestCase):
 def test_valid_patch_and_ambiguous_input_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   p=Path(d);classes=p/'classes';classes.mkdir()
   subprocess.run(['javac','--release','11','-d',str(classes),str(ROOT/'src/io/honeydelight/tools/CrcPatch.java')],check=True)
   entries=['classes.dex','classes2.dex','classes3.dex']
   for kind in ['stock','modified']:
    with zipfile.ZipFile(p/(kind+'.zip'),'w') as z:
     for name in entries:z.writestr(name,(kind+'-'+name).encode())
   with zipfile.ZipFile(p/'stock.zip') as a,zipfile.ZipFile(p/'modified.zip') as b:
    old=[struct.pack('<I',b.getinfo(n).CRC) for n in entries];new=[struct.pack('<I',a.getinfo(n).CRC) for n in entries]
   source=b'HEADER'+b'guard'.join(old)+b'TAIL';expected=b'HEADER'+b'guard'.join(new)+b'TAIL'
   (p/'artifact').write_bytes(source)
   cmd=['java','-cp',str(classes),'io.honeydelight.tools.CrcPatch',str(p/'stock.zip'),str(p/'modified.zip'),str(p/'artifact'),str(p/'result')]
   result=subprocess.run(cmd,capture_output=True)
   self.assertEqual(result.returncode,0,result.stderr)
   self.assertEqual((p/'result').read_bytes(),expected)
   (p/'artifact').write_bytes(source+old[0]);(p/'result').write_bytes(b'unchanged')
   result=subprocess.run(cmd,capture_output=True)
   self.assertNotEqual(result.returncode,0)
   self.assertEqual((p/'result').read_bytes(),b'unchanged')

if __name__=='__main__':unittest.main()
