package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.loader.PathUtil;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class PathUtilTest {

    @Test
    public void testNullAndBlankPaths() {
        assertNull(PathUtil.expandUserHome(null));
        assertNull(PathUtil.resolveFile((File) null));
        assertNull(PathUtil.resolveFile((String) null));
    }

    @Test
    public void testQuoteStripping() {
        assertEquals("C:/path/file.jar", PathUtil.expandUserHome("\"C:/path/file.jar\""));
        assertEquals("C:/path/file.jar", PathUtil.expandUserHome("'C:/path/file.jar'"));
        assertEquals("nested", PathUtil.expandUserHome("\"  nested  \""));
    }

    @Test
    public void testFileUriStripping() {
        String p1 = PathUtil.expandUserHome("file:///C:/libs/app.jar");
        assertTrue(p1.equals("C:/libs/app.jar") || p1.equals("/C:/libs/app.jar"));

        String p2 = PathUtil.expandUserHome("file://libs/app.jar");
        assertEquals("libs/app.jar", p2);

        String p3 = PathUtil.expandUserHome("file:/C:/libs/app.jar");
        assertTrue(p3.equals("C:/libs/app.jar") || p3.equals("/C:/libs/app.jar"));
    }

    @Test
    public void testTildeAndHomeExpansion() {
        String userHome = System.getProperty("user.home").replace('\\', '/');

        assertEquals(userHome, PathUtil.expandUserHome("~"));
        assertEquals(userHome + "/.m2/repository", PathUtil.expandUserHome("~/.m2/repository"));
        assertEquals(userHome + "/.gradle/caches", PathUtil.expandUserHome("~\\.gradle\\caches"));

        assertEquals(userHome, PathUtil.expandUserHome("$HOME"));
        assertEquals(userHome + "/.m2/repository", PathUtil.expandUserHome("$HOME/.m2/repository"));
        assertEquals(userHome + "/.gradle/caches", PathUtil.expandUserHome("$HOME\\.gradle\\caches"));

        assertEquals(userHome, PathUtil.expandUserHome("%USERPROFILE%"));
        assertEquals(userHome + "/.m2", PathUtil.expandUserHome("%USERPROFILE%/.m2"));
        assertEquals(userHome + "/.m2", PathUtil.expandUserHome("%USERPROFILE%\\.m2"));
    }

    @Test
    public void testResolveFile() {
        String userHome = System.getProperty("user.home");
        File resolved = PathUtil.resolveFile("~/.m2");
        assertNotNull(resolved);
        assertTrue(resolved.getAbsolutePath().contains(userHome.replace('/', File.separatorChar)));
    }
}
