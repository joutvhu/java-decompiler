package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.ClassMetricsResult;
import com.joutvhu.javadec.core.model.MethodMetricsItem;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts JVM technical metrics (instruction counts, maxStack, maxLocals, try-catch blocks) from bytecode.
 */
public class BytecodeMetricsExtractor {

    public static ClassMetricsResult extract(byte[] bytecode, String filterMethod) {
        if (bytecode == null || bytecode.length < 8) {
            return ClassMetricsResult.of("unknown", 0, List.of());
        }

        byte[] compatible = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatible = bytecode.clone();
            compatible[6] = 0;
            compatible[7] = 65;
        }

        final String[] classHolder = new String[]{"unknown"};
        final int[] fieldCount = new int[]{0};
        final List<MethodMetricsItem> items = new ArrayList<>();
        final String cleanMethod = (filterMethod != null && !filterMethod.isBlank())
                ? (filterMethod.contains("(") ? filterMethod.substring(0, filterMethod.indexOf('(')).trim() : filterMethod.trim())
                : null;

        ClassReader reader = new ClassReader(compatible);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classHolder[0] = name;
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                fieldCount[0]++;
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
                if (cleanMethod != null && !cleanMethod.equals(methodName)) {
                    return null;
                }

                return new MethodVisitor(Opcodes.ASM9) {
                    private int instructionCount = 0;
                    private int tryCatchCount = 0;
                    private boolean maxsVisited = false;

                    @Override
                    public void visitInsn(int opcode) { instructionCount++; }
                    @Override
                    public void visitIntInsn(int opcode, int operand) { instructionCount++; }
                    @Override
                    public void visitVarInsn(int opcode, int varIndex) { instructionCount++; }
                    @Override
                    public void visitTypeInsn(int opcode, String type) { instructionCount++; }
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) { instructionCount++; }
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) { instructionCount++; }
                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) { instructionCount++; }
                    @Override
                    public void visitJumpInsn(int opcode, Label label) { instructionCount++; }
                    @Override
                    public void visitLdcInsn(Object value) { instructionCount++; }
                    @Override
                    public void visitIincInsn(int varIndex, int increment) { instructionCount++; }
                    @Override
                    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) { instructionCount++; }
                    @Override
                    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) { instructionCount++; }
                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) { instructionCount++; }

                    @Override
                    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                        tryCatchCount++;
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        maxsVisited = true;
                        boolean isLarge = instructionCount > 2000;
                        items.add(new MethodMetricsItem(methodName, methodDesc, instructionCount, maxStack, maxLocals, tryCatchCount, isLarge));
                    }

                    @Override
                    public void visitEnd() {
                        if (!maxsVisited) {
                            items.add(new MethodMetricsItem(methodName, methodDesc, 0, 0, 0, 0, false));
                        }
                    }
                };
            }
        }, 0);

        return ClassMetricsResult.of(classHolder[0], fieldCount[0], items);
    }
}
