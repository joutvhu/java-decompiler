#!/usr/bin/env node

const { spawn, spawnSync } = require('child_process');
const path = require('path');
const fs = require('fs');

function parseJavaMajorVersion(javaExecutable) {
    try {
        const res = spawnSync(javaExecutable, ['-version'], { encoding: 'utf8', windowsHide: true });
        const output = (res.stderr || '') + (res.stdout || '');
        const match = output.match(/(?:java|openjdk)\s+(?:version\s+)?"?(\d+)(?:\.(\d+))?/i);
        if (match) {
            const first = parseInt(match[1], 10);
            if (first === 1 && match[2]) {
                return parseInt(match[2], 10); // e.g. 1.8 -> 8
            }
            return first; // e.g. 21 -> 21
        }
    } catch (e) {}
    return null;
}

function findInstalledModernJdk() {
    const candidates = [];

    if (process.platform === 'win32') {
        const programFiles = [process.env['ProgramFiles'], process.env['ProgramFiles(x86)']];
        for (const pf of programFiles) {
            if (!pf) continue;
            const roots = [
                path.join(pf, 'Java'),
                path.join(pf, 'Eclipse Adoptium'),
                path.join(pf, 'Microsoft'),
                path.join(pf, 'BellSoft'),
                path.join(pf, 'Amazon Corretto'),
                path.join(pf, 'Zulu')
            ];
            for (const r of roots) {
                if (fs.existsSync(r)) {
                    try {
                        const dirs = fs.readdirSync(r);
                        for (const d of dirs) {
                            const javaBin = path.join(r, d, 'bin', 'java.exe');
                            if (fs.existsSync(javaBin)) {
                                candidates.push(javaBin);
                            }
                        }
                    } catch (e) {}
                }
            }
        }
    } else if (process.platform === 'darwin') {
        const jvmDir = '/Library/Java/JavaVirtualMachines';
        if (fs.existsSync(jvmDir)) {
            try {
                const dirs = fs.readdirSync(jvmDir);
                for (const d of dirs) {
                    const javaBin = path.join(jvmDir, d, 'Contents', 'Home', 'bin', 'java');
                    if (fs.existsSync(javaBin)) {
                        candidates.push(javaBin);
                    }
                }
            } catch (e) {}
        }
    } else {
        const jvmDir = '/usr/lib/jvm';
        if (fs.existsSync(jvmDir)) {
            try {
                const dirs = fs.readdirSync(jvmDir);
                for (const d of dirs) {
                    const javaBin = path.join(jvmDir, d, 'bin', 'java');
                    if (fs.existsSync(javaBin)) {
                        candidates.push(javaBin);
                    }
                }
            } catch (e) {}
        }
    }

    for (const cand of candidates) {
        const ver = parseJavaMajorVersion(cand);
        if (ver && ver >= 21) {
            return cand;
        }
    }
    return null;
}

function findJavaExecutable() {
    // 1. Direct path via JAVADEC_JAVA_PATH
    if (process.env.JAVADEC_JAVA_PATH) {
        const customPath = process.env.JAVADEC_JAVA_PATH.replace(/^"(.*)"$/, '$1').trim();
        if (fs.existsSync(customPath)) {
            return customPath;
        }
        console.warn(`[javadec] Warning: JAVADEC_JAVA_PATH was specified as "${customPath}" but file does not exist.`);
    }

    // 2. Specific JDK root directory via JAVADEC_JAVA_HOME
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
            const ver = parseJavaMajorVersion(binJava);
            if (ver && ver >= 21) {
                return binJava;
            }
        }
    }

    // 4. Default system java in PATH
    const defaultVer = parseJavaMajorVersion('java');
    if (defaultVer && defaultVer >= 21) {
        return 'java';
    }

    // 5. If PATH java is too old or missing, look for installed JDK 21+
    const discovered = findInstalledModernJdk();
    if (discovered) {
        return discovered;
    }

    // Fall back to default and let caller get clear version error if too old
    return 'java';
}

const args = process.argv.slice(2);
const isMcp = args.length > 0 && args[0] === 'mcp';

const jarName = isMcp ? 'javadec-mcp.jar' : 'javadec-cli.jar';
const jarToRun = path.join(__dirname, '..', 'jars', jarName);

// Force UTF-8 encoding across JVM console I/O to avoid mojibake box-drawing on Windows
const defaultJvmFlags = [
    '-Dfile.encoding=UTF-8',
    '-Dsun.stdout.encoding=UTF-8',
    '-Dsun.stderr.encoding=UTF-8'
];

const javaArgs = isMcp
    ? [...defaultJvmFlags, '-jar', jarToRun, ...args.slice(1)]
    : [...defaultJvmFlags, '-jar', jarToRun, ...args];

if (!fs.existsSync(jarToRun)) {
    console.error(`[javadec] Error: Core JAR not found at: ${jarToRun}`);
    console.error(`[javadec] Please build the package first: npm run build`);
    process.exit(1);
}

const javaBin = findJavaExecutable();
const detectedVer = parseJavaMajorVersion(javaBin);

if (detectedVer !== null && detectedVer < 21) {
    console.error(`[javadec] Error: javadec requires Java runtime version 21 or higher (detected: Java ${detectedVer}).`);
    console.error(`[javadec] Please install JDK 21+ or set JAVADEC_JAVA_HOME / JAVADEC_JAVA_PATH.`);
    process.exit(1);
}

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
