package com.joutvhu.javadec.engine.bytecode.analysis.variables;

import com.joutvhu.javadec.engine.bytecode.BytecodeMeta;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.LocalVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.bytecode.analysis.types.MethodPrototype;
import com.joutvhu.javadec.engine.bytecode.analysis.types.RawJavaType;
import com.joutvhu.javadec.engine.bytecode.analysis.types.discovery.InferredJavaType;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.collections.MapFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VariableFactory {
    private final VariableNamer variableNamer;
    private final Map<Integer, InferredJavaType> typedArgs;
    private final Set<Integer> clashes;
    private final Method method;
    private int ignored;

    private final Map<LValue, LValue> cache = MapFactory.newMap();

    public VariableFactory(Method method, BytecodeMeta bytecodeMeta) {
        this.variableNamer = method.getVariableNamer();
        this.clashes = bytecodeMeta.getLivenessClashes();
        MethodPrototype methodPrototype = method.getMethodPrototype();
        List<JavaTypeInstance> args = methodPrototype.getArgs();
        this.typedArgs = MapFactory.newMap();
        int offset = 0;
        if (methodPrototype.isInstanceMethod()) {
            JavaTypeInstance thisType = method.getClassFile().getClassType();
            typedArgs.put(offset++, new InferredJavaType(thisType, InferredJavaType.Source.UNKNOWN, true));
        }
        for (JavaTypeInstance arg : args) {
            typedArgs.put(offset, new InferredJavaType(arg, InferredJavaType.Source.UNKNOWN, true));
            offset += arg.getStackType().getComputationCategory();
        }
        if (methodPrototype.parametersComputed()) {
            for (LocalVariable localVariable : methodPrototype.getComputedParameters()) {
                cache.put(localVariable, localVariable);
            }
        }
        this.method = method;
    }

    public JavaTypeInstance getReturn() {
        return method.getMethodPrototype().getReturnType();
    }

    public LValue ignoredVariable(InferredJavaType type) {
        LocalVariable res = new LocalVariable("cfr_ignored_" + ignored++, type);
        res.markIgnored();
        return res;
    }

    public LValue tempVariable(InferredJavaType type) {
        return new LocalVariable("cfr_temp_" + ignored++, type);
    }

    /*
     * NB: idx is slot, i.e. offset.
     */
    public LValue localVariable(int stackPosition, Ident ident, int origCodeRawOffset) {
        if (ident == null) {
            // This isn't right, and indicates a failure, possibly in synthetic argument deduction.
            // however, we can recover from here.
            ident = new Ident(stackPosition, -1);
        }
        InferredJavaType varType = ident.getIdx() == 0 ? typedArgs.get(stackPosition) : null;
        if (varType == null) {
            varType = new InferredJavaType(RawJavaType.VOID, InferredJavaType.Source.UNKNOWN);
        }

        LValue tmp = new LocalVariable(stackPosition, ident, variableNamer, origCodeRawOffset,
                clashes.contains(stackPosition)
                // TODO : This is a hack disabling explicit clashes, to avoid breaking Tower. (see tests)
                // However, it will need to be fixed to deal with bug #87
                && typedArgs.get(stackPosition) == null,
                varType);
        LValue val = cache.get(tmp);
        if (val == null) {
            cache.put(tmp, tmp);
            val = tmp;
        }
        return val;
    }

    public void mutatingRenameUnClash(LocalVariable toRename) {
        variableNamer.mutatingRenameUnClash(toRename.getName());
    }
}
