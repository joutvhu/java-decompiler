package com.joutvhu.javadec.engine;

import com.joutvhu.javadec.engine.api.CfrDriver;
import com.joutvhu.javadec.engine.api.ClassFileSource;
import com.joutvhu.javadec.engine.api.OutputSinkFactory;
import com.joutvhu.javadec.engine.apiunreleased.ClassFileSource2;
import com.joutvhu.javadec.engine.state.ClassFileSourceChained;
import com.joutvhu.javadec.engine.state.ClassFileSourceImpl;
import com.joutvhu.javadec.engine.state.ClassFileSourceWrapper;
import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.util.AnalysisType;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.getopt.Options;
import com.joutvhu.javadec.engine.util.getopt.OptionsImpl;
import com.joutvhu.javadec.engine.util.output.DumperFactory;
import com.joutvhu.javadec.engine.util.output.InternalDumperFactoryImpl;
import com.joutvhu.javadec.engine.util.output.SinkDumperFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CfrDriverImpl implements CfrDriver {
    private final Options options;
    private final ClassFileSource2 classFileSource;
    private final OutputSinkFactory outputSinkFactory;

    public CfrDriverImpl(ClassFileSource source, OutputSinkFactory outputSinkFactory, Options options, boolean fallbackToDefaultSource) {
        if (options == null) {
            options = new OptionsImpl(new HashMap<String, String>());
        }
        ClassFileSource2 tmpSource;
        if (source == null) {
            tmpSource = new ClassFileSourceImpl(options);
        } else {
            tmpSource = source instanceof ClassFileSource2 ? (ClassFileSource2)source : new ClassFileSourceWrapper(source);
            if (fallbackToDefaultSource) {
                tmpSource = new ClassFileSourceChained(Arrays.asList(tmpSource, new ClassFileSourceImpl(options)));
            }
        }
        this.outputSinkFactory = outputSinkFactory;
        this.options = options;
        this.classFileSource = tmpSource;
    }

    @Override
    public void analyse(List<String> toAnalyse) {
        /*
         * There's an interesting question here - do we want to skip inner classes, if we've been given a wildcard?
         * (or a wildcard expanded by the operating system).
         *
         * Assume yes.
         */
        boolean skipInnerClass = toAnalyse.size() > 1 && options.getOption(OptionsImpl.SKIP_BATCH_INNER_CLASSES);

        // Can't sort a 1.6 singleton list.
        toAnalyse = ListFactory.newList(toAnalyse);
        Collections.sort(toAnalyse);
        for (String path : toAnalyse) {
            // TODO : We shouldn't have to discard state here.  But we do, because
            // it causes test fails.  (used class name table retains useful symbols).
            classFileSource.informAnalysisRelativePathDetail(null, null);
            // Note - both of these need to be reset, as they have caches.
            DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
            DumperFactory dumperFactory = outputSinkFactory != null ?
                    new SinkDumperFactory(outputSinkFactory, options) :
                    new InternalDumperFactoryImpl(options);

            AnalysisType type = options.getOption(OptionsImpl.ANALYSE_AS);
            if (type == null || type == AnalysisType.DETECT) {
                type = dcCommonState.detectClsJar(path);
            }

            if (type == AnalysisType.JAR || type == AnalysisType.WAR) {
                Driver.doJar(dcCommonState, path, type, dumperFactory);
            } else if (type == AnalysisType.CLASS) {
                Driver.doClass(dcCommonState, path, skipInnerClass, dumperFactory);
            }
        }
    }
}
