Jenkins War Numbering Plugin
=======================================

[Parallel deployment](http://tomcat.apache.org/tomcat-7.0-doc/config/context.html#Parallel_deployment) of Tomcat 7 is useful.
This plugin makes \*.war files to "xxx##{$BUILD\_NAME}.war" format.

Author
---------------
* @mallowlabs

For developers
---------------
Build:

    $ mvn package

You will get target/war-numbering.hpi
