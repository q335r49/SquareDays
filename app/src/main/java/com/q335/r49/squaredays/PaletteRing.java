package com.q335.r49.squaredays;

import android.graphics.Color;
import android.util.Log;

class PaletteRing {
    private int length;
    private int size;
    private int[] ring;
    private int pos;
    PaletteRing(int length) {
        this.length = length;
        ring = new int[length];
        pos = 0;
        size = 0;
    }
    public void add(int c) {
        for (int i = 0; i < length; i++) {
            if (ring[(pos + i) % length] ==  c) {
                if (i != 0) {
                    for (int j = pos + i; j > pos; j--)
                        ring[j % length] = ring[(j - 1) % length];
                    size = size > 0 ? size - 1 : 0;
                }
                break;
            }
        }
        ring[pos] = c;
        pos = (pos + 1) % length;
        size++;
    }
    public void add(String[] colors) {
        for (String c:colors) {
            try {
                add(Color.parseColor(c));
            } catch (Exception e) { Log.d("SquareDays","Bad color: " + c); }
        }
    }
    int get(int i) {
        return ring[(pos - 1 - i + 10*length) % length];
    }
}