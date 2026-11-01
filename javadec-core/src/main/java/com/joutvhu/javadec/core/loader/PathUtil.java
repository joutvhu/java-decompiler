package com.joutvhu.javadec.core.loader;

import java.io.File;

/**
 * Utility for resolving, normalizing, and expanding file paths across platforms.
 * Automatically expands user home tilde (~) and $HOME, and strips surrounding quotes.
 */
public class PathUtil {

    public static String expandUserHome(String path) {
        if (path == null) return null;
        String p = path.trim();
        if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'"))) {
            p = p.substring(1, p.length() - 1).trim();
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            String normalizedHome = userHome.replace('\\', '/');
            if (p.equals("~")) {
                return normalizedHome;
            }
            if (p.startsWith("~/") || p.startsWith("~\\")) {
                return normalizedHome + p.substring(1);
            }
            if (p.startsWith("$HOME/") || p.startsWith("$HOME\\")) {
                return normalizedHome + p.substring(5);
            }
            if (p.equals("$HOME")) {
                return normalizedHome;
            }
        }
        return p;
    }

    public static File resolveFile(File file) {
        if (file == null) return null;
        return new File(expandUserHome(file.getPath()));
    }

    public static File resolveFile(String path) {
        if (path == null) return null;
        return new File(expandUserHome(path));
    }
}
