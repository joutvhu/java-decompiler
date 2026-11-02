package com.joutvhu.javadec.core.loader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses user and corporate Maven configuration from ~/.m2/settings.xml.
 * Resolves custom localRepository, mirrors, active profile repositories, and server credentials.
 */
public class MavenSettings {

    private final String localRepositoryPath;
    private final List<RepoConfig> repositories = new ArrayList<>();
    private final Map<String, String> serverCredentials = new HashMap<>(); // serverId -> "Basic base64"

    public record RepoConfig(String id, String url) {}

    public MavenSettings(String localRepositoryPath, List<RepoConfig> repositories, Map<String, String> serverCredentials) {
        this.localRepositoryPath = localRepositoryPath;
        this.repositories.addAll(repositories);
        this.serverCredentials.putAll(serverCredentials);
    }

    public String getLocalRepositoryPath() {
        return localRepositoryPath;
    }

    public List<RepoConfig> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    public String getAuthorizationHeader(String repoId) {
        if (repoId == null) return null;
        return serverCredentials.get(repoId);
    }

    public static MavenSettings load() {
        String userHome = System.getProperty("user.home");
        File userSettings = new File(userHome, ".m2/settings.xml");

        // Also check M2_HOME / MAVEN_HOME global settings if user settings absent
        File settingsFile = userSettings;
        if (!settingsFile.exists() || !settingsFile.isFile()) {
            String m2Home = System.getenv("M2_HOME");
            if (m2Home == null) m2Home = System.getenv("MAVEN_HOME");
            if (m2Home != null) {
                File globalSettings = new File(m2Home, "conf/settings.xml");
                if (globalSettings.exists() && globalSettings.isFile()) {
                    settingsFile = globalSettings;
                }
            }
        }

        String localRepo = new File(userHome, ".m2/repository").getAbsolutePath();
        List<RepoConfig> repos = new ArrayList<>();
        Map<String, String> authMap = new HashMap<>();

        if (settingsFile.exists() && settingsFile.isFile()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(settingsFile);
                doc.getDocumentElement().normalize();

                // 1. Check <localRepository>
                NodeList localRepoNodes = doc.getElementsByTagName("localRepository");
                if (localRepoNodes.getLength() > 0) {
                    String customRepo = localRepoNodes.item(0).getTextContent().trim();
                    if (!customRepo.isEmpty()) {
                        localRepo = PathUtil.expandUserHome(customRepo);
                    }
                }

                // 2. Check <servers> for credentials
                NodeList serverNodes = doc.getElementsByTagName("server");
                for (int i = 0; i < serverNodes.getLength(); i++) {
                    Element serverEl = (Element) serverNodes.item(i);
                    String id = getChildText(serverEl, "id");
                    String username = getChildText(serverEl, "username");
                    String password = getChildText(serverEl, "password");
                    if (id != null && username != null && password != null) {
                        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                        authMap.put(id, "Basic " + token);
                    }
                }

                // 3. Check <mirrors> (mirrors have highest precedence)
                Set<String> seenUrls = new HashSet<>();
                NodeList mirrorNodes = doc.getElementsByTagName("mirror");
                for (int i = 0; i < mirrorNodes.getLength(); i++) {
                    Element mirrorEl = (Element) mirrorNodes.item(i);
                    String id = getChildText(mirrorEl, "id");
                    String url = getChildText(mirrorEl, "url");
                    if (url != null && !url.isBlank()) {
                        String cleanUrl = url.trim().replaceAll("/+$", "");
                        if (seenUrls.add(cleanUrl)) {
                            repos.add(new RepoConfig(id != null ? id : "mirror-" + i, cleanUrl));
                        }
                    }
                }

                // 4. Check <activeProfiles>
                Set<String> activeProfileIds = new HashSet<>();
                NodeList activeProfileNodes = doc.getElementsByTagName("activeProfile");
                for (int i = 0; i < activeProfileNodes.getLength(); i++) {
                    String id = activeProfileNodes.item(i).getTextContent().trim();
                    if (!id.isEmpty()) {
                        activeProfileIds.add(id);
                    }
                }

                // 5. Check <profiles>
                NodeList profileNodes = doc.getElementsByTagName("profile");
                List<RepoConfig> activeProfileRepos = new ArrayList<>();
                List<RepoConfig> otherProfileRepos = new ArrayList<>();

                for (int i = 0; i < profileNodes.getLength(); i++) {
                    Element profileEl = (Element) profileNodes.item(i);
                    String profId = getChildText(profileEl, "id");
                    boolean isActive = profId != null && activeProfileIds.contains(profId);

                    NodeList repoList = profileEl.getElementsByTagName("repository");
                    for (int j = 0; j < repoList.getLength(); j++) {
                        Element rEl = (Element) repoList.item(j);
                        String rId = getChildText(rEl, "id");
                        String rUrl = getChildText(rEl, "url");
                        if (rUrl != null && !rUrl.isBlank()) {
                            String cleanUrl = rUrl.trim().replaceAll("/+$", "");
                            RepoConfig config = new RepoConfig(rId != null ? rId : "repo-" + j, cleanUrl);
                            if (isActive) {
                                activeProfileRepos.add(config);
                            } else {
                                otherProfileRepos.add(config);
                            }
                        }
                    }
                }

                for (RepoConfig rc : activeProfileRepos) {
                    if (seenUrls.add(rc.url())) {
                        repos.add(rc);
                    }
                }
                for (RepoConfig rc : otherProfileRepos) {
                    if (seenUrls.add(rc.url())) {
                        repos.add(rc);
                    }
                }

            } catch (Exception ignored) {}
        }

        // 6. Default fallback to Maven Central if not already included
        boolean hasCentral = repos.stream().anyMatch(r -> r.url().contains("repo1.maven.org") || r.url().contains("repo.maven.apache.org"));
        if (!hasCentral) {
            repos.add(new RepoConfig("central", "https://repo1.maven.org/maven2"));
        }

        return new MavenSettings(localRepo, repos, authMap);
    }

    private static String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }
}
