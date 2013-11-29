package org.jdexter.annotation.processor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.jdexter.reader.Reader;

public interface MetaDataCollector {

	public Class<? extends Reader> getReader();

	public Set<Field> getDependencies();

	public Set<Field> getOptionalDependencies();

	public Method getDecisionMethod();

	public Set<Field> getInnerConfigurations();

	public Set<Field> getConditionalConfigurations();

	public Set<Field> getDependenciesForConditionalConfiguration(Field field);

}