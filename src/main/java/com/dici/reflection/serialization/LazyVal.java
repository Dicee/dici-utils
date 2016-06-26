package com.dici.reflection.serialization;

import com.dici.check.Check;

import java.io.Serializable;

public class LazyVal<T> implements Serializable {
	private static final long	serialVersionUID	= 1L;

	private final SerializableFactory<T> factory;
    private transient T instance = null;

    public LazyVal(SerializableFactory<T> factory) { this.factory = Check.notNull(factory); }

    public T instance() {
        if (instance == null) instance = factory.create();
        return instance;
    }
}

