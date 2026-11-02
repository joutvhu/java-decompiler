package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.loader.MavenCoordinateResolver;
import com.joutvhu.javadec.core.loader.PathUtil;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class MavenCoordinateResolverTest {

    @Test
    public void testIsMavenCoordinate() {
        assertTrue(MavenCoordinateResolver.isMavenCoordinate("org.springframework:spring-core:6.1.0"));
        assertTrue(MavenCoordinateResolver.isMavenCoordinate("com.google.guava:guava:33.0.0-jre"));
        assertTrue(MavenCoordinateResolver.isMavenCoordinate("org.ow2.asm:asm:9.7"));

        // File paths or invalid formats must be false
        assertFalse(MavenCoordinateResolver.isMavenCoordinate("C:/path/file.jar"));
        assertFalse(MavenCoordinateResolver.isMavenCoordinate("lib/app.jar"));
        assertFalse(MavenCoordinateResolver.isMavenCoordinate("C:\\path\\file.jar"));
        assertFalse(MavenCoordinateResolver.isMavenCoordinate("app.jar"));
        assertFalse(MavenCoordinateResolver.isMavenCoordinate(null));
        assertFalse(MavenCoordinateResolver.isMavenCoordinate(""));
    }

    @Test
    public void testResolveLocalM2Coordinate() throws Exception {
        // org.ow2.asm:asm:9.7 is a required dependency of javadec, so it is guaranteed present in ~/.m2/repository
        File jar = PathUtil.resolveFile("org.ow2.asm:asm:9.7");
        assertNotNull(jar);
        assertTrue(jar.exists(), "Local Maven repo should contain asm:9.7: " + jar.getAbsolutePath());
        assertTrue(jar.getName().startsWith("asm-9.7.jar"));
    }
}
