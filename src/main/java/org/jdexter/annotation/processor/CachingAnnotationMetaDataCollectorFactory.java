package org.jdexter.annotation.processor;

public class CachingAnnotationMetaDataCollectorFactory extends CachingFactory<AnnotationMetaDataCollector>{

	@Override
	protected AnnotationMetaDataCollector createInstance(Class<?> configurationClass) {
		return AnnotationMetaDataCollector.of(configurationClass);
	}
}
