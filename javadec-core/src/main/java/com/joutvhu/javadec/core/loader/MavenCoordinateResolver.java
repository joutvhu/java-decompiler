package com.joutvhu.javadec.core.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Resolves Maven GAV coordinates (e.g. 'org.springframework:spring-core:6.1.0') into local JAR files.
 * Checks local Maven repository (~/.m2/repository) and Gradle cache first;
 * if absent, automatically downloads the artifact (and companion sources jar) from Maven Central.
 */
public class MavenCoordinateResolver {

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean isMavenCoordinate(String input) {
        if (input == null) return false;
        String s = input.trim();
        if (s.contains("/") || s.contains("\\")) return false;
        if (s.length() >= 2 && s.charAt(1) == ':') return false; // Windows drive letter
        String[] parts = s.split(":");
        return parts.length >= 3 && parts.length <= 4 && !parts[0].isEmpty() && !parts[1].isEmpty() && !parts[2].isEmpty();
    }

    /**
     * Resolves a coordinate 'groupId:artifactId:version' to a local File on disk.
     */
    public static File resolveCoordinate(String coordinate) throws Exception {
        String[] parts = coordinate.trim().split(":");
        String groupId = parts[0].trim();
        String artifactId = parts[1].trim();
        String version = parts[2].trim();

        String groupPath = groupId.replace('.', '/');
        String jarFileName = artifactId + "-" + version + ".jar";
        String sourcesFileName = artifactId + "-" + version + "-sources.jar";

        String userHome = System.getProperty("user.home");

        // 1. Check local Maven repository
        File m2RepoDir = new File(userHome, ".m2/repository/" + groupPath + "/" + artifactId + "/" + version);
        File localM2Jar = new File(m2RepoDir, jarFileName);
        if (localM2Jar.exists() && localM2Jar.isFile()) {
            return localM2Jar;
        }

        // 2. Check Gradle cache
        File gradleCacheDir = new File(userHome, ".gradle/caches/modules-2/files-2.1/" + groupId + "/" + artifactId + "/" + version);
        if (gradleCacheDir.exists() && gradleCacheDir.isDirectory()) {
            File[] hashDirs = gradleCacheDir.listFiles(File::isDirectory);
            if (hashDirs != null) {
                for (File hDir : hashDirs) {
                    File candidate = new File(hDir, jarFileName);
                    if (candidate.exists() && candidate.isFile()) {
                        return candidate;
                    }
                }
            }
        }

        // 3. Download from Maven Central into local repository
        m2RepoDir.mkdirs();
        String remoteUrl = MAVEN_CENTRAL_URL + "/" + groupPath + "/" + artifactId + "/" + version + "/" + jarFileName;
        System.err.println("[javadec] Downloading " + coordinate + " from Maven Central...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteUrl))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "javadec/1.0.0 (Java Decompiler for AI & Developers)")
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 404) {
            throw new FileNotFoundException("Artifact not found in Maven Central: " + coordinate + " (URL: " + remoteUrl + ")");
        }
        if (response.statusCode() != 200) {
            throw new Exception("Failed to download " + coordinate + " (HTTP " + response.statusCode() + " from " + remoteUrl + ")");
        }

        Path targetPath = localM2Jar.toPath();
        try (InputStream is = response.body()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 4. Concurrently attempt to download companion -sources.jar for Javadocs (best-effort)
        try {
            File localSourcesJar = new File(m2RepoDir, sourcesFileName);
            if (!localSourcesJar.exists()) {
                String sourcesRemoteUrl = MAVEN_CENTRAL_URL + "/" + groupPath + "/" + artifactId + "/" + version + "/" + sourcesFileName;
                HttpRequest sourcesReq = HttpRequest.newBuilder()
                        .uri(URI.create(sourcesRemoteUrl))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();
                HTTP_CLIENT.sendAsync(sourcesReq, HttpResponse.BodyHandlers.ofInputStream())
                        .thenAccept(res -> {
                            if (res.statusCode() == 200) {
                                try (InputStream sis = res.body()) {
                                    Files.copy(sis, localSourcesJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (Exception ignored) {}
                            }
                        });
            }
        } catch (Exception ignored) {}

        return localM2Jar;
    }
}
