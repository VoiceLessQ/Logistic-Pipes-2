package com.Morph.logisticspipes;

public final class LPConstants {
    public static final String MOD_ID = "morph";

    // Pipe geometry — 4/16 to 12/16 of a block
    public static final double PIPE_MIN_POS = 0.25;
    public static final double PIPE_MAX_POS = 0.75;

    // In Block.box() units (0-16)
    public static final double PIPE_MIN = PIPE_MIN_POS * 16; // 4
    public static final double PIPE_MAX = PIPE_MAX_POS * 16; // 12
}
