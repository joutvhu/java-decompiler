package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.SecurityAuditResult;
import com.joutvhu.javadec.core.model.SecurityFindingItem.Severity;
import com.joutvhu.javadec.core.scanner.BytecodeSecurityAuditor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeSecurityAuditorTest {

    @Test
    public void testDetectCommandInjectionAndInsecureDeserialization() {
        String src = """
                package com.example.vulnerable;
                import java.io.InputStream;
                import java.io.ObjectInputStream;
                public class ExploitableService {
                    public void runCommand(String cmd) throws Exception {
                        Runtime.getRuntime().exec(cmd);
                    }
                    public Object deserialize(InputStream is) throws Exception {
                        ObjectInputStream ois = new ObjectInputStream(is);
                        return ois.readObject();
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.vulnerable.ExploitableService", src);
        byte[] bytes = classes.get("com.example.vulnerable.ExploitableService");
        assertNotNull(bytes);

        SecurityAuditResult result = BytecodeSecurityAuditor.auditClass(bytes, "ExploitableService", null);
        assertNotNull(result);
        assertTrue(result.criticalCount() >= 1, "Should detect critical command injection");
        assertTrue(result.highCount() >= 1, "Should detect high insecure deserialization");

        boolean hasCommand = result.findings().stream().anyMatch(f -> f.ruleId().equals("COMMAND_INJECTION"));
        boolean hasDeserialization = result.findings().stream().anyMatch(f -> f.ruleId().equals("INSECURE_DESERIALIZATION"));
        assertTrue(hasCommand);
        assertTrue(hasDeserialization);

        String summary = result.formattedSummary();
        assertTrue(summary.contains("COMMAND_INJECTION"));
        assertTrue(summary.contains("INSECURE_DESERIALIZATION"));
    }
}
