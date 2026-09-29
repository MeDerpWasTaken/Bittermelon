package com.site21.bittermelon.common.content.entities.scp939.lure;

import java.util.List;

public class LureScene {
    private final LureType type;
    private final List<LureDialogue> lines;
    private final LurePool pool;
    private int beat = 0;

    public LureScene(LureType type, List<LureDialogue> lines, LurePool pool) {
        this.type = type;
        this.lines = lines;
        this.pool = pool;
    }

    public LureType type() {
        return type;
    }

    public List<LureDialogue> lines() {
        return lines;
    }

    public LurePool pool() {
        return pool;
    }

    public void incrementBeat() {
        beat++;
    }

    public boolean isFinished() {
        return beat >= pool.beats() || beat >= lines.size();
    }
}
