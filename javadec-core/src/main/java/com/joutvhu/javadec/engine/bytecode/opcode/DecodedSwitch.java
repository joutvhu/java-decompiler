package com.joutvhu.javadec.engine.bytecode.opcode;

import java.util.List;

public interface DecodedSwitch {

    List<DecodedSwitchEntry> getJumpTargets();
}
