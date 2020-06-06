package com.dici.math.geometry;

import com.dici.math.geometry.geometry3D.Vector3D;

public interface Translatable<T> {
	T translate(Vector3D v);
}
