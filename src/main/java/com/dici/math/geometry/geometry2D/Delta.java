package com.dici.math.geometry.geometry2D;

public class Delta {
    public final int dx, dy;

    public Delta(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }
    
    public Delta times(int length ) { return new Delta(dx * length  , dy * length  ); }
    public Delta plus (Delta delta) { return new Delta(dx + delta.dx, dy + delta.dy); }
    
    @Override public String toString() { return String.format("Delta(%d, %d)", dx, dy);}
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + dx;
        result = prime * result + dy;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Delta that = (Delta) obj;
        return dx == that.dx && dy == that.dy;
    }
}