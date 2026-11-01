const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const rootDir = path.resolve(__dirname, '..');
const isWindows = process.platform === 'win32';

// 1. Resolve Maven command (mvnw or system mvn)
function resolveMavenCmd() {
    const mvnwCmd = isWindows ? path.join(rootDir, 'mvnw.cmd') : path.join(rootDir, 'mvnw');
    if (fs.existsSync(mvnwCmd)) {
        if (!isWindows) {
            try {
                fs.chmodSync(mvnwCmd, '755');
            } catch (e) {}
        }
        return { cmd: mvnwCmd, isWrapper: true };
    }
    return { cmd: isWindows ? 'mvn.cmd' : 'mvn', isWrapper: false };
}

// 2. Resolve Environment (prefer JAVADEC_JAVA_HOME if defined)
function resolveEnv() {
    const env = { ...process.env };
    if (env.JAVADEC_JAVA_HOME && fs.existsSync(env.JAVADEC_JAVA_HOME)) {
        env.JAVA_HOME = env.JAVADEC_JAVA_HOME;
    }
    return env;
}

// 3. Auto-detect generated jar file in target directory
function findTargetJar(dir, moduleName) {
    const targetDir = path.join(dir, moduleName, 'target');
    if (!fs.existsSync(targetDir)) return null;

    try {
        const files = fs.readdirSync(targetDir);
        const candidates = files.filter(f =>
            f.startsWith(moduleName) &&
            f.endsWith('.jar') &&
            !f.startsWith('original-') &&
            !f.endsWith('-sources.jar') &&
            !f.endsWith('-javadoc.jar')
        );

        if (candidates.length === 0) return null;
        candidates.sort((a, b) => {
            const statA = fs.statSync(path.join(targetDir, a));
            const statB = fs.statSync(path.join(targetDir, b));
            return statB.mtimeMs - statA.mtimeMs;
        });
        return path.join(targetDir, candidates[0]);
    } catch (e) {
        return null;
    }
}

function build() {
    console.log('=== [Building Java Modules with Maven] ===');
    const { cmd: mavenCmd, isWrapper } = resolveMavenCmd();
    console.log(`Using Maven: ${isWrapper ? 'Maven Wrapper (' + mavenCmd + ')' : 'System Maven (' + mavenCmd + ')'}`);

    const extraArgs = process.argv.slice(2);
    const mavenArgs = extraArgs.length > 0 ? extraArgs : ['clean', 'package', '-DskipTests'];

    const childEnv = resolveEnv();
    if (childEnv.JAVA_HOME) {
        console.log(`Using JAVA_HOME: ${childEnv.JAVA_HOME}`);
    }

    const buildRes = isWindows
        ? spawnSync('cmd.exe', ['/c', mavenCmd, ...mavenArgs], {
              cwd: rootDir,
              env: childEnv,
              stdio: 'inherit'
          })
        : spawnSync(mavenCmd, mavenArgs, {
              cwd: rootDir,
              env: childEnv,
              stdio: 'inherit'
          });

    if (buildRes.status !== 0) {
        console.error(`\n[ERROR] Maven build failed with exit code ${buildRes.status}`);
        process.exit(buildRes.status || 1);
    }

    const jarsDir = path.join(rootDir, 'jars');
    if (!fs.existsSync(jarsDir)) {
        fs.mkdirSync(jarsDir, { recursive: true });
    }

    const cliTargetJar = findTargetJar(rootDir, 'javadec-cli');
    const mcpTargetJar = findTargetJar(rootDir, 'javadec-mcp');
    const cliDistJar = path.join(jarsDir, 'javadec-cli.jar');
    const mcpDistJar = path.join(jarsDir, 'javadec-mcp.jar');

    if (cliTargetJar) {
        fs.copyFileSync(cliTargetJar, cliDistJar);
        console.log(`Copied ${cliTargetJar} -> ${cliDistJar}`);
    }
    if (mcpTargetJar) {
        fs.copyFileSync(mcpTargetJar, mcpDistJar);
        console.log(`Copied ${mcpTargetJar} -> ${mcpDistJar}`);
    }

    if (!fs.existsSync(cliDistJar) || !fs.existsSync(mcpDistJar)) {
        console.error(`\n[ERROR] Output JARs missing in ${jarsDir}. Please verify maven build.`);
        process.exit(1);
    }

    console.log('\n[SUCCESS] Build completed. Packaged JARs ready in jars/');
}

if (require.main === module) {
    build();
}

module.exports = { build };
