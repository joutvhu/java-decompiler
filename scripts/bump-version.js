const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');
const packageJsonPath = path.join(rootDir, 'package.json');
const rootPomPath = path.join(rootDir, 'pom.xml');
const submodulePoms = [
    path.join(rootDir, 'javadec-core', 'pom.xml'),
    path.join(rootDir, 'javadec-cli', 'pom.xml'),
    path.join(rootDir, 'javadec-mcp', 'pom.xml')
];
const cliJavaPath = path.join(rootDir, 'javadec-cli', 'src', 'main', 'java', 'com', 'joutvhu', 'javadec', 'cli', 'JavadecCli.java');
const mcpJavaPath = path.join(rootDir, 'javadec-mcp', 'src', 'main', 'java', 'com', 'joutvhu', 'javadec', 'mcp', 'JavadecMcpServer.java');

function parseSemver(v) {
    const match = v.match(/^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$/);
    if (!match) return null;
    return {
        major: parseInt(match[1], 10),
        minor: parseInt(match[2], 10),
        patch: parseInt(match[3], 10),
        prerelease: match[4] || ''
    };
}

function bumpSemver(current, type) {
    const parsed = parseSemver(current);
    if (!parsed) {
        throw new Error(`Cannot parse current version "${current}" as semver.`);
    }
    let { major, minor, patch } = parsed;
    if (type === 'major') {
        major += 1;
        minor = 0;
        patch = 0;
    } else if (type === 'minor') {
        minor += 1;
        patch = 0;
    } else if (type === 'patch') {
        patch += 1;
    } else {
        throw new Error(`Unknown bump type "${type}". Use major, minor, or patch.`);
    }
    return `${major}.${minor}.${patch}`;
}

function updateRootPom(filePath, newVersion) {
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /(<artifactId>javadec-parent<\/artifactId>\s*<version>)([^<]+)(<\/version>)/;
    if (!regex.test(content)) {
        throw new Error(`Could not find <version> for javadec-parent in ${filePath}`);
    }
    const updated = content.replace(regex, `$1${newVersion}$3`);
    fs.writeFileSync(filePath, updated, 'utf8');
}

function updateSubmodulePom(filePath, newVersion) {
    const content = fs.readFileSync(filePath, 'utf8');
    const regex = /(<parent>[\s\S]*?<artifactId>javadec-parent<\/artifactId>[\s\S]*?<version>)([^<]+)(<\/version>)/;
    if (!regex.test(content)) {
        throw new Error(`Could not find parent <version> in ${filePath}`);
    }
    const updated = content.replace(regex, `$1${newVersion}$3`);
    fs.writeFileSync(filePath, updated, 'utf8');
}

function updateJavaFile(filePath, regex, replacement) {
    if (fs.existsSync(filePath)) {
        const content = fs.readFileSync(filePath, 'utf8');
        const updated = content.replace(regex, replacement);
        fs.writeFileSync(filePath, updated, 'utf8');
    }
}

function main() {
    const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    const currentVersion = pkg.version;
    const arg = process.argv[2];

    let targetVersion = '';

    if (!arg) {
        // Called during npm version lifecycle hook: package.json has already been updated by npm
        targetVersion = process.env.npm_package_version || pkg.version;
    } else if (['major', 'minor', 'patch'].includes(arg.toLowerCase())) {
        targetVersion = bumpSemver(currentVersion, arg.toLowerCase());
        pkg.version = targetVersion;
        fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');
    } else {
        // Explicit version provided (e.g. 1.0.1 or 1.0.1-SNAPSHOT)
        targetVersion = arg.replace(/^v/, '');
        pkg.version = targetVersion;
        fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n', 'utf8');
    }

    console.log(`\n=== [Syncing Project Versions to ${targetVersion}] ===`);
    console.log(`- Updated package.json: ${currentVersion} -> ${targetVersion}`);

    // 1. Update root pom.xml
    updateRootPom(rootPomPath, targetVersion);
    console.log(`- Updated root pom.xml`);

    // 2. Update submodule pom.xml files
    for (const pom of submodulePoms) {
        updateSubmodulePom(pom, targetVersion);
        const relPath = path.relative(rootDir, pom);
        console.log(`- Updated ${relPath}`);
    }

    // 3. Update JavadecCli.java and JavadecMcpServer.java version strings
    updateJavaFile(cliJavaPath, /version\s*=\s*"javadec\s+[^"]+"/, `version = "javadec ${targetVersion}"`);
    updateJavaFile(mcpJavaPath, /\.serverInfo\("javadec-mcp",\s*"[^"]+"\)/, `.serverInfo("javadec-mcp", "${targetVersion}")`);
    console.log(`- Updated Java CLI and MCP Server version strings`);

    // 4. Stage changes in git if git repository is active
    try {
        const filesToStage = [
            'package.json',
            'pom.xml',
            ...submodulePoms.map(p => path.relative(rootDir, p)),
            path.relative(rootDir, cliJavaPath),
            path.relative(rootDir, mcpJavaPath)
        ];
        execSync(`git add ${filesToStage.join(' ')}`, { cwd: rootDir, stdio: 'ignore' });
        console.log(`- Staged version changes in git`);
    } catch (e) {}

    console.log(`\n[SUCCESS] Synchronized version ${targetVersion} across package.json, POMs, and Java classes.\n`);
}

main();
