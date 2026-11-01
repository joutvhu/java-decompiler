package com.joutvhu.javadec.engine.bytecode;

import com.joutvhu.javadec.engine.state.DCCommonState;
import com.joutvhu.javadec.engine.util.DecompilerComment;
import com.joutvhu.javadec.engine.util.collections.ListFactory;
import com.joutvhu.javadec.engine.util.getopt.MutableOptions;
import com.joutvhu.javadec.engine.util.getopt.Options;

import java.util.List;

public class RecoveryOptions {
    private final List<RecoveryOption<?>> recoveryOptions;

    public RecoveryOptions(RecoveryOption<?>... recoveryOptions) {
        this.recoveryOptions = ListFactory.newImmutableList(recoveryOptions);
    }

    public RecoveryOptions(RecoveryOptions prev, RecoveryOption<?>... recoveryOptions) {
        List<RecoveryOption<?>> recoveryOptionList = ListFactory.newImmutableList(recoveryOptions);
        this.recoveryOptions = ListFactory.newList();
        this.recoveryOptions.addAll(prev.recoveryOptions);
        this.recoveryOptions.addAll(recoveryOptionList);
    }

    public static class Applied {
        public Options options;
        public List<DecompilerComment> comments;
        public boolean valid;

        public Applied(Options options, List<DecompilerComment> comments, boolean valid) {
            this.options = options;
            this.comments = comments;
            this.valid = valid;
        }
    }

    public Applied apply(DCCommonState commonState, Options originalOptions, BytecodeMeta bytecodeMeta) {
        MutableOptions mutableOptions = new MutableOptions(originalOptions);
        List<DecompilerComment> appliedComments = ListFactory.newList();
        boolean hadEffect = false;
        for (RecoveryOption<?> option : recoveryOptions) {
            if (option.apply(mutableOptions, appliedComments, bytecodeMeta)) hadEffect = true;
        }
        return new Applied(mutableOptions, appliedComments, hadEffect);
    }
}
