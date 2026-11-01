package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.types.ClassSignature;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.AccessFlag;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.util.CannotLoadClassException;
import com.joutvhu.javadec.engine.util.DecompilerComment;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;

import java.util.Set;

/*
 * non-sealed is an annoying beast - we need to check if any of our base/interfaces are sealed.
 * If they are, and we are NOT final or sealed, we are non-sealed, however this isn't explicit in
 * the classfile.
 */
public class SealedClassChecker {
    private static boolean isSealed(JavaTypeInstance t, DCCommonState state) {
        try {
            ClassFile i = state.getClassFile(t);
            return i.getAccessFlags().contains(AccessFlag.ACC_FAKE_SEALED);
        } catch (CannotLoadClassException e) {
            return false;
        }
    }

    private static boolean anySealed(ClassSignature sig, DCCommonState state) {
        if (isSealed(sig.getSuperClass(), state)) return true;
        for (JavaTypeInstance t : sig.getInterfaces()) {
            if (isSealed(t, state)) return true;
        }
        return false;
    }

    public static void rewrite(ClassFile classFile, DCCommonState state) {
        Set<AccessFlag> accessFlags = classFile.getAccessFlags();
        if (accessFlags.contains(AccessFlag.ACC_FAKE_SEALED)) {
            markExperimental(classFile, state);
            return;
        }
        if (accessFlags.contains(AccessFlag.ACC_FINAL)) {
            return;
        }
        if (anySealed(classFile.getClassSignature(), state)) {
            markExperimental(classFile, state);
            accessFlags.add(AccessFlag.ACC_FAKE_NON_SEALED);
        }
    }

    public static void markExperimental(ClassFile classFile, DCCommonState state) {
        if (!state.getOptions().optionIsSet(OptionsImpl.SEALED) &&
                OptionsImpl.sealedExpressionVersion.isExperimentalIn(classFile.getClassFileVersion())) {
            classFile.addComment(DecompilerComment.EXPERIMENTAL_FEATURE);
        }
    }
}
