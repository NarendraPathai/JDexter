package org.jdexter.reader;

import org.jdexter.util.ReflectionUtil;

public class DefaultReader extends Reader{

	@Override
	public Object read(Class<?> classToRead) throws Exception {
		return ReflectionUtil.createDefaultInstance(classToRead);
	}

}
