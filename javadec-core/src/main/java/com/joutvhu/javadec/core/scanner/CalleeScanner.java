package com.joutvhu.javadec.core.scanner;

import com.joutvhu.javadec.core.model.CalleeItem;
import com.joutvhu.javadec.core.model.CalleeResult;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Ultra-fast scanner to extract all outgoing method calls (callees) from a class or specific method.
 */
public class CalleeScanner {

    public static CalleeResult scan(byte[] bytecode, String filterMethod, Integer limit) {
        if (bytecode == null || bytecode.length < 8) {
            return CalleeResult.of("unknown", filterMethod, List.of());
        }

        byte[] compatible = bytecode;
        int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
        if (majorVersion > 65) {
            compatible = bytecode.clone();
            compatible[6] = 0;
            compatible[7] = 65;
        }

        int max = (limit != null && limit > 0) ? limit : 200;
        List<CalleeItem> items = new ArrayList<>();
        final String cleanMethod = (filterMethod != null && !filterMethod.isBlank())
                ? (filterMethod.contains("(") ? filterMethod.substring(0, filterMethod.indexOf('(')).trim() : filterMethod.trim())
                : null;

        final String[] classHolder = new String[]{"unknown"};

        ClassReader reader = new ClassReader(compatible);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classHolder[0] = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
                if (items.size() >= max) return null;
                if (cleanMethod != null && !cleanMethod.equals(methodName)) {
                    return null;
                }

                return new MethodVisitor(Opcodes.ASM9) {
                    private Integer currentLine;

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        this.currentLine = line;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (items.size() >= max) return;
                        items.add(new CalleeItem(methodName, owner, name, descriptor, currentLine, false));
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
                        if (items.size() >= max) return;
                        if (bsmArgs != null) {
                            for (Object arg : bsmArgs) {
                                if (arg instanceof Handle h) {
                                    items.add(new CalleeItem(methodName, h.getOwner(), h.getName(), h.getDesc(), currentLine, true));
                                }
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);

        return CalleeResult.of(classHolder[0], filterMethod, items);
    }
}
