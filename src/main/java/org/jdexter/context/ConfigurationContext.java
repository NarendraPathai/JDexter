package org.jdexter.context;

import static org.jdexter.util.ReflectionUtil.injectFieldForcefully;
import static org.jdexter.util.ReflectionUtil.invokeLifeCycleEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdexter.annotation.PostRead;
import org.jdexter.annotation.processor.MetaDataCollector;
import org.jdexter.exception.ReadConfigurationException;
import org.jdexter.reader.Reader;
import org.jdexter.reader.ReaderFactory;
import org.jdexter.reader.exception.ReaderInstantiationException;
import org.jdexter.util.ReflectionUtil;


public class ConfigurationContext {

	private ReaderFactory readerFactory;

	private Map<Class<?>,Object> readConfigurations;
	
	public ConfigurationContext() {
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
			
			if(isSaved(configurationClassToRead))
				return configurationClassToRead.cast(fetch(configurationClassToRead));

			MetaDataCollector metaDataCollector = MetaDataCollector.of(configurationClassToRead);

			Object configurationInstance = readerFactory.getInstanceOf(metaDataCollector.getReader()).read(configurationClassToRead);
			
			injectDependencies(configurationInstance, metaDataCollector);

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

	private void injectOptionallyRequiredDependencies(Object configurationInstance, MetaDataCollector metaDataCollector) throws IllegalArgumentException, ReadConfigurationException, IllegalAccessException, InvocationTargetException {
		for(Field field : metaDataCollector.getOptionallyRequiredConfigurations()){
			if(decision(metaDataCollector, configurationInstance, field)){
				injectDependency(configurationInstance, field);
			}
		}
	}

	private boolean decision(MetaDataCollector metaDataCollector, Object configurationInstance, Field field) throws IllegalAccessException, InvocationTargetException {
		return (Boolean) ReflectionUtil.invokeMethod(metaDataCollector.getDecisionMethod(), configurationInstance, field.getType());
	}

	public void injectRequiredDependencies(Object configurationInstance,MetaDataCollector metaDataCollector) throws ReadConfigurationException, IllegalAccessException {
		for(Field field : metaDataCollector.getRequiredConfigurations()){
			injectDependency(configurationInstance, field);
		}
	}

	private void injectDependency(Object configurationInstance, Field field) throws ReadConfigurationException, IllegalArgumentException, IllegalAccessException {
		Object dependency = read(field.getType());
		injectFieldForcefully(configurationInstance, field, dependency);
	}

	Reader getReader(Class<? extends Reader> reader) throws ReaderInstantiationException {
		return readerFactory.getInstanceOf(reader);
	}	
}		
