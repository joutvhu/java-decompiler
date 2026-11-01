package com.joutvhu.javadec.engine.entities.classfilehelpers;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.NewAnonymousArray;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue.StaticVariable;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredComment;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredReturn;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.placeholder.BeginBlock;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.placeholder.EndBlock;
import com.joutvhu.javadec.engine.entities.ClassFile;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.util.CannotLoadClassException;
import com.joutvhu.javadec.engine.util.ConfusedCFRException;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.functors.Predicate;

import java.util.List;
import java.util.Map;

public class SwitchClassHelper {
    public static Map<Integer, StaticVariable> getSwitchEntriesByOrdinal(DCCommonState dcCommonState, Expression lvalue) {
        ClassFile enumClass;
        try {
            enumClass = dcCommonState.getClassFile(lvalue.getInferredJavaType().getJavaTypeInstance());
        } catch (CannotLoadClassException e) {
            // Oh dear, can't load that class.  Proceed without it.
            return null;
        }
        Method values = enumClass.getSingleMethodByNameOrNull("$values");
        if (values == null) {
            return null;
        }
        Op04StructuredStatement valuesData = null;
        try {
            valuesData = values.getAnalysis();
        } catch (ConfusedCFRException e) {
            return null;
        }

        List<StructuredStatement> decls = ListFactory.newList();
        valuesData.linearizeStatementsInto(decls);
        decls = Functional.filter(decls, new Predicate<StructuredStatement>() {
            @Override
            public boolean test(StructuredStatement in) {
                return !(in instanceof StructuredComment || in instanceof BeginBlock || in instanceof EndBlock) ;
            }
        });

        if (decls.size() != 1) return null;

        StructuredStatement decl = decls.get(0);
        if (!(decl instanceof StructuredReturn)) return null;

        Expression value = ((StructuredReturn) decl).getValue();
        if (!(value instanceof NewAnonymousArray)) return null;

        List<Expression> enumValues = ((NewAnonymousArray) value).getValues();

        int idx = 0;
        Map<Integer, StaticVariable> ordinals = MapFactory.newMap();
        for (Expression e : enumValues) {
            if (!(e instanceof LValueExpression)) return null;
            LValue lv = ((LValueExpression) e).getLValue();
            if (!(lv instanceof StaticVariable)) return null;
            if (!lv.getInferredJavaType().getJavaTypeInstance().equals(lvalue.getInferredJavaType().getJavaTypeInstance()))
                return null;
            ordinals.put(idx++, (StaticVariable) lv);
        }
        return ordinals;
    }
}
