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

const args = process.argv.slice(2);
const isMcp = args.length > 0 && args[0] === 'mcp';

const jarName = isMcp ? 'javadec-mcp.jar' : 'javadec-cli.jar';
const jarToRun = path.join(__dirname, '..', 'jars', jarName);

const javaArgs = isMcp
    ? ['-jar', jarToRun, ...args.slice(1)]
    : ['-jar', jarToRun, ...args];

if (!fs.existsSync(jarToRun)) {
    console.error(`[javadec] Error: Core JAR not found at: ${jarToRun}`);
    console.error(`[javadec] Please build the package first: npm run build`);
    process.exit(1);
}

const javaBin = findJavaExecutable();
const child = spawn(javaBin, javaArgs, {
    stdio: 'inherit',
    windowsHide: true
});

child.on('error', (err) => {
    if (err.code === 'ENOENT') {
        console.error('[javadec] Error: Java runtime ("java") was not found in PATH.');
        console.error('[javadec] Please install Java (JDK 21+) or set JAVADEC_JAVA_PATH / JAVADEC_JAVA_HOME.');
    } else {
        console.error(`[javadec] Error executing process: ${err.message}`);
    }
    process.exit(1);
});

child.on('close', (code) => {
    process.exit(code || 0);
});
