package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.InstrIndex;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.Statement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.AnonBreakTarget;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.JumpingStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifier;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockIdentifierFactory;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.BlockType;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.JumpType;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.functors.Predicate;

import java.util.List;
import java.util.Set;

public class AnonymousBlocks {
    public static void labelAnonymousBlocks(List<Op03SimpleStatement> statements, BlockIdentifierFactory blockIdentifierFactory) {
        List<Op03SimpleStatement> anonBreaks = Functional.filter(statements, new Predicate<Op03SimpleStatement>() {
            @Override
            public boolean test(Op03SimpleStatement in) {
                Statement statement = in.getStatement();
                if (!(statement instanceof JumpingStatement)) return false;
                JumpType jumpType = ((JumpingStatement) statement).getJumpType();
                return jumpType == JumpType.BREAK_ANONYMOUS;
            }
        });
        if (anonBreaks.isEmpty()) return;

        /*
         * Collect the unique set of targets for the anonymous breaks.
         */
        Set<Op03SimpleStatement> targets = SetFactory.newOrderedSet();
        for (Op03SimpleStatement anonBreak : anonBreaks) {
            JumpingStatement jumpingStatement = (JumpingStatement) anonBreak.getStatement();
            Op03SimpleStatement anonBreakTarget = (Op03SimpleStatement) jumpingStatement.getJumpTarget().getContainer();
            if (anonBreakTarget.getStatement() instanceof AnonBreakTarget) continue;
            targets.add(anonBreakTarget);
        }

        for (Op03SimpleStatement target : targets) {
            BlockIdentifier blockIdentifier = blockIdentifierFactory.getNextBlockIdentifier(BlockType.ANONYMOUS);
            InstrIndex targetIndex = target.getIndex();
            Op03SimpleStatement anonTarget = new Op03SimpleStatement(
                    target.getBlockIdentifiers(), new AnonBreakTarget(blockIdentifier), targetIndex.justBefore());
            List<Op03SimpleStatement> sources = ListFactory.newList(target.getSources());
            for (Op03SimpleStatement source : sources) {
                if (targetIndex.isBackJumpTo(source)) {
                    target.removeSource(source);
                    source.replaceTarget(target, anonTarget);
                    anonTarget.addSource(source);
                }
            }
            target.addSource(anonTarget);
            anonTarget.addTarget(target);
            int pos = statements.indexOf(target);
            statements.add(pos, anonTarget);
        }
    }


}
