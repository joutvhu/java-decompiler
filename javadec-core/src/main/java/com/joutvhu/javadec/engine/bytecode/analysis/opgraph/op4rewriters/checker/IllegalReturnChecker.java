package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op4rewriters.checker;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op04StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredScope;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.StructuredStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.Block;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredComment;
import com.joutvhu.javadec.engine.bytecode.analysis.structured.statement.StructuredReturn;
import com.joutvhu.javadec.engine.util.DecompilerComment;
import com.joutvhu.javadec.engine.util.DecompilerComments;

import java.util.List;

public class IllegalReturnChecker implements Op04Checker {
    private boolean found = false;

    @Override
    public StructuredStatement transform(StructuredStatement in, StructuredScope scope) {
        if (found) return in;
        if (in instanceof Block) {
            List<Op04StructuredStatement> stms = ((Block) in).getBlockStatements();
            StructuredStatement last = null;
            for (int x=0, len=stms.size();x<len;++x) {
                Op04StructuredStatement statement = stms.get(x);
                StructuredStatement stm = statement.getStatement();
                if (stm instanceof StructuredReturn) {
                    if (last == null) {
                        last = stm;
                    } else {
                        // this is a bit of a cheat, but if we do come across this, we
                        // shouldn't let it break us!
                        if (last.equals(stm)) {
                            statement.nopOut();
                            continue;
                        }
                        found = true;
                        return in;
                    }
                } else if (stm instanceof StructuredComment) {
                    // ignore
                } else {
                    last = null;
                }
            }
        }

        in.transformStructuredChildren(this, scope);
        return in;
    }

    @Override
    public void commentInto(DecompilerComments comments) {
        if (found) {
            comments.addComment(DecompilerComment.NEIGHBOUR_RETURN);
        }
    }
}
