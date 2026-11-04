import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT=Path(__file__).resolve().parents[1]/'audit-port-target.py'
spec=importlib.util.spec_from_file_location('port_audit',SCRIPT)
audit=importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class PortAuditTest(unittest.TestCase):
 def test_finds_fourth_dex_without_modifying_target(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d); f=root/'smali_classes4/demo/a.smali';f.parent.mkdir(parents=True)
   before='.class public Ldemo/a;\n.super Ljava/lang/Object;\n.method public value()I\n .locals 1\n const/4 v0, 0x1\n return v0\n.end method\n'
   f.write_text(before)
   patch='--- a/smali/old.smali\n+++ b/smali/old.smali\n@@ -3,5 +3,5 @@\n .method public value()I\n  .locals 1\n- const/4 v0, 0x1\n+ const/4 v0, 0x2\n  return v0\n .end method\n'
   r=audit.inspect(root,{'smali/old.smali':'Ldemo/a;'},patch,[])
   self.assertEqual(r['hooks'][0]['status'],'context_matches')
   self.assertEqual(r['hooks'][0]['dex_directory'],'smali_classes4')
   self.assertFalse(r['runtime_validated'])
   self.assertEqual(f.read_text(),before)

 def test_rejects_missing_class_and_wrong_context_independently(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);f=root/'smali/demo/a.smali';f.parent.mkdir(parents=True)
   f.write_text('.class public Ldemo/a;\n.super Ljava/lang/Object;\n# changed\n')
   patch='--- a/smali/a.smali\n+++ b/smali/a.smali\n@@ -1,3 +1,4 @@\n .class public Ldemo/a;\n .super Ljava/lang/Object;\n # expected\n+# hook\n'
   r=audit.inspect(root,{'smali/a.smali':'Ldemo/a;','smali/missing.smali':'Ldemo/missing;'},patch,[])
   self.assertEqual([h['status'] for h in r['hooks']],['context_differs','class_missing'])

 def test_duplicate_descriptor_is_ambiguous(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d)
   for dex in ('smali','smali_classes2'):
    f=root/dex/'a.smali';f.parent.mkdir(parents=True);f.write_text('.class public Ldemo/a;\n.super Ljava/lang/Object;\n')
   r=audit.inspect(root,{'smali/a.smali':'Ldemo/a;'},'',[])
   self.assertEqual(r['hooks'][0]['status'],'ambiguous_class')

 def test_changed_method_and_field_descriptors_are_not_accepted(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);f=root/'smali/demo/a.smali';f.parent.mkdir(parents=True)
   f.write_text('.class public Ldemo/a;\n.super Ljava/lang/Object;\n.field public points:I\n.method public score()F\n.end method\n')
   r=audit.inspect(root,{},'',['Ldemo/a;->score()D','Ldemo/a;->points:[F','Ldemo/a;->score()F'])
   self.assertEqual([m['status'] for m in r['members']],['missing_member','missing_member','declared'])

 def test_constructor_is_not_inherited_and_external_ancestor_is_unproven(self):
  with tempfile.TemporaryDirectory() as d:
   root=Path(d);f=root/'smali/demo/a.smali';f.parent.mkdir(parents=True)
   f.write_text('.class public Ldemo/a;\n.super Ljava/util/AbstractList;\n')
   r=audit.inspect(root,{},'',['Ldemo/a;->isEmpty()Z','Ldemo/a;-><init>(I)V'])
   self.assertEqual([m['status'] for m in r['members']],['external_ancestor','missing_member'])

if __name__=='__main__':unittest.main()
