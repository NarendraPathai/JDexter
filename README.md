JDexter
=======

A painless configuration management API for Java

What is this?

Code named Project JDexter is an API that allows you to manage the configuration of your Java Application pretty easily.
The API will focus on providing following features:

 - Reading configuration files (filetypes: xml, properties, json, many other types) 
 - Serializing configuration
 - Reloading configuration without application restart

How does it help?

 - Do you have many configuration files that you have to read at the start of the application?
 - Do you manually read those configuration files using SAX parsers?
 - Are you unable to easily manage dependencies among those configuration files?
 - Do you manually do validation for the values in the configuration?
 - Do you want to reload the configuration on the fly without application restarts?
 
 If answer any of the above questions was yes then we can help you.
 
How does it work?

The architecture of the API is focused on Readers and Writers which are an extensible part of the API and the users
are allowed to provide their own custom implementations.

Basic usage:

Suppose you have an xml named test-xml-configuration1.xml which stores some configuration that you need to read. So
you create a class TestXMLConfiguration and provide the required information

        @Configuration(readWith = JAXBReader.class)
        @XmlRootElement(name = "test-xml-configuration")
        @XMLProperties(path = "test-xml-configuration1.xml")
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
                public void postReadOperations(){
                    //handle post read callback   
                }
        }
        
Now the XML contains following data:

      <?xml version="1.0" encoding="UTF-8"?>
      <test-xml-configuration>
	        <x>2</x>
      </test-xml-configuration>
      
Reading the XML is just the matter of telling the API to give you the read instance of TestXMLConfiguration class
from the location you have specified.

      ConfigurationContext ctx = new ConfigurationContext();
      TestXMLConfiguration config = ctx.read(TestXMLConfiguration.class);
      
      int value = config.getX(); //you can now easily use the read configuration
      
Advanced Usages:

Not just this API also allows for dependency resolving on other configuration files. Documentation on it coming soon.


How to build?
=============

Execute the following command after cloning the git repo at your local machine

mvn install

How to configure eclipse workspace?
====================================
Execute the following command after cloning the git repo at your local machine

mvn eclipse:eclipse
