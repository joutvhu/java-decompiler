package com.joutvhu.javadec.core.loader;

import java.io.File;

/**
 * Utility for resolving, normalizing, and expanding file paths across platforms.
 * Automatically expands user home tilde (~), $HOME, and %USERPROFILE%,
 * strips surrounding quotes, and sanitizes 'file://' URI schemes.
 */
public class PathUtil {

    public static String expandUserHome(String path) {
        if (path == null) return null;
        String p = path.trim();

        // 1. Strip surrounding quotes
        if ((p.startsWith("\"") && p.endsWith("\"")) || (p.startsWith("'") && p.endsWith("'"))) {
            p = p.substring(1, p.length() - 1).trim();
        }

        // 2. Strip file:// URI scheme if present (e.g. file:///C:/path or file:/home/user)
        if (p.startsWith("file:///")) {
            p = p.substring(8);
            // On Unix systems, restore leading slash if needed
            if (p.indexOf(':') < 0 && !p.startsWith("/")) {
                p = "/" + p;
            }
        } else if (p.startsWith("file://")) {
            p = p.substring(7);
        } else if (p.startsWith("file:/")) {
            p = p.substring(5);
            if (p.indexOf(':') < 0 && !p.startsWith("/")) {
                p = "/" + p;
            }
        }

        // 3. Expand user home across operating systems
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
            if (p.startsWith("%USERPROFILE%/") || p.startsWith("%USERPROFILE%\\")) {
                return normalizedHome + p.substring(14);
            }
            if (p.equalsIgnoreCase("%USERPROFILE%")) {
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
