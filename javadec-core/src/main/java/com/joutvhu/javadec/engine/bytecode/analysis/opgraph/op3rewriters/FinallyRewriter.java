package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.CatchStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.FinallyStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.TryStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.finalhelp.FinalAnalyzer;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.functors.Predicate;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.getopt.Options;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;

import java.util.List;
import java.util.Set;

public class FinallyRewriter {
    public static void identifyFinally(Options options, Method method, List<Op03SimpleStatement> in, BlockIdentifierFactory blockIdentifierFactory) {
        if (!options.getOption(OptionsImpl.DECODE_FINALLY)) return;
        /* Get all the try statements, get their catches.  For all the EXIT points to the catches, try to identify
         * a common block of code (either before a throw, return or goto.)
         * Be careful, if a finally block contains a throw, this will mess up...
         */
        final Set<Op03SimpleStatement> analysedTries = SetFactory.newSet();
        boolean continueLoop;
        do {
            List<Op03SimpleStatement> tryStarts = Functional.filter(in, new Predicate<Op03SimpleStatement>() {
                @Override
                public boolean test(Op03SimpleStatement in) {
                    if (in.getStatement() instanceof TryStatement &&
                            !analysedTries.contains(in)) return true;
                    return false;
                }
            });
            for (Op03SimpleStatement tryS : tryStarts) {
                FinalAnalyzer.identifyFinally(method, tryS, in, blockIdentifierFactory, analysedTries);
            }
            /*
             * We may need to reloop, if analysis has created new tries inside finally handlers. (!).
             */
            continueLoop = (!tryStarts.isEmpty());
        } while (continueLoop);
    }

    static Set<BlockIdentifier> getBlocksAffectedByFinally(List<Op03SimpleStatement> statements) {
        Set<BlockIdentifier> res = SetFactory.newSet();
        for (Op03SimpleStatement stm : statements) {
            if (stm.getStatement() instanceof TryStatement) {
                TryStatement tryStatement = (TryStatement)stm.getStatement();
                Set<BlockIdentifier> newBlocks = SetFactory.newSet();
                boolean found = false;
                newBlocks.add(tryStatement.getBlockIdentifier());
                for (Op03SimpleStatement tgt : stm.getTargets()) {
                    Statement inr = tgt.getStatement();
                    if (inr instanceof CatchStatement) {
                        newBlocks.add(((CatchStatement)inr).getCatchBlockIdent());
                    }
                    if (tgt.getStatement() instanceof FinallyStatement) {
                        found = true;
                    }
                }
                if (found) res.addAll(newBlocks);
            }
        }
        return res;
    }
}
