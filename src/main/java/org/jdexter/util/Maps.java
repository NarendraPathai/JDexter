package org.jdexter.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

public final class Maps {

	private Maps(){}
	
	public static <K,V,T> Map<K,V> asHashMap(Set<T> valueSet, Function<T, K> keyFunction, Function<T, V> valueFunction){
		checkNotNull(valueSet, "valueSet must not be null");
		checkNotNull(keyFunction, "keyFunction must not be null");
		checkNotNull(valueFunction, "valueFunction must not be null");
		
		HashMap<K, V> map = new HashMap<K, V>();
		
		for(T v : valueSet){
			map.put(keyFunction.apply(v), valueFunction.apply(v));
		}
		
		return map;
	}
}
