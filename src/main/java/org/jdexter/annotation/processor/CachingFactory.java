package org.jdexter.annotation.processor;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

public abstract class CachingFactory<T>{
	private ConcurrentMap<Class<?>, T> cache;
	
	public CachingFactory(){
		cache = new MapMaker().weakValues().makeMap();
	}
	
	public T create(Class<?> configurationClass){
		T instance = cache.get(configurationClass);
		
		if(instance == null){
			synchronized (configurationClass) {
				instance = createInstance(configurationClass);
				cache.put(configurationClass, instance);
			}
		}
		
		return instance;
	}

	protected abstract T createInstance(Class<?> configurationClass);
	
	T cacheQuery(Class<?> configurationClass){
		return cache.get(configurationClass);
	}
}
