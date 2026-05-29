package com.Morph.logisticspipes;

public final class LPConstants {
    public static final String MOD_ID = "logisticspipes";

    // Pipe geometry — 5/16 to 11/16 of a block (matches LP1 PipeModel_moved.obj)
    public static final double PIPE_MIN_POS = 5.0 / 16.0;
    public static final double PIPE_MAX_POS = 11.0 / 16.0;

    // In Block.box() units (0-16)
    public static final double PIPE_MIN = PIPE_MIN_POS * 16; // 5
    public static final double PIPE_MAX = PIPE_MAX_POS * 16; // 11
}
