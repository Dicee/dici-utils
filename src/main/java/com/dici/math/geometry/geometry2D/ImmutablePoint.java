package com.dici.math.geometry.geometry2D;

public class ImmutablePoint {
    public final int x, y;

    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public ImmutablePoint moveX(int dx)      { return new ImmutablePoint(x + dx, y     ); }
    public ImmutablePoint moveY(int dy)      { return new ImmutablePoint(x     , y + dy); }
    public ImmutablePoint move (Delta delta) { return moveX(delta.dx).moveY(delta.dy)   ; }
    
    @Override public String toString() { return String.format("(%d, %d)", x, y); }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImmutablePoint that = (ImmutablePoint) obj;
        return x == that.x && y == that.y;
    }
}