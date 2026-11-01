#!/usr/bin/env node

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

function findJavaExecutable() {
    // 1. Direct path to java executable (e.g. C:\jdk\bin\java.exe or /usr/lib/jvm/java-21/bin/java)
    if (process.env.JAVADEC_JAVA_PATH) {
        const customPath = process.env.JAVADEC_JAVA_PATH.replace(/^"(.*)"$/, '$1').trim();
        if (fs.existsSync(customPath)) {
            return customPath;
        }
        console.warn(`[javadec] Warning: JAVADEC_JAVA_PATH was specified as "${customPath}" but file does not exist.`);
    }

    // 2. Specific JDK root directory for javadec
    if (process.env.JAVADEC_JAVA_HOME) {
        const rawHome = process.env.JAVADEC_JAVA_HOME.replace(/^"(.*)"$/, '$1').trim();
        const binJava = path.join(rawHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        if (fs.existsSync(binJava)) {
            return binJava;
        }
        console.warn(`[javadec] Warning: JAVADEC_JAVA_HOME was specified as "${rawHome}" but "${binJava}" does not exist.`);
    }

    // 3. System JAVA_HOME
    if (process.env.JAVA_HOME) {
        const rawHome = process.env.JAVA_HOME.replace(/^"(.*)"$/, '$1').trim();
        const binJava = path.join(rawHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
        if (fs.existsSync(binJava)) {
            return binJava;
        }
    }

    // 4. Default to java in system PATH
    return 'java';
}

function findTargetJar(moduleName) {
    const targetDir = path.join(__dirname, '..', moduleName, 'target');
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

function resolveJar(distJarName, moduleName) {
    // 1. Packaged npm distribution (jars/)
    const distJar = path.join(__dirname, '..', 'jars', distJarName);
    if (fs.existsSync(distJar)) {
        return distJar;
    }
    // 2. Maven target output (local development fallback - auto-detect)
    const targetJar = findTargetJar(moduleName);
    if (targetJar && fs.existsSync(targetJar)) {
        return targetJar;
    }
    return distJar;
}

const args = process.argv.slice(2);
const isMcp = args.length > 0 && args[0] === 'mcp';

const jarToRun = isMcp
    ? resolveJar('javadec-mcp.jar', 'javadec-mcp')
    : resolveJar('javadec-cli.jar', 'javadec-cli');

const javaArgs = isMcp
    ? ['-jar', jarToRun, ...args.slice(1)]
    : ['-jar', jarToRun, ...args];

if (!fs.existsSync(jarToRun)) {
    console.error(`[javadec] Error: Core JAR not found at: ${jarToRun}`);
    console.error(`[javadec] Please build the maven project first: mvn clean package`);
    process.exit(1);
}

const javaBin = findJavaExecutable();
const child = spawn(javaBin, javaArgs, {
    stdio: 'inherit',
    windowsHide: true
});

child.on('error', (err) => {
    if (err.code === 'ENOENT') {
        console.error(`[javadec] Error: Java runtime ("${javaBin}") was not found.`);
        console.error('[javadec] Please install Java (JDK 21+) and ensure "java" is accessible in PATH,');
        console.error('[javadec] or configure JAVADEC_JAVA_PATH / JAVADEC_JAVA_HOME / JAVA_HOME.');
    } else {
        console.error(`[javadec] Error executing process: ${err.message}`);
    }
    process.exit(1);
});

child.on('exit', (code, signal) => {
    if (signal) {
        process.kill(process.pid, signal);
    } else {
        process.exit(code || 0);
    }
});

const forwardSignal = (sig) => {
    if (child && !child.killed) {
        try {
            child.kill(sig);
        } catch (e) {}
    }
};
process.on('SIGINT', () => forwardSignal('SIGINT'));
process.on('SIGTERM', () => forwardSignal('SIGTERM'));
