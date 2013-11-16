package org.jdexter.context;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.lang.reflect.InvocationTargetException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.Depends;
import org.jdexter.annotation.PostRead;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClass;
import org.jdexter.context.ConfigurationContextUnitTest.TestReader;
import org.jdexter.exception.ReadConfigurationException;
import org.jdexter.reader.JAXBReader;
import org.jdexter.reader.Reader;
import org.jdexter.reader.ReaderFactory;
import org.jdexter.reader.annotation.XMLProperties;
import org.jdexter.reader.exception.ReaderInstantiationException;
import org.jdexter.util.ReflectionUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


//TODO should call read on proper reader
//TODO tests if file not found are remaining
public class ConfigurationContextIntegrationTest {

	private ConfigurationContext ctx;

	@BeforeMethod
	public void setUp(){
		ctx = new ConfigurationContext();
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldCreateInstanceOfReaderIfNotAvailable")
	public void testRead_ShouldCreateInstanceOfReaderIfNotAvailable(Class<?> configurationClassToRead, Class<? extends Reader> reader) throws ReaderInstantiationException, ReadConfigurationException{
		ctx.read(configurationClassToRead);
		assertNotNull(ctx.getReader(reader));
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldCreateInstanceOfReaderIfNotAvailable",
			dependsOnMethods = {"testRead_ShouldCreateInstanceOfReaderIfNotAvailable"})
	public void testRead_ShouldCreateInstanceOfReaderDeclaredInConfigurationProperties(Class<?> configurationClassToRead, Class<? extends Reader> reader) throws ReadConfigurationException, InstantiationException, IllegalAccessException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException, ReaderInstantiationException{
		ctx.read(configurationClassToRead);
		assertEquals(ctx.getReader(reader).getClass(),reader);
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldThrowProperlyWrappedExceptions_WhenReaderCannotBeInstantiated")
	public void testRead_ShouldThrowProperlyWrappedExceptions_WhenReaderCannotBeInstantiated(Class<?> configurationClassToRead, Class<?> expectedWrappedException){
		ReadConfigurationException rce = null;
		try{
			ctx.read(configurationClassToRead);
		}catch(ReadConfigurationException rce1){
			rce = rce1;
		}
		assertNotNull(rce,expectedWrappedException + " not thrown while reading : " + configurationClassToRead);
		assertEquals(rce.getCause().getClass(), expectedWrappedException);
	}

	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testConstructor_ShouldThrowIllegalArgumentException_WhenReaderFactoryPassedIsNull(){
		ctx = new ConfigurationContext(null);
	}
	
	@Test
	public void testRead_ShouldCallReaderFactoryToGetInstanceOfReader_WhenAValidConfigurationClassIsPassed() throws ReaderInstantiationException, ReadConfigurationException{
		ReaderFactory rf = mock(ReaderFactory.class);
		when(rf.getInstanceOf(TestReader.class)).thenReturn(new TestReader());
		
		ctx = new ConfigurationContext(rf);
		ctx.read(TestConfigurationClass.class);
		
		verify(rf,times(1)).getInstanceOf(TestReader.class);
	}
	
	@Test
	public void testRead_ShouldCallReadOnReaderProvidedInConfigurationProperties() throws Throwable{
		ReaderFactory rf = mock(ReaderFactory.class);
		TestReader mockReader = mock(TestReader.class);
		
		when(rf.getInstanceOf(TestReader.class)).thenReturn(mockReader);
		when(mockReader.read(TestConfigurationClass.class)).thenReturn(ReflectionUtil.createDefaultInstance(TestConfigurationClass.class));
		ctx = new ConfigurationContext(rf);
		ctx.read(TestConfigurationClass.class);
		
		verify(mockReader,times(1)).read(TestConfigurationClass.class);
	}
	
	@Test
	public void testRead_ShouldBeAbleToReadCompositeConfiguration() throws ReadConfigurationException{
		ctx.read(TestCompositeConfigurationWithJAXBReader.class);
	}
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldThrowProperlyWrappedExceptions_WhenReaderCannotBeInstantiated(){
		return new Object[][]{
				{TestConfigurationClassReadUsingReaderWithNoDefaultConstructor.class,			NoSuchMethodException.class},
				{TestConfigurationClassReadUsingReaderWithProtectedConstructor.class,			NoSuchMethodException.class},
				{TestConfigurationClassReadUsingReaderPrivateConstructor.class,					NoSuchMethodException.class},
				{TestConfigurationClassReadUsingReaderWithPackageProtectedConstructor.class,	NoSuchMethodException.class}
		};
	}
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldCreateInstanceOfReaderIfNotAvailable(){
		return new Object[][]{
				{TestConfigurationClassReadUsingTestReader.class,		TestReader.class},
		};
	}
	
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassReadUsingTestReader{
		
	}
	
	@Configuration(readWith = ReaderWithNoDefaultConstructor.class)
	public static class TestConfigurationClassReadUsingReaderWithNoDefaultConstructor{
	}
	
	@Configuration(readWith = ReaderWithPackageProtectedConstructor.class)
	public static class TestConfigurationClassReadUsingReaderWithPackageProtectedConstructor{
	}
	
	@Configuration(readWith = ReaderWithProtectedConstructor.class)
	public static class TestConfigurationClassReadUsingReaderWithProtectedConstructor{
	}
	
	@Configuration(readWith = ReaderWithPrivateConstructor.class)
	public static class TestConfigurationClassReadUsingReaderPrivateConstructor{
	}
	
	public static class ReaderWithNoDefaultConstructor extends Reader{
		public ReaderWithNoDefaultConstructor(Object o) {
		}

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	public static class ReaderWithProtectedConstructor extends Reader{
		protected ReaderWithProtectedConstructor() {
		}

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	public static class ReaderWithPackageProtectedConstructor extends Reader{
		ReaderWithPackageProtectedConstructor() {
		}

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	public static class ReaderWithPrivateConstructor extends Reader{
		private ReaderWithPrivateConstructor() {
		}

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	@XMLProperties(path = "src/test/resources/test-xml-configuration2.xml")
	public static class TestCompositeConfigurationWithJAXBReader{
		@Depends private TestXMLConfiguration xmlDependency;
		@Depends private TestInnerCompositeConfiguration compositeDependency;
		
		private int x;

		@XmlElement(name = "x")
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}
		
		@PostRead
		public void verifyValues(){
			assertEquals(getX(), 2);
			assertNotNull(xmlDependency);
			assertNotNull(compositeDependency);
		}
	}
	
	@SuppressWarnings("unused")
	@Configuration
	public static class TestInnerCompositeConfiguration{
		@Depends private TestXMLConfiguration1 xmlDependency;
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	@XMLProperties(path = "src/test/resources/test-xml-configuration.xml")
	public static class TestXMLConfiguration{
		private int x;

		@XmlElement(name = "x")
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}
		
		@PostRead
		public void verify(){
			assertEquals(getX(), 1);
		}
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	@XMLProperties(path = "src/test/resources/test-xml-configuration1.xml")
	public static class TestXMLConfiguration1{
		private int x;

		@XmlElement(name = "x") 
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}
		
		@PostRead
		public void verify(){
			assertEquals(getX(), 2);
		}
		
	}
}
