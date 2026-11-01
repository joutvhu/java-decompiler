package com.joutvhu.javadec.core;

import com.joutvhu.javadec.core.api.DecompileOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DecompileOptionsTest {

    @Test
    public void testDefaults() {
        DecompileOptions opts = DecompileOptions.defaults();
        assertFalse(opts.showLineNumbers());
        assertFalse(opts.includeSyntheticMembers());
        assertTrue(opts.hideDefaultConstructors());
        assertEquals("auto", opts.preferredEngine());
        assertNull(opts.lineOffset());
        assertNull(opts.lineLimit());
    }

    @Test
    public void testBuilder() {
        DecompileOptions opts = DecompileOptions.builder()
                .showLineNumbers(true)
                .includeSyntheticMembers(true)
                .hideDefaultConstructors(false)
                .preferredEngine("native")
                .lineOffset(10)
                .lineLimit(25)
                .build();

        assertTrue(opts.showLineNumbers());
        assertTrue(opts.includeSyntheticMembers());
        assertFalse(opts.hideDefaultConstructors());
        assertEquals("native", opts.preferredEngine());
        assertEquals(10, opts.lineOffset());
        assertEquals(25, opts.lineLimit());
    }
}
