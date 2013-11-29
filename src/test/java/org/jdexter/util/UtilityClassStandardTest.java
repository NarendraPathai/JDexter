package org.jdexter.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jdexter.util.ReflectionUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public final class UtilityClassStandardTest {

	private Class<?> utilityClassUnderTest;
	
	@Factory(dataProvider="dataFor_UtilityClassStandardTest")
	public UtilityClassStandardTest(Class<?> utilityClassToTest) {
		this.utilityClassUnderTest = utilityClassToTest;
	}
	
	@DataProvider
	public static Object[][] dataFor_UtilityClassStandardTest(){
		return new Object[][]{
				{ReflectionUtil.class},
				{Maps.class}
		};
	}
	
	@Test
	public void testClassMustBeFinal(){
		assertTrue(Modifier.isFinal(utilityClassUnderTest.getModifiers()));
	}
	
	@Test
	public void testPresenceOfOnlyOneConstructor(){
		assertEquals(utilityClassUnderTest.getDeclaredConstructors().length, 1,"More than one constructor found in utility class");
	}
	
	@Test
	public void testConstructorMustBePrivate() throws NoSuchMethodException, SecurityException{
		assertTrue(Modifier.isPrivate(utilityClassUnderTest.getDeclaredConstructor(new Class<?>[]{}).getModifiers()));
	}
	
	@Test
	public void testInstantiationOfUtilityClass() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Constructor<?> constructor = utilityClassUnderTest.getDeclaredConstructor(new Class<?>[]{});
		constructor.setAccessible(true);
		constructor.newInstance((Object[])null);
		constructor.setAccessible(false);
	}
	
	@Test
	public void allMethodsMustBeStatic(){
		for(Method method : utilityClassUnderTest.getDeclaredMethods()){
			assertTrue(Modifier.isStatic(method.getModifiers()), "Non-static method present in utility class");
		}
	}
}
