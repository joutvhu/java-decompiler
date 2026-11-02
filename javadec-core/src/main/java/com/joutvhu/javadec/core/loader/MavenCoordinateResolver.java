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
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves Maven GAV coordinates (e.g. 'org.springframework:spring-core:6.1.0') into local JAR files.
 * Automatically respects user and corporate ~/.m2/settings.xml (custom localRepository, Nexus/Artifactory mirrors,
 * private repositories, and server authentication tokens).
 */
public class MavenCoordinateResolver {

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

        MavenSettings settings = MavenSettings.load();
        String localRepoPath = settings.getLocalRepositoryPath();

        // 1. Check local Maven repository (respecting custom <localRepository> from settings.xml)
        File m2RepoDir = new File(localRepoPath, groupPath + "/" + artifactId + "/" + version);
        File localM2Jar = new File(m2RepoDir, jarFileName);
        if (localM2Jar.exists() && localM2Jar.isFile()) {
            return localM2Jar;
        }

        // 2. Check Gradle cache
        String userHome = System.getProperty("user.home");
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

        // 3. Download from configured repositories (mirrors, active profiles, corporate Nexus, and central fallback)
        m2RepoDir.mkdirs();
        List<String> triedUrls = new ArrayList<>();
        boolean downloaded = false;

        for (MavenSettings.RepoConfig repo : settings.getRepositories()) {
            String remoteJarUrl = repo.url() + "/" + groupPath + "/" + artifactId + "/" + version + "/" + jarFileName;
            triedUrls.add(remoteJarUrl);

            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(remoteJarUrl))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "javadec/1.0.0 (Java Decompiler for AI & Developers)")
                        .GET();

                String auth = settings.getAuthorizationHeader(repo.id());
                if (auth != null) {
                    reqBuilder.header("Authorization", auth);
                }

                HttpResponse<InputStream> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 200) {
                    System.err.println("[javadec] Downloaded " + coordinate + " from " + repo.url());
                    Path targetPath = localM2Jar.toPath();
                    Path partPath = new File(m2RepoDir, jarFileName + ".part-" + System.currentTimeMillis()).toPath();
                    try {
                        try (InputStream is = response.body()) {
                            Files.copy(is, partPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        try {
                            Files.move(partPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception moveEx) {
                            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception copyEx) {
                        try { Files.deleteIfExists(partPath); } catch (Exception ignoredDel) {}
                        throw copyEx;
                    }
                    downloaded = true;

                    // 4. Concurrently attempt to download companion -sources.jar from the same repository (best-effort)
                    try {
                        File localSourcesJar = new File(m2RepoDir, sourcesFileName);
                        if (!localSourcesJar.exists()) {
                            String remoteSourcesUrl = repo.url() + "/" + groupPath + "/" + artifactId + "/" + version + "/" + sourcesFileName;
                            HttpRequest.Builder sourcesReq = HttpRequest.newBuilder()
                                    .uri(URI.create(remoteSourcesUrl))
                                    .timeout(Duration.ofSeconds(20))
                                    .header("User-Agent", "javadec/1.0.0 (Java Decompiler for AI & Developers)")
                                    .GET();
                            if (auth != null) {
                                sourcesReq.header("Authorization", auth);
                            }
                            Path partSourcesPath = new File(m2RepoDir, sourcesFileName + ".part-" + System.currentTimeMillis()).toPath();
                            HTTP_CLIENT.sendAsync(sourcesReq.build(), HttpResponse.BodyHandlers.ofInputStream())
                                    .thenAccept(res -> {
                                        if (res.statusCode() == 200) {
                                            try {
                                                try (InputStream sis = res.body()) {
                                                    Files.copy(sis, partSourcesPath, StandardCopyOption.REPLACE_EXISTING);
                                                }
                                                try {
                                                    Files.move(partSourcesPath, localSourcesJar.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                                                } catch (Exception me) {
                                                    Files.move(partSourcesPath, localSourcesJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                                }
                                            } catch (Exception ignored) {
                                                try { Files.deleteIfExists(partSourcesPath); } catch (Exception ignored2) {}
                                            }
                                        }
                                    });
                        }
                    } catch (Exception ignored) {}

                    break;
                }
            } catch (Exception ignored) {
                // Try next repository
            }
        }

        if (!downloaded || !localM2Jar.exists()) {
            throw new FileNotFoundException("Artifact not found: " + coordinate + ". Attempted repositories: " + triedUrls);
        }

        return localM2Jar;
    }
}
