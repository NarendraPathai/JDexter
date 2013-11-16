package org.jdexter.reader;


public abstract class Reader {
	public abstract Object read(Class<?> classToRead) throws Throwable;
}
