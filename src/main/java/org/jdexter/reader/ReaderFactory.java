package org.jdexter.reader;

import java.lang.reflect.InvocationTargetException;

import org.jdexter.reader.exception.ReaderInstantiationException;
import org.jdexter.util.ReflectionUtil;

/**
 * This is a non-caching reader factory, where on each invocation of {@link #getInstanceOf(Class)} a fresh instance
 * is created for the reader.
 * @author Narendra
 *
 */
public class ReaderFactory {
	public Reader getInstanceOf(Class<? extends Reader> readerClass) throws ReaderInstantiationException{
		try{	
			return ReflectionUtil.createDefaultInstance(readerClass);
		}catch (InvocationTargetException e) {
			throw new ReaderInstantiationException(e.getTargetException());
		}catch (Throwable e) {
			throw new ReaderInstantiationException(e);
		}
	}
}
