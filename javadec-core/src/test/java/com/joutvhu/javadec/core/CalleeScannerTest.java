package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.CalleeResult;
import com.joutvhu.javadec.core.scanner.CalleeScanner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CalleeScannerTest {

    @Test
    public void testExtractOutgoingCallees() {
        String src = """
                package com.example;
                import java.util.List;
                public class OrderService {
                    public void checkout(String user, int amount) {
                        validate(user);
                        System.out.println("Processing: " + amount);
                    }
                    private void validate(String user) {
                        if (user == null) throw new IllegalArgumentException();
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.OrderService", src);
        byte[] bytes = classes.get("com.example.OrderService");
        assertNotNull(bytes);

        // 1. Scan specific method
        CalleeResult result = CalleeScanner.scan(bytes, "checkout", 50);
        assertNotNull(result);
        assertTrue(result.totalFound() >= 2);
        boolean callsValidate = result.callees().stream().anyMatch(c -> c.targetName().equals("validate"));
        boolean callsPrintln = result.callees().stream().anyMatch(c -> c.targetName().equals("println"));
        assertTrue(callsValidate);
        assertTrue(callsPrintln);

        String summary = result.formattedSummary();
        assertTrue(summary.contains("OrderService.checkout"));
        assertTrue(summary.contains("validate"));
    }
}
