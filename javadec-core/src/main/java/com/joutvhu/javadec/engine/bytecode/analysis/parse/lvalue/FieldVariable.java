package com.joutvhu.javadec.engine.bytecode.analysis.parse.lvalue;

import com.joutvhu.javadec.engine.bytecode.analysis.parse.Expression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.LValue;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.StatementContainer;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.LValueExpression;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.expression.misc.Precedence;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.CloneHelper;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueRewriter;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.LValueUsageCollector;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.SSAIdentifiers;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.ClassFileField;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntry;
import com.joutvhu.javadec.engine.entities.exceptions.ExceptionCheck;
import com.joutvhu.javadec.engine.state.TypeUsageCollector;
import com.joutvhu.javadec.engine.util.MiscConstants;
import com.joutvhu.javadec.engine.util.Troolean;
import com.joutvhu.javadec.engine.util.output.Dumper;

/**
 * Note - a field variable LValue means an lValue of ANY object.
 */
public class FieldVariable extends AbstractFieldVariable {

    private Expression object;

    public FieldVariable(Expression object, ConstantPoolEntry field) {
        super(field);
        this.object = object;
    }

    public FieldVariable(Expression object, ClassFileField field, JavaTypeInstance owningClass) {
        super(field, owningClass);
        this.object = object;
    }

    private FieldVariable(FieldVariable other, CloneHelper cloneHelper) {
        super(other);
        this.object = cloneHelper.replaceOrClone(other.object);
    }

    private FieldVariable(FieldVariable other, Expression object) {
        super(other);
        this.object = object;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        super.collectTypeUsages(collector);
        collector.collectFrom(object);
    }

    @Override
    public LValue deepClone(CloneHelper cloneHelper) {
        return new FieldVariable(this, cloneHelper);
    }

    public FieldVariable withReplacedObject(Expression object) {
        return new FieldVariable(this, object);
    }
    /*
     * This will only be meaningful after the inner class constructor transformation.
     */
    private boolean isOuterRef() {
        ClassFileField classFileField = getClassFileField();
        return classFileField != null && classFileField.isSyntheticOuterRef();
    }

    public Expression getObject() {
        return object;
    }

    // Eclipse has a nasty habit of chaining outer accessors, leading to
    // a.this.b.this.c.this.xxx
    private boolean objectIsEclipseOuterThis() {
        if (object instanceof LValueExpression) {
            LValue lValue = ((LValueExpression) object).getLValue();
            if (lValue instanceof FieldVariable) {
                if (((FieldVariable) lValue).getClassFileField().getFieldName().endsWith(MiscConstants.DOT_THIS)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean objectIsThis() {
        if (object instanceof LValueExpression) {
            LValue lValue = ((LValueExpression) object).getLValue();
            if (lValue instanceof LocalVariable) {
                return ((LocalVariable) lValue).getName().getStringName().equals(MiscConstants.THIS);
            }
        }
        return false;
    }

    @Override
    public boolean canThrow(ExceptionCheck caught) {
        if (!super.canThrow(caught))  {
            return false;
        }
        if (objectIsThis()) {
            return false;
        }
        return true;
    }

    private boolean objectIsIllegalThis() {
        if (object instanceof LValueExpression) {
            LValue lValue = ((LValueExpression) object).getLValue();
            if (lValue instanceof FieldVariable) {
                FieldVariable fv = (FieldVariable)lValue;
                return fv.getFieldName().equals(MiscConstants.THIS);
            }
        }
        return false;
    }

    @Override
    public Precedence getPrecedence() {
        return Precedence.PAREN_SUB_MEMBER;
    }

    @Override
    public Dumper dumpInner(Dumper d) {
        if (!(isOuterRef() && (objectIsThis() || objectIsEclipseOuterThis()))) {
            // I'd rather not have this check here, but I don't want to have a pass to get rid of
            // what is actually useful information.
            if (!objectIsIllegalThis()) {
                object.dumpWithOuterPrecedence(d, getPrecedence(), Troolean.NEITHER).separator(".");
            }
        }
        return d.fieldName(getFieldName(), getOwningClassType(), isHiddenDeclaration(), false, false);
    }

    @Override
    public void collectLValueUsage(LValueUsageCollector lValueUsageCollector) {
        object.collectUsedLValues(lValueUsageCollector);
    }

    @Override
    public LValue replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        object = object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        return this;
    }

    @Override
    public LValue applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        object = expressionRewriter.rewriteExpression(object, ssaIdentifiers, statementContainer, flags);
        return this;
    }

    public void rewriteLeftNestedSyntheticOuterRefs() {
        if (isOuterRef()) {
            while (object instanceof LValueExpression) {
                LValue lValueLhs = ((LValueExpression) object).getLValue();
                if (lValueLhs instanceof FieldVariable) {
                    FieldVariable lhs = (FieldVariable) lValueLhs;
                    if (lhs.isOuterRef()) {
                        object = lhs.object;
                        continue;
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof FieldVariable)) return false;
        FieldVariable other = (FieldVariable) o;

        if (!super.equals(o)) return false;
        if (!object.equals(other.object)) return false;
        return true;
    }

    // THIS IS ABSOLUTELY WRONG.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
//        throw new ConfusedCFRException("Mutable object");
//        int hashcode = super.hashCode();
//        if (object != null) hashcode = hashcode * 13 + object.hashCode();
//        return hashcode;
    }

}
