package org.jdexter.reader;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.StringReader;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.PostRead;
import org.jdexter.annotation.PreRead;
import org.jdexter.context.ConfigurationContextUnitTest.TestCompositeConfigurationClassWithJAXBReader;
import org.jdexter.reader.JAXBReader;
import org.jdexter.reader.Reader;
import org.jdexter.reader.annotation.XMLProperties;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JAXBReaderUnitTest {
	
	private JAXBReader reader;

	@BeforeMethod
	public void setUp(){
		reader = new JAXBReader();
	}
	
	@Test
	public void testJAXBReader_ShouldInheritReaderClass(){
		assertEquals(JAXBReader.class.getSuperclass(),Reader.class);
	}
	
	@Test
	public void testRead_ShouldReturnNonNullInstance() throws Throwable{
		assertNotNull(reader.read(TestXMLConfigurationWithProperXMLRootElement.class, new StringReader(TestXMLConfigurationWithProperXMLRootElement.xml)));
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldReturnTheInstanceOfConfigurationClassPassed")
	public void testRead_ShouldReturnTheInstanceOfConfigurationClassPassed(Class<?> configurationClassToRead) throws Throwable{
		assertEquals(reader.read(configurationClassToRead,new StringReader(TestXMLConfigurationWithProperXMLRootElement.xml)).getClass(), configurationClassToRead);
	}
	
	@Test(dependsOnMethods = {"testRead_ShouldReturnTheInstanceOfConfigurationClassPassed"})
	public void testRead_ShouldCallPreReadLifeCycleEvent() throws Throwable{
		TestXMLConfigurationWithProperXMLRootElement instance = (TestXMLConfigurationWithProperXMLRootElement) reader.read(TestXMLConfigurationWithProperXMLRootElement.class,new StringReader(TestXMLConfigurationWithProperXMLRootElement.xml));
		assertTrue(instance.isPreReadCalled());
	}
	
	@Test(dependsOnMethods = {"testRead_ShouldReturnTheInstanceOfConfigurationClassPassed"})
	public void testRead_ShouldCallPreReadBeforePostReadLifeCycleEvent() throws Throwable{
		TestXMLConfigurationWithProperXMLRootElement instance = (TestXMLConfigurationWithProperXMLRootElement) reader.read(TestXMLConfigurationWithProperXMLRootElement.class, new StringReader(TestXMLConfigurationWithProperXMLRootElement.xml));
		assertTrue(instance.isPreCalledBeforePost());
	}
	
	@Test
	public void testRead_ShouldCallPreLifeCycleEventOnce() throws Throwable{
		TestXMLConfigurationWithProperXMLRootElement instance = (TestXMLConfigurationWithProperXMLRootElement) reader.read(TestXMLConfigurationWithProperXMLRootElement.class, new StringReader(TestXMLConfigurationWithProperXMLRootElement.xml));
		assertEquals(instance.getPreReadCallCount(), 1);
	}
	
	
	@Test(expectedExceptions = {Exception.class},
			dataProvider = "dataFor_testRead_ShouldThrowException_WhenConfigurationClassHasInvalidXMLRootElementAnnotation")
	public void testRead_ShouldThrowException_WhenConfigurationClassHasInvalidXMLRootElementAnnotation(Class<?> configurationClassToRead) throws Throwable{
		reader.read(configurationClassToRead);
	}
	
	
	@Test(dataProvider = "dataFor_testRead_ShouldBeAbleToReadInMemoryXMLFile")
	public void testRead_ShouldBeAbleToReadInMemoryXMLFile(String xmlData) throws FileNotFoundException, Throwable{
		Object instance = reader.read(TestXMLConfigurationWithProperXMLRootElement.class, new StringReader(xmlData));
		assertNotNull(instance);
	}
	
	@Test
	public void testRead_ShouldBeAbleToReadXMLConfiguration() throws Throwable{
		TestXMLConfigurationWithElements instance = (TestXMLConfigurationWithElements) reader.read(TestXMLConfigurationWithElements.class, new StringReader(TestXMLConfigurationWithElements.xml));
		assertEquals(1, instance.getIntVal());
	}
	
	@Test(expectedExceptions = {Exception.class})
	public void testRead_ShouldThrowExactException_WhenPreReadThrowsException() throws Throwable{
		reader.read(TestXMLConfigurationWithPreReadThrowingException.class,new StringReader(TestXMLConfigurationWithPreReadThrowingException.xml));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testRead_ShouldThrowIllegalArgumentException_WhenXMLPropertiesAnnotationIsNotPresent() throws Throwable{
		//JAXB cannot handle non static inner classes but this class should not reach the reading point
		@Configuration(readWith = JAXBReader.class)
		class Test{
		}
		
		reader.read(Test.class);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testRead_ShouldThrowIllegalArgumentExceptionIfFileNameIsNullOrEmpty() throws Throwable{
		//JAXB cannot handle non static inner classes but this class should not reach the reading point
		@Configuration(readWith = JAXBReader.class)
		@XMLProperties(path = "")
		class Test{
		}
		
		reader.read(Test.class);
	}

	@Test
	public void testRead_ShouldBeAbleToReadCompositeConfigurationWithDependencies() throws Throwable{
		reader.read(TestCompositeConfigurationClassWithJAXBReader.class, new StringReader(TestCompositeConfigurationClassWithJAXBReader.xml));
	}
	
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldBeAbleToReadInMemoryXMLFile() {
		return new Object[][] {
				{"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test-xml-configuration></test-xml-configuration>"},
				{"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test-xml-configuration><x>1</x></test-xml-configuration>"},
				{"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test-xml-configuration><x>123</x></test-xml-configuration>"}
		};
	}
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldThrowException_WhenConfigurationClassHasInvalidXMLRootElementAnnotation() {
		return new Object[][] {
				{TestXMLConfigurationWithoutXMLRootElement.class},
				{TestXMLConfigurationWithNoNameInXMLRootElement.class},
				{TestXMLConfigurationWithInvalidNameInXMLRootElement.class}
		};
	}
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldReturnTheInstanceOfConfigurationClassPassed(){
		return new Object[][]{
				{TestXMLConfigurationWithProperXMLRootElement.class},
		};
	}
	
	@Configuration(readWith = JAXBReader.class)
	public static class TestXMLConfigurationWithoutXMLRootElement{
		
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement
	public static class TestXMLConfigurationWithNoNameInXMLRootElement{
		
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "invalid-name")
	public static class TestXMLConfigurationWithInvalidNameInXMLRootElement{
	
	}
	
	@XMLProperties(path = "")
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	public static class TestXMLConfigurationWithProperXMLRootElement{
		public static String xml = "<test-xml-configuration></test-xml-configuration>";
		private boolean preReadCalled = false;
		private boolean postReadCalled = false;
		private boolean preCalledBeforePost = false;
		private int preReadCallCount = 0;
		private int postReadCallCount = 0;

		@PreRead
		public void preRead(){
			preReadCalled = true;
			preReadCallCount++;
			preCalledBeforePost = !postReadCalled;
		}

		@PostRead
		public void postRead() throws Exception{
			postReadCalled = true;
			postReadCallCount++;
		}
		
		public int getPreReadCallCount() {
			return preReadCallCount;
		}
		
		public int getPostReadCallCount() {
			return postReadCallCount;
		}

		public boolean isPreReadCalled() {
			return preReadCalled;
		}

		public boolean isPreCalledBeforePost() {
			return preCalledBeforePost;
		}

		public boolean isPostReadCalled() {
			return postReadCalled;
		}

	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement
	public static class TestXMLConfigurationWithoutXMLElement{
		
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	public static class TestXMLConfigurationWithElements{
		private static final String xml = "<test-xml-configuration><int-val>1</int-val></test-xml-configuration>";
		private int intVal;

		@XmlElement(name = "int-val")
		public int getIntVal() {
			return intVal;
		}

		public void setIntVal(int intVal) {
			this.intVal = intVal;
		}
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	public static class TestXMLConfigurationWithPreReadThrowingException{
		private static final String xml = "<test-xml-configuration></test-xml-configuration>";
		
		@PreRead
		public void preRead() throws Exception{
			throw new Exception();
		}
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	public static class TestXMLConfigurationWithPostReadThrowingException{
		private static final String xml = "<test-xml-configuration></test-xml-configuration>";
		
		@PostRead
		public void postRead() throws Exception{
			throw new Exception();
		}
	}
}
