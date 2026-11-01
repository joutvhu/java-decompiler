package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.analysis.loc.BytecodeLoc;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.GotoStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.statement.ReturnNothingStatement;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.util.MiscConstants;
import com.joutvhu.javadec.engine.util.getopt.Options;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;

import java.util.List;

/*
 * We shouldn't have return statements in static initializers.
 */
public class StaticInitReturnRewriter {
    public static List<Op03SimpleStatement> rewrite(Options options, Method method, List<Op03SimpleStatement> statementList) {
        if (!method.getName().equals(MiscConstants.STATIC_INIT_METHOD)) return statementList;
        if (!options.getOption(OptionsImpl.STATIC_INIT_RETURN)) return statementList;
        /*
         * if the final statement is a return, then replace all other returns with a jump to that.
         */
        Op03SimpleStatement last = statementList.get(statementList.size()-1);
        if (last.getStatement().getClass() != ReturnNothingStatement.class) return statementList;
        for (int x =0, len=statementList.size()-1;x<len;++x) {
            Op03SimpleStatement stm = statementList.get(x);
            if (stm.getStatement().getClass() == ReturnNothingStatement.class) {
                stm.replaceStatement(new GotoStatement(BytecodeLoc.TODO));
                stm.addTarget(last);
                last.addSource(stm);
            }
        }
        return statementList;
    }
}
