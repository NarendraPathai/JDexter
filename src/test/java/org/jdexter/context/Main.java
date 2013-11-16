package org.jdexter.context;

import java.io.File;

public class Main {
	public static void main(String[] args) {
		File f = new File("src/test/resources/test-xml-configuration2.xml");
		System.out.println(f.getAbsolutePath());
		System.out.println(f.exists());
	}
}
