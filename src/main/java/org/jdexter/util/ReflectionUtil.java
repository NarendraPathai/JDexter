package org.jdexter.util;

import static org.jdexter.util.ReflectionUtil.Constraints.ATMOST_ONE;
import static org.jdexter.util.ReflectionUtil.Constraints.NON_STATIC;
import static org.jdexter.util.ReflectionUtil.Constraints.PUBLIC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class ReflectionUtil {
	private ReflectionUtil(){}
	
	public enum Constraints{
		ATMOST_ONE {
			@Override
			void validateConstraint(List<Method> annotatedMethods,Class<?> clazzUnderValidation,ConstraintsResultContainer cvc) {
				if(annotatedMethods.size() > 1){
					cvc.addViolation("Atmost one method allowed");
				}
			}
		},
		NON_STATIC {
			@Override
			void validateConstraint(List<Method> annotatedMethods,Class<?> clazzUnderValidation,ConstraintsResultContainer cvc) {
				for(Method method : annotatedMethods){
					if(Modifier.isStatic(method.getModifiers())){
						cvc.addViolation("Method: " + method.getName() + " should not be static");
					}
				}
			}
		},
		PUBLIC {
			@Override
			void validateConstraint(List<Method> annotatedMethods,Class<?> clazzUnderValidation,ConstraintsResultContainer cvc) {
				for(Method method : annotatedMethods){
					if(!Modifier.isPublic(method.getModifiers())){
						cvc.addViolation("Method: " + method.getName() + " should be public");
					}
				}
			}
		};
		
		abstract void validateConstraint(List<Method> annotatedMethods, Class<?> clazzUnderValidation, ConstraintsResultContainer cvc);
	}
	
	static class ConstraintsResultContainer{
		private List<String> violations = new ArrayList<String>();
		private boolean anyViolation = false;
		
		public List<String> getViolations() {
			return violations;
		}

		public boolean isAnyViolation() {
			return anyViolation;
		}

		public void addViolation(String message){
			anyViolation = true;
			violations.add(message);
		}
	}
	
	public static <T> T createDefaultInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException{
		if(clazz == null)
			throw new IllegalArgumentException("Class cannot be null");
		
		Constructor<?> defaultConstructor = clazz.getConstructor(new Class<?>[]{});
		Object instance = defaultConstructor.newInstance(new Object[]{});
		
		return clazz.cast(instance);
	}
	
	public static void invokeLifeCycleEvent(Object instance, Class<? extends Annotation> lifeCycleEvent) throws Throwable{
		try {
			ReflectionUtil.invokeSingleNonStaticMethodWithAnnotation(lifeCycleEvent, instance, EnumSet.of(ATMOST_ONE,PUBLIC,NON_STATIC));
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			throw e;
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static void invokeSingleNonStaticMethodWithAnnotation(Class<? extends Annotation> annotation, Object instance, EnumSet<Constraints> constraints) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = instance.getClass();
		
		List<Method> annotatedMethods = getDeclaredMethodsAnnotatedWith(annotation, clazz);
		
		if(annotatedMethods.isEmpty())
			return;
		
		validateConstraints(annotatedMethods, clazz, constraints);
		
		invokeMethodWithNoArguments(annotatedMethods.get(0), instance);
	}

	private static void validateConstraints(List<Method> annotatedMethods, Class<?> clazzUnderValidation, EnumSet<Constraints> constraints) {
		ConstraintsResultContainer cvc = new ConstraintsResultContainer();
		for(Constraints constraint : constraints){
			constraint.validateConstraint(annotatedMethods, clazzUnderValidation, cvc);
		}
		if(cvc.isAnyViolation()){
			throw new IllegalArgumentException(String.valueOf(cvc.getViolations()));
		}
	}

	public static void invokeMethodWithNoArguments(Method method, Object instance) throws IllegalAccessException,InvocationTargetException {
		method.invoke(instance, (Object[])null);
	}
	
	public static Object invokeMethod(Method method, Object instance, Object...arguments) throws IllegalAccessException,InvocationTargetException {
		return method.invoke(instance, arguments);
	}
	
	private static List<Method> getDeclaredMethodsAnnotatedWith(Class<? extends Annotation> annotation, Class<?> classToScan){
		List<Method> annotatedMethods = new ArrayList<Method>();
		for(Method method : classToScan.getDeclaredMethods()){
			if(method.getAnnotation(annotation) != null){
				annotatedMethods.add(method);
			}
		}
		return annotatedMethods;
	}

	public static <T extends Annotation> T getAnnotation(Class<?> clazzToScan, Class<T> annotation){
		return clazzToScan.getAnnotation(annotation);
	}
	
	public static void injectFieldForcefully(Object configurationInstance, Field field, Object dependency) throws IllegalAccessException {
		boolean accessibility = field.isAccessible();
		field.setAccessible(true);
		field.set(configurationInstance, dependency);
		field.setAccessible(accessibility);
	}
}
