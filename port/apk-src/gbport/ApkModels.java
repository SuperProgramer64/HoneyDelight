package gbport;

import java.io.*;
import java.nio.file.*;
import java.security.*;

/** Only used by the experimental full APK, never included in the root bridge payload. */
final class ApkModels {
    interface Source { InputStream open() throws IOException; }

    static synchronized void installIfMissing(File target, Source source, String expected) throws IOException {
        if (Files.isSymbolicLink(target.toPath())) throw new IOException("Model path is a symbolic link");
        if (target.isFile() && target.length() > 0) return;
        if (!expected.matches("[0-9a-f]{64}")) throw new IOException("Invalid base-model digest");
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot prepare model directory");
        File pending = new File(parent, target.getName() + ".part");
        if (Files.isSymbolicLink(pending.toPath())) throw new IOException("Temporary model path is a symbolic link");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = source.open(); FileOutputStream out = new FileOutputStream(pending)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) { out.write(buffer, 0, count); digest.update(buffer, 0, count); }
                out.getFD().sync();
            }
            StringBuilder actual = new StringBuilder();
            for (byte value : digest.digest()) actual.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            if (!expected.equals(actual.toString())) throw new IOException("Packaged base model checksum mismatch");
            pending.setReadable(false, false); pending.setWritable(false, false);
            pending.setReadable(true, true); pending.setWritable(true, true);
            Files.move(pending.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchAlgorithmException error) { throw new IOException(error); }
        finally { Files.deleteIfExists(pending.toPath()); }
    }
}
