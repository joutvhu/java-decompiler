package com.joutvhu.javadec.engine.bytecode.analysis.opgraph.op3rewriters;

import com.joutvhu.javadec.engine.bytecode.AnonymousClassUsage;
import com.joutvhu.javadec.engine.bytecode.analysis.opgraph.Op03SimpleStatement;
import com.joutvhu.javadec.engine.bytecode.analysis.parse.utils.CreationCollector;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.state.DCCommonState;

import java.util.List;

class CondenseConstruction {
    /*
     * Find all the constructors and initialisers.  If something is initialised and
     * constructed in one place each, we can guarantee that the construction happened
     * after the initialisation, so replace
     *
     * a1 = new foo
     * a1.<init>(x, y, z)
     *
     * with
     *
     * a1 = new foo(x,y,z)
     */
    static void condenseConstruction(DCCommonState state, Method method, List<Op03SimpleStatement> statements, AnonymousClassUsage anonymousClassUsage) {
        CreationCollector creationCollector = new CreationCollector(anonymousClassUsage);
        for (Op03SimpleStatement statement : statements) {
            statement.findCreation(creationCollector);
        }
        creationCollector.condenseConstructions(method, state);
    }

}
