package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Minimal self-contained Gradle Wrapper implementation.
 *
 * <p>This class reads {@code gradle/wrapper/gradle-wrapper.properties}, downloads the Gradle
 * distribution if needed, unpacks it into the Gradle user home, and then delegates to the
 * downloaded Gradle executable.</p>
 *
 * <p>It intentionally implements only the features required for typical CI/dev usage.</p>
 */
public final class GradleWrapperMain {

  public static void main(String[] args) throws Exception {
    Path projectDir = Paths.get(System.getProperty("user.dir"));
    Path propsPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
    if (!Files.exists(propsPath)) {
      System.err.println("Missing " + propsPath);
      System.exit(1);
    }

    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsPath)) {
      props.load(in);
    }

    String distributionUrl = require(props, "distributionUrl");
    Path gradleUserHome = resolveGradleUserHome();
    String distBase = props.getProperty("distributionBase", "GRADLE_USER_HOME");
    String distPath = props.getProperty("distributionPath", "wrapper/dists");

    Path distsDir = ("GRADLE_USER_HOME".equals(distBase) ? gradleUserHome : projectDir).resolve(distPath);
    Files.createDirectories(distsDir);

    String fileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
    String distName = fileName.replace(".zip", "");
    // Use a stable dir name; real wrapper uses hash+uri, but this is sufficient.
    Path distDir = distsDir.resolve(distName);
    Path marker = distDir.resolve(".ok");
    Path zipPath = distsDir.resolve(fileName);

    if (!Files.exists(marker)) {
      if (!Files.exists(zipPath)) {
        download(distributionUrl, zipPath);
      }
      unpack(zipPath, distDir);
      Files.writeString(marker, "ok");
    }

    Path gradleHome = findGradleHome(distDir);
    Path gradleBin = gradleHome.resolve(isWindows() ? "bin/gradle.bat" : "bin/gradle");

    if (!Files.exists(gradleBin)) {
      System.err.println("Gradle executable not found at: " + gradleBin);
      System.exit(1);
    }

    ProcessBuilder pb = new ProcessBuilder();
    pb.command(buildCommand(gradleBin, args));
    pb.directory(projectDir.toFile());
    pb.inheritIO();
    Process p = pb.start();
    int code = p.waitFor();
    System.exit(code);
  }

  private static String require(Properties p, String key) {
    String v = p.getProperty(key);
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException("Missing property: " + key);
    }
    return v.trim();
  }

  private static Path resolveGradleUserHome() {
    String env = System.getenv("GRADLE_USER_HOME");
    if (env != null && !env.isBlank()) return Paths.get(env);
    return Paths.get(System.getProperty("user.home")).resolve(".gradle");
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  private static void download(String url, Path target) throws IOException {
    System.out.println("Downloading Gradle distribution: " + url);
    URL u = URI.create(url).toURL();
    URLConnection c = u.openConnection();
    c.setConnectTimeout(30_000);
    c.setReadTimeout(120_000);

    Files.createDirectories(target.getParent());
    try (InputStream in = c.getInputStream();
         OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      byte[] buf = new byte[8192];
      int r;
      long total = 0;
      while ((r = in.read(buf)) >= 0) {
        out.write(buf, 0, r);
        total += r;
        if (total % (10L * 1024 * 1024) < 8192) {
          System.out.print(".");
        }
      }
      System.out.println();
    }
  }

  private static void unpack(Path zip, Path destDir) throws IOException {
    System.out.println("Unpacking: " + zip + " -> " + destDir);
    if (Files.exists(destDir)) {
      deleteRecursively(destDir);
    }
    Files.createDirectories(destDir);

    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
      ZipEntry e;
      while ((e = zis.getNextEntry()) != null) {
        Path out = destDir.resolve(e.getName()).normalize();
        if (!out.startsWith(destDir)) {
          throw new IOException("Zip slip detected: " + e.getName());
        }
        if (e.isDirectory()) {
          Files.createDirectories(out);
        } else {
          Files.createDirectories(out.getParent());
          try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            zis.transferTo(os);
          }
          // Make executables executable on Unix
          if (!isWindows() && out.getFileName().toString().equals("gradle")) {
            out.toFile().setExecutable(true, false);
          }
        }
      }
    }
  }

  private static Path findGradleHome(Path distDir) throws IOException {
    // Typical gradle distribution zips contain a single top-level directory like gradle-8.13/
    try (var stream = Files.list(distDir)) {
      return stream
        .filter(Files::isDirectory)
        .findFirst()
        .orElseThrow(() -> new IOException("Cannot locate Gradle home under " + distDir));
    }
  }

  private static String[] buildCommand(Path gradleBin, String[] args) {
    String[] cmd = new String[1 + args.length];
    cmd[0] = gradleBin.toAbsolutePath().toString();
    System.arraycopy(args, 0, cmd, 1, args.length);
    return cmd;
  }

  private static void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) return;
    Files.walk(path)
      .sorted((a, b) -> b.getNameCount() - a.getNameCount())
      .forEach(p -> {
        try { Files.deleteIfExists(p); } catch (IOException ex) { throw new UncheckedIOException(ex); }
      });
  }

  private GradleWrapperMain() {}
}
