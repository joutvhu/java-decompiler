const fs = require('fs');
const path = require('path');
const { spawnSync, execSync } = require('child_process');
const { build } = require('./build');

const rootDir = path.resolve(__dirname, '..');
const isWindows = process.platform === 'win32';

const args = process.argv.slice(2);
const skipBuild = args.includes('--skip-build') || args.includes('-s');

if (skipBuild) {
    console.log('=== [1/2] Skipping Maven build (--skip-build requested) ===');
    const jarsDir = path.join(rootDir, 'jars');
    const cliDistJar = path.join(jarsDir, 'javadec-cli.jar');
    const mcpDistJar = path.join(jarsDir, 'javadec-mcp.jar');

    if (!fs.existsSync(cliDistJar) || !fs.existsSync(mcpDistJar)) {
        console.error(`\n[ERROR] Output JARs missing in ${jarsDir}. Please run "npm run build" first.`);
        process.exit(1);
    }
} else {
    console.log('=== [1/2] Building Jars with Maven ===');
    build();
}

console.log('\n=== [2/2] Installing into global npm directory (npm install -g) ===');

// On Windows, if javadec-cli or javadec-mcp is currently running, stop any lingering processes
try {
    if (isWindows) {
        const script = "Get-CimInstance Win32_Process | Where-Object CommandLine -match 'javadec-(cli|mcp)\\.jar' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }";
        const encoded = Buffer.from(script, 'utf16le').toString('base64');
        execSync('powershell.exe -NoProfile -EncodedCommand ' + encoded, { stdio: 'ignore' });
    } else {
        execSync('pkill -f javadec-.*\\.jar || true', { stdio: 'ignore' });
    }
} catch (e) {}

let tarballPath = '';
try {
    const packOutput = execSync('npm pack --ignore-scripts', {
        cwd: rootDir,
        encoding: 'utf8'
    }).trim();
    const tarball = packOutput.split('\n').pop().trim();
    tarballPath = path.join(rootDir, tarball);

    const installRes = isWindows
        ? spawnSync('cmd.exe', ['/c', 'npm', 'install', '-g', tarballPath, '--force'], {
              cwd: rootDir,
              stdio: 'inherit'
          })
        : spawnSync('npm', ['install', '-g', tarballPath, '--force'], {
              cwd: rootDir,
              stdio: 'inherit'
          });

    if (installRes.status !== 0) {
        console.error('\n[ERROR] Global npm install failed with exit code ' + installRes.status);
        process.exit(1);
    }
} catch (err) {
    console.error('\n[ERROR] Failed to package and install globally: ' + err.message);
    process.exit(1);
} finally {
    if (tarballPath && fs.existsSync(tarballPath)) {
        try {
            fs.unlinkSync(tarballPath);
        } catch (e) {}
    }
}

let npmGlobalRoot = '';
try {
    npmGlobalRoot = execSync('npm root -g', { encoding: 'utf8' }).trim();
} catch (e) {}

const installedPath = npmGlobalRoot ? path.join(npmGlobalRoot, 'javadec') : 'global node_modules';

console.log('\n======================================================');
console.log('SUCCESS! javadec has been published to npm -g');
console.log('Global location: ' + installedPath);
console.log('======================================================');
console.log('\n--- HOW TO TEST ---');
console.log('1. Run CLI directly from any terminal:');
console.log('   javadec --help');
console.log('\n2. Configure MCP Server in mcp.json:');
console.log(JSON.stringify({
    mcpServers: {
        javadec: {
            command: "javadec",
            args: ["mcp"]
        }
    }
}, null, 2));
console.log('\n(To uninstall from global: npm uninstall -g javadec)');
