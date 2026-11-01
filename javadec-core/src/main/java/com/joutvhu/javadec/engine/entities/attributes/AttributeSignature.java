package com.joutvhu.javadec.engine.entities.attributes;

import com.joutvhu.javadec.engine.entities.constantpool.ConstantPool;
import com.joutvhu.javadec.engine.entities.constantpool.ConstantPoolEntryUTF8;
import com.joutvhu.javadec.engine.util.bytestream.ByteData;
import com.joutvhu.javadec.engine.util.output.Dumper;

/*
 * See 4.4.4 in Java class file format.
 *
 * ClassSignature:
 *   FormalTypeParametersopt SuperclassSignature SuperinterfaceSignature*
 *
 * FormalTypeParameters:
 *   <FormalTypeParameter+>
 *
 * FormalTypeParameter:
 *   Identifier ClassBound InterfaceBound*
 *
 * ClassBound:
 *  : FieldTypeSignatureopt
 *
 * InterfaceBound:
 *  : FieldTypeSignature
 *
 * SuperclassSignature:
 *   ClassTypeSignature
 *
 * SuperinterfaceSignature:
 *   ClassTypeSignature
 *
 * FieldTypeSignature:
 *   ClassTypeSignature
 *   ArrayTypeSignature
 *   TypeVariableSignature
 */
public class AttributeSignature extends Attribute {
    public static final String ATTRIBUTE_NAME = "Signature";

    private final ConstantPoolEntryUTF8 signature;

    public AttributeSignature(ByteData raw, ConstantPool cp) {
        this.signature = cp.getUTF8Entry(raw.getU2At(0));
    }

    @Override
    public String getRawName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public Dumper dump(Dumper d) {
        return d.print("Signature : " + signature);
    }

    public ConstantPoolEntryUTF8 getSignature() {
        return signature;
    }
}
