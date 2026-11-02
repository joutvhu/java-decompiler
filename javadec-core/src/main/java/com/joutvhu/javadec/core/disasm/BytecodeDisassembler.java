package com.joutvhu.javadec.core.disasm;

import com.joutvhu.javadec.core.loader.ZipBytecodeLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Ultra-fast JVM bytecode disassembler powered by ASM Textifier.
 * Outputs readable JVM opcodes (iload, invokevirtual, monitorenter, tableswitch, labels, line numbers)
 * for entire classes or selective methods.
 */
public class BytecodeDisassembler {

    public static String disassemble(byte[] bytecode, String methodName, Integer lineOffset, Integer lineLimit) {
        if (bytecode == null || bytecode.length < 8) {
            return "// Error: Empty or invalid bytecode.";
        }

        // Java 22-27+ ASM reader version patching
        byte[] compatibleBytecode = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatibleBytecode = bytecode.clone();
            compatibleBytecode[6] = 0;
            compatibleBytecode[7] = 65;
        }

        ClassReader reader = new ClassReader(compatibleBytecode);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        if (methodName == null || methodName.isBlank()) {
            // Disassemble full class
            TraceClassVisitor tracer = new TraceClassVisitor(pw);
            reader.accept(tracer, ClassReader.EXPAND_FRAMES);
        } else {
            // Disassemble specific method or overloads
            String cleanMethod = methodName.trim();
            if (cleanMethod.contains("(")) {
                cleanMethod = cleanMethod.substring(0, cleanMethod.indexOf('(')).trim();
            }
            final String targetName = cleanMethod;
            final boolean[] matched = {false};

            Textifier textifier = new Textifier();
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(targetName) || (targetName.equals("<init>") && name.equals("<init>"))) {
                        matched[0] = true;
                        Textifier methodTextifier = textifier.visitMethod(access, name, descriptor, signature, exceptions);
                        return new TraceMethodVisitor(methodTextifier);
                    }
                    return null;
                }
            };
            reader.accept(cv, ClassReader.EXPAND_FRAMES);
            textifier.print(pw);

            if (!matched[0]) {
                return "// Method '" + methodName + "' not found in class bytecode.";
            }
        }

        pw.flush();
        String output = sw.toString().trim();
        return ZipBytecodeLoader.paginateLines(output, lineOffset, lineLimit);
    }
}
