package org.jdexter.reader;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;

import org.jdexter.annotation.PreRead;
import org.jdexter.reader.annotation.XMLProperties;
import org.jdexter.util.ReflectionUtil;

public class JAXBReader extends Reader{

	@Override
	public Object read(Class<?> classToRead) throws Throwable {
		String fileName = extractFileName(classToRead);
		Unmarshaller unmarshaller = createUnMarshaller(classToRead);
		return unmarshaller.unmarshal(new File(fileName));
	}

	public String extractFileName(Class<?> classToRead) {
		XMLProperties xmlProperties = classToRead.getAnnotation(XMLProperties.class);
		if(xmlProperties == null)
			throw new IllegalArgumentException("@XMLProperties annotation is missing from class: " + classToRead.getName());
		
		String path = xmlProperties.path();
		if(path.length() == 0)
			throw new IllegalArgumentException("path in @XMLProperties annotation cannot be blank or null");
		
		return classToRead.getAnnotation(XMLProperties.class).path();
	}
	
	Object read(Class<?> classToRead, java.io.Reader reader) throws Throwable{
		try{
			Unmarshaller unmarshaller = createUnMarshaller(classToRead);
			return unmarshaller.unmarshal(reader);
		}catch (IllegalArgumentException e) {
			throw e.getCause();
		}
	}

	public Unmarshaller createUnMarshaller(Class<?> classToRead) throws JAXBException {
		Unmarshaller unmarshaller = JAXBContext.newInstance(classToRead).createUnmarshaller();
		unmarshaller.setListener(new LifeCycleEventsExecutor());
		return unmarshaller;
	}
	
	class LifeCycleEventsExecutor extends Listener{
		@Override
		public void beforeUnmarshal(Object arg0, Object arg1) {
			try {
				ReflectionUtil.invokeLifeCycleEvent(arg0, PreRead.class);
			} catch (Throwable e) {
				//FIXME these exceptions can be changed to some custom runtime exception as handler does not support checked exception
				throw new IllegalArgumentException(e);
			}
		}
	}
}
