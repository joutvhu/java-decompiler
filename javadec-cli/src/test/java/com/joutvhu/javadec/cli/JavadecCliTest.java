package com.joutvhu.javadec.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JavadecCliTest {

    private static File testJar;

    private static synchronized File getTestJar() {
        if (testJar != null && testJar.exists()) {
            return testJar;
        }

        try {
            File temp = File.createTempFile("javadec-test-", ".jar");
            temp.deleteOnExit();

            String source = """
                    package com.joutvhu.javadec.test;
                    public class SampleService {
                        public static void main(String[] args) {
                            System.out.println("SampleService CLI mock");
                        }
                    }
                    """;
            javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            File srcDir = java.nio.file.Files.createTempDirectory("javadec-src-").toFile();
            File clsDir = java.nio.file.Files.createTempDirectory("javadec-cls-").toFile();
            File javaFile = new File(srcDir, "com/joutvhu/javadec/test/SampleService.java");
            javaFile.getParentFile().mkdirs();
            clsDir.mkdirs();
            java.nio.file.Files.writeString(javaFile.toPath(), source);

            compiler.run(null, null, null, "-parameters", "-g", "-d", clsDir.getAbsolutePath(), javaFile.getAbsolutePath());

            File classFile = new File(clsDir, "com/joutvhu/javadec/test/SampleService.class");
            byte[] classBytes = java.nio.file.Files.readAllBytes(classFile.toPath());

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(temp))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("com/joutvhu/javadec/test/SampleService.class"));
                zos.write(classBytes);
                zos.closeEntry();

                zos.putNextEntry(new java.util.zip.ZipEntry("META-INF/javadec-test.xml"));
                zos.write("<javadec-test><name>Mock</name></javadec-test>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            java.nio.file.Files.walk(srcDir.toPath()).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
            java.nio.file.Files.walk(clsDir.toPath()).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);

            testJar = temp;
            return testJar;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare test JAR", e);
        }
    }

    private final File refJar = getTestJar();

    @Test
    public void testCliHelpAndVersion() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int helpCode = cmd.execute("--help");
        assertEquals(0, helpCode);

        int verCode = cmd.execute("--version");
        assertEquals(0, verCode);
    }

    @Test
    public void testTreeCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("tree", refJar.getAbsolutePath(), "--depth", "1");
        assertEquals(0, exitCode);
    }

    @Test
    public void testListCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("list", refJar.getAbsolutePath(), "-p", "SampleService");
        assertEquals(0, exitCode);
    }

    @Test
    public void testOutlineCommandWithUriAndSpaceSeparated() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        // 1. Target URI format
        int code1 = cmd.execute("outline", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class");
        assertEquals(0, code1);

        // 2. Space separated arguments
        int code2 = cmd.execute("outline", refJar.getAbsolutePath(), "com/joutvhu/javadec/test/SampleService.class");
        assertEquals(0, code2);
    }

    @Test
    public void testViewCommandWithMethodAndPagination() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        int exitCode = cmd.execute("view", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class", "-m", "main", "--limit", "5");
        assertEquals(0, exitCode);
    }

    @Test
    public void testViewCommandWithClasspathAndNoDeps() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("view", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class", "-cp", refJar.getAbsolutePath(), "--no-deps");
        assertEquals(0, exitCode);
    }

    @Test
    public void testResourceCommandWithPagination() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        int exitCode = cmd.execute("resource", refJar.getAbsolutePath(), "META-INF/javadec-test.xml", "--offset", "1", "--limit", "5");
        assertEquals(0, exitCode);
    }

    @Test
    public void testDumpAndExtractCommands(@TempDir Path tempDir) {
        CommandLine cmd = new CommandLine(new JavadecCli());

        File dumpDir = tempDir.resolve("dump").toFile();
        int dumpCode = cmd.execute("dump", refJar.getAbsolutePath(), "-o", dumpDir.getAbsolutePath(), "-p", "SampleService");
        assertEquals(0, dumpCode);

        File extractDir = tempDir.resolve("extract").toFile();
        int extractCode = cmd.execute("extract", refJar.getAbsolutePath(), "-o", extractDir.getAbsolutePath(), "-p", "META-INF/javadec-test.xml");
        assertEquals(0, extractCode);
    }

    @Test
    public void testFindCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("find", refJar.getAbsolutePath(), "#main", "--limit", "10");
        assertEquals(0, exitCode);
    }

    @Test
    public void testDisasmCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("disasm", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class", "-m", "main");
        assertEquals(0, exitCode);
    }

    @Test
    public void testStringsCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("strings", refJar.getAbsolutePath(), "--limit", "10");
        assertEquals(0, exitCode);
    }

    @Test
    public void testTraceCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("trace", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class", "4");
        assertEquals(0, exitCode);
    }

    @Test
    public void testDiffCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("diff", refJar.getAbsolutePath(), refJar.getAbsolutePath());
        assertEquals(0, exitCode);
    }

    @Test
    public void testCalleesCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("callees", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class", "-m", "main");
        assertEquals(0, exitCode);
    }

    @Test
    public void testDepsCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("deps", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class");
        assertEquals(0, exitCode);
    }

    @Test
    public void testManifestCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("manifest", refJar.getAbsolutePath());
        assertEquals(0, exitCode);
    }

    @Test
    public void testModuleCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("module", refJar.getAbsolutePath());
        assertEquals(0, exitCode);
    }

    @Test
    public void testAuditCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("audit", refJar.getAbsolutePath());
        assertEquals(0, exitCode);
    }

    @Test
    public void testMetricsCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("metrics", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class");
        assertEquals(0, exitCode);
    }

    @Test
    public void testHierarchyCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("hierarchy", refJar.getAbsolutePath() + "!com/joutvhu/javadec/test/SampleService.class");
        assertEquals(0, exitCode);
    }

    @Test
    public void testVerifyCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("verify", refJar.getAbsolutePath());
        assertEquals(0, exitCode);
    }

    @Test
    public void testSearchResCommand() {
        CommandLine cmd = new CommandLine(new JavadecCli());
        int exitCode = cmd.execute("search-res", refJar.getAbsolutePath(), "Mock");
        assertEquals(0, exitCode);
    }

    @Test
    public void testErrorHandlingForNonexistentFiles() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        int code1 = cmd.execute("tree", "nonexistent.jar");
        assertEquals(1, code1);

        int code2 = cmd.execute("list", "nonexistent.jar");
        assertEquals(1, code2);

        int code3 = cmd.execute("resource", "nonexistent.jar", "foo.txt");
        assertEquals(1, code3);
    }

    @Test
    public void testErrorHandlingWhenArchivePassedWithoutClass() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        // Passing JAR directly to view without a class must fail gracefully with exit code 1
        int code1 = cmd.execute("view", refJar.getAbsolutePath());
        assertEquals(1, code1);

        // Passing JAR directly to outline without a class must fail gracefully with exit code 1
        int code2 = cmd.execute("outline", refJar.getAbsolutePath());
        assertEquals(1, code2);
    }
}
