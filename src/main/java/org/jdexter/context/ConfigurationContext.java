package org.jdexter.context;

import static org.jdexter.util.ReflectionUtil.injectFieldForcefully;
import static org.jdexter.util.ReflectionUtil.invokeLifeCycleEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jdexter.annotation.PostRead;
import org.jdexter.annotation.processor.CachingAnnotationMetaDataCollectorFactory;
import org.jdexter.annotation.processor.MetaDataCollector;
import org.jdexter.exception.ReadConfigurationException;
import org.jdexter.reader.Reader;
import org.jdexter.reader.ReaderFactory;
import org.jdexter.reader.exception.ReaderInstantiationException;
import org.jdexter.util.ReflectionUtil;

//TODO on a second thought I think it won't be advisable to catch all the throwable.
//TODO Run time exceptions such as NPE and all should not be caught by context and should be allowed to bubble up
public class ConfigurationContext {
	
	private CachingAnnotationMetaDataCollectorFactory collectorFactory;
	private ReaderFactory readerFactory;
	
	private Map<Class<?>,Object> readConfigurations;
	
	public ConfigurationContext() {
		collectorFactory = new CachingAnnotationMetaDataCollectorFactory();
		readerFactory = new ReaderFactory();
		readConfigurations = new ConcurrentHashMap<Class<?>, Object>();
	}
	
	ConfigurationContext(ReaderFactory readerFactory){
		this();
		if(readerFactory == null)
			throw new IllegalArgumentException("Reader Factory cannot be null");
		
		this.readerFactory = readerFactory;
	}
	
	public <T> T read(Class<T> configurationClassToRead) throws ReadConfigurationException{
		try {
			if(configurationClassToRead == null)
				throw new IllegalArgumentException("Class to read cannot be null");

			MetaDataCollector metaDataCollector = collectorFactory.create(configurationClassToRead);

			Object configurationInstance = readerFactory.getInstanceOf(metaDataCollector.getReader()).read(configurationClassToRead);
			
			injectDependencies(configurationInstance, metaDataCollector);
			
			readInnerConfigurations(configurationInstance, metaDataCollector);

			save(configurationInstance);
			
			invokeLifeCycleEvent(configurationInstance, PostRead.class);
			
			return configurationClassToRead.cast(configurationInstance);
		}catch (ReaderInstantiationException e) {
			throw new ReadConfigurationException(e.getCause());
		}catch (InvocationTargetException e) {
			throw new ReadConfigurationException(e.getTargetException());
		}catch (ReadConfigurationException e) {
			throw new ReadConfigurationException(e.getCause());
		}catch(Throwable t){
			throw new ReadConfigurationException(t);
		}
	}

	private void readInnerConfigurations(Object configurationInstance,MetaDataCollector metaDataCollector) throws ReadConfigurationException, IllegalAccessException, InvocationTargetException {
		for(Field field : metaDataCollector.getInnerConfigurations()){
			//expects always a freshly read instance
			Object innerConfiguration = read(field.getType());
			injectFieldForcefully(configurationInstance, field, innerConfiguration);
		}
		
		Set<Field> alreadyReadFields = new HashSet<Field>();
		for(Field field : metaDataCollector.getConditionalConfigurations()){
			
			if(alreadyReadFields.contains(field))
				continue;
			
			Set<Field> dependenciesOfConditionalConfiguration = metaDataCollector.getDependenciesForConditionalConfiguration(field);
			
			for(Field dependencyOfConditionalConfiguration : dependenciesOfConditionalConfiguration){
				boolean read = readConditionally(metaDataCollector, configurationInstance, dependencyOfConditionalConfiguration);
				
				if(read){
					alreadyReadFields.add(dependencyOfConditionalConfiguration);
				}
			}
			readConditionally(metaDataCollector, configurationInstance, field);
		}
	}

	private boolean readConditionally(MetaDataCollector metaDataCollector,Object configurationInstance, Field dependencyOfConditionalConfiguration) throws IllegalAccessException, InvocationTargetException, ReadConfigurationException {
		boolean result = false;
		
		if(decision(metaDataCollector, configurationInstance, dependencyOfConditionalConfiguration)){
			Object instance = read(dependencyOfConditionalConfiguration.getType());
			injectFieldForcefully(configurationInstance, dependencyOfConditionalConfiguration, instance);
			result = true;
		}
		
		return result;
	}

	private boolean isSaved(Class<?> configurationClassToRead){
		return readConfigurations.containsKey(configurationClassToRead);
	}
	
	private void save(Object configurationInstance) {
		readConfigurations.put(configurationInstance.getClass(), configurationInstance);
	}

	Object fetch(Class<?> configurationClass){
		return readConfigurations.get(configurationClass);
	}
	
	//FIXME exception handling tests
	/**
	 * Checks if the instance to be injected is already present, if yes then same instance is returned
	 * else a call to {@link #read(Class)} is made
	 * @throws ReadConfigurationException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws InvocationTargetException 
	 */
	private void injectDependencies(Object configurationInstance, MetaDataCollector metaDataCollector) throws ReadConfigurationException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		injectRequiredDependencies(configurationInstance, metaDataCollector);
		injectOptionallyRequiredDependencies(configurationInstance, metaDataCollector);
	}

	public void injectRequiredDependencies(Object configurationInstance,MetaDataCollector metaDataCollector) throws ReadConfigurationException, IllegalAccessException {
		for(Field field : metaDataCollector.getDependencies()){
			injectDependency(configurationInstance, field);
		}
	}
	
	private void injectOptionallyRequiredDependencies(Object configurationInstance, MetaDataCollector metaDataCollector) throws IllegalArgumentException, ReadConfigurationException, IllegalAccessException, InvocationTargetException {
		for(Field field : metaDataCollector.getOptionalDependencies()){
			try{
				injectDependency(configurationInstance, field);
			}catch (ReadConfigurationException ex) {
				//eating away the exception silently as dependency is optional
				ex.printStackTrace();
			}
		}
	}

	private boolean decision(MetaDataCollector metaDataCollector, Object configurationInstance, Field field) throws IllegalAccessException, InvocationTargetException {
		return (Boolean) ReflectionUtil.invokeMethod(metaDataCollector.getDecisionMethod(), configurationInstance, field.getType());
	}


	private void injectDependency(Object configurationInstance, Field field) throws ReadConfigurationException, IllegalArgumentException, IllegalAccessException {
		Class<?> dependencyClass = field.getType();
		Object dependency = isSaved(dependencyClass) ? fetch(dependencyClass) : read(dependencyClass);
		injectFieldForcefully(configurationInstance, field, dependency);
	}

	Reader getReader(Class<? extends Reader> reader) throws ReaderInstantiationException {
		return readerFactory.getInstanceOf(reader);
	}	
}		
