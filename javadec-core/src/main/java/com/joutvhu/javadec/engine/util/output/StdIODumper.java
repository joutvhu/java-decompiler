package com.joutvhu.javadec.engine.util.output;

import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.entities.Method;
import com.joutvhu.javadec.engine.state.TypeUsageInformation;
import com.joutvhu.javadec.engine.util.getopt.Options;

import java.io.BufferedOutputStream;
import java.util.Set;

public class StdIODumper extends StreamDumper {
    StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(typeUsageInformation, options, illegalIdentifierDump, context);
    }

    private StdIODumper(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context, Set<JavaTypeInstance> emitted) {
        super(typeUsageInformation, options, illegalIdentifierDump, context, emitted);
    }

    @Override
    protected void write(String s) {
        System.out.print(s);
    }

    @Override
    public void addSummaryError(Method method, String s) {
    }

    @Override
    public void close() {
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new StdIODumper(innerclassTypeUsageInformation, options, illegalIdentifierDump, context, emitted);
    }

    @Override
    public BufferedOutputStream getAdditionalOutputStream(String description) {
        return new BufferedOutputStream(System.out);
    }
}
