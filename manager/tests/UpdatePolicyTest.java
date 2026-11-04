import io.honeydelight.manager.UpdatePolicy;
public class UpdatePolicyTest {
 public static void main(String[] args)throws Exception{
  String a=UpdatePolicy.compatibilityId("device/A","abc");String b=UpdatePolicy.compatibilityId("device/B","abc");
  if(!a.equals("d277caf40e01"))throw new AssertionError("Compatibility ID differs from release packager");
  if(a.equals(b))throw new AssertionError("Build mismatch not separated");
  if(!UpdatePolicy.compatible("HoneyDelight-0.1-alpha-SM-S936B-"+a+".zip",a))throw new AssertionError("Compatible module rejected");
  for(String n:new String[]{"../HoneyDelight-"+a+".zip","HoneyDelight-source.zip","HoneyDelight-"+b+".zip","HoneyDelight-"+a+".apk"})if(UpdatePolicy.compatible(n,a))throw new AssertionError("Wrong asset accepted: "+n);
  if(!UpdatePolicy.validDigest("sha256:"+"a".repeat(64))||UpdatePolicy.validDigest("sha256:abc")||UpdatePolicy.validDigest(null))throw new AssertionError("Invalid checksum accepted");
  for(String u:new String[]{"http://github.com/a","https://evil.test/","https://github.com/other/repo/a","https://user@github.com/SuperProgramer64/HoneyDelight/releases/download/a/b"})if(UpdatePolicy.trustedUrl(u))throw new AssertionError("Untrusted download URL accepted");
  if(!UpdatePolicy.trustedUrl("https://github.com/SuperProgramer64/HoneyDelight/releases/download/v1/module.zip"))throw new AssertionError("Release URL rejected");
  System.out.println("PASS compatibility variants, safe file names, checksums and GitHub-only download URLs");
 }
}
