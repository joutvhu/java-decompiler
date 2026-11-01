package com.joutvhu.javadec.engine;

import com.joutvhu.javadec.engine.api.ClassFileSource;
import com.joutvhu.javadec.engine.apiunreleased.ClassFileSource2;
import com.joutvhu.javadec.engine.bytecode.analysis.types.JavaTypeInstance;
import com.joutvhu.javadec.engine.state.ClassFileSourceImpl;
import com.joutvhu.javadec.engine.state.ClassFileSourceWrapper;
import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.state.TypeUsageInformation;
import com.joutvhu.javadec.engine.util.AnalysisType;
import com.joutvhu.javadec.engine.util.collections.Functional;
import com.joutvhu.javadec.engine.util.collections.MapFactory;
import com.joutvhu.javadec.engine.util.functors.UnaryFunction;
import com.joutvhu.javadec.engine.util.getopt.Options;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;
import com.joutvhu.javadec.engine.util.output.*;
import com.joutvhu.javadec.engine.util.output.MethodErrorCollector.SummaryDumperMethodErrorCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused"})
@Deprecated
public class PluginRunner {
    private final DCCommonState dcCommonState;

    public PluginRunner() {
        this(MapFactory.<String, String>newMap(), null);
    }

    public PluginRunner(Map<String, String> options) {
        this(options, null);
    }

    public PluginRunner(Map<String, String> options, ClassFileSource classFileSource) {
        this.dcCommonState = initDCState(options, classFileSource);
    }

    public Options getOptions() {
        return this.dcCommonState.getOptions();
    }

    public List<List<String>> addJarPaths(String[] jarPaths) {
        List<List<String>> res = new ArrayList<List<String>>();
        for (String jarPath : jarPaths) {
            res.add(addJarPath(jarPath));
        }
        return res;
    }

    public List<String> addJarPath(String jarPath) {
        try {
            List<JavaTypeInstance> types = dcCommonState.explicitlyLoadJar(jarPath, AnalysisType.JAR).get(0);
            return Functional.map(types, new UnaryFunction<JavaTypeInstance, String>() {
                @Override
                public String invoke(JavaTypeInstance arg) {
                    return arg.getRawName();
                }
            });
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private class PluginDumperFactory implements DumperFactory {
        private final IllegalIdentifierDump illegalIdentifierDump = new IllegalIdentifierDump.Nop();

        private final StringBuilder outBuffer;
        private final Options options;

        public PluginDumperFactory(StringBuilder out, Options options) {
            this.outBuffer = out;
            this.options = options;
        }

        public Dumper getNewTopLevelDumper(JavaTypeInstance classType, SummaryDumper summaryDumper, TypeUsageInformation typeUsageInformation, IllegalIdentifierDump illegalIdentifierDump) {
            return new StringStreamDumper(new SummaryDumperMethodErrorCollector(classType, summaryDumper), outBuffer, typeUsageInformation, options, this.illegalIdentifierDump);
        }

        @Override
        public Dumper wrapLineNoDumper(Dumper dumper) {
            return dumper;
        }

        /*
         * A summary dumper will receive errors.  Generally, it's only of value when dumping jars to file.
         */
        public SummaryDumper getSummaryDumper() {
            if (!options.optionIsSet(OptionsImpl.OUTPUT_DIR)) return new NopSummaryDumper();

            return new FileSummaryDumper(options.getOption(OptionsImpl.OUTPUT_DIR), options, null);
        }

        @Override
        public ProgressDumper getProgressDumper() {
            return ProgressDumperNop.INSTANCE;
        }

        @Override
        public ExceptionDumper getExceptionDumper() {
            return new StdErrExceptionDumper();
        }

        @Override
        public DumperFactory getFactoryWithPrefix(String prefix, int version) {
            return this;
        }
    }

    public String getDecompilationFor(String classFilePath) {
        try {
            StringBuilder output = new StringBuilder();
            DumperFactory dumperFactory = new PluginDumperFactory(output, dcCommonState.getOptions());
            Main.doClass(dcCommonState, classFilePath, false, dumperFactory);
            return output.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private static DCCommonState initDCState(Map<String, String> optionsMap, ClassFileSource classFileSource) {
        OptionsImpl options = new OptionsImpl(optionsMap);
        ClassFileSource2 source = classFileSource == null ?
                new ClassFileSourceImpl(options) :
                new ClassFileSourceWrapper(classFileSource);
        return new DCCommonState(options, source);
    }

}
