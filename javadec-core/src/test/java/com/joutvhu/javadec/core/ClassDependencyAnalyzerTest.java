package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.model.DependencyAnalysisResult;
import com.joutvhu.javadec.core.scanner.ClassDependencyAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ClassDependencyAnalyzerTest {

    @Test
    public void testAnalyzeDependencies() {
        String src = """
                package com.example.service;
                import java.io.File;
                import java.util.List;
                import java.util.Map;
                public class ReportService {
                    private final File file = new File(".");
                    public List<String> generate(Map<String, Object> params) {
                        return List.of(file.getName());
                    }
                }
                """;

        Map<String, byte[]> classes = TestCompiler.compile("com.example.service.ReportService", src);
        byte[] bytes = classes.get("com.example.service.ReportService");
        assertNotNull(bytes);

        DependencyAnalysisResult result = ClassDependencyAnalyzer.analyze(bytes);
        assertNotNull(result);

        assertTrue(result.jdkDependencies().contains("java.io.File"));
        assertTrue(result.jdkDependencies().contains("java.util.List"));
        assertTrue(result.jdkDependencies().contains("java.util.Map"));

        String report = result.formattedReport();
        assertTrue(report.contains("JDK Standard"));
        assertTrue(report.contains("java.io.File"));
    }
}
