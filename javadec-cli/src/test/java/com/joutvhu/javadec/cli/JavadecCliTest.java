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

    private final File refJar = new File("../.source/jetbrains/java-decompiler.jar");

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
        int exitCode = cmd.execute("list", refJar.getAbsolutePath(), "-p", "ConsoleDecompiler");
        assertEquals(0, exitCode);
    }

    @Test
    public void testOutlineCommandWithUriAndSpaceSeparated() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        // 1. Target URI format
        int code1 = cmd.execute("outline", refJar.getAbsolutePath() + "!org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class");
        assertEquals(0, code1);

        // 2. Space separated arguments
        int code2 = cmd.execute("outline", refJar.getAbsolutePath(), "org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class");
        assertEquals(0, code2);
    }

    @Test
    public void testViewCommandWithMethodAndPagination() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        int exitCode = cmd.execute("view", refJar.getAbsolutePath() + "!org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class", "-m", "main", "--limit", "5");
        assertEquals(0, exitCode);
    }

    @Test
    public void testResourceCommandWithPagination() {
        CommandLine cmd = new CommandLine(new JavadecCli());

        int exitCode = cmd.execute("resource", refJar.getAbsolutePath(), "META-INF/plugin.xml", "--offset", "1", "--limit", "5");
        assertEquals(0, exitCode);
    }

    @Test
    public void testDumpAndExtractCommands(@TempDir Path tempDir) {
        CommandLine cmd = new CommandLine(new JavadecCli());

        File dumpDir = tempDir.resolve("dump").toFile();
        int dumpCode = cmd.execute("dump", refJar.getAbsolutePath(), "-o", dumpDir.getAbsolutePath(), "-p", "ConsoleDecompiler");
        assertEquals(0, dumpCode);

        File extractDir = tempDir.resolve("extract").toFile();
        int extractCode = cmd.execute("extract", refJar.getAbsolutePath(), "-o", extractDir.getAbsolutePath(), "-p", "META-INF/plugin.xml");
        assertEquals(0, extractCode);
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
