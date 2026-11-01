package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.TypeUsageInformation;

public interface DumperFactory {

    Dumper getNewTopLevelDumper(JavaTypeInstance classType, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, IllegalIdentifierDump illegalIdentifierDump);

    // If we support line numbers, we'll be wrapped around the top level dumper.
    Dumper wrapLineNoDumper(Dumper dumper);

    ProgressDumper getProgressDumper();

    SummaryDumper getSummaryDumper();

    ExceptionDumper getExceptionDumper();

    DumperFactory getFactoryWithPrefix(String prefix, int version);
}
