Polaris Code Search
===================

A code search engine for Java

Prestiques
---------
* JDK 1.6+
* Maven 2+
* Git
* GNU Toolchain (gcc, g++, make, automake, autoconf, libtool...)
* Protocol Buffers 2.40+

Build Apache Crunch
-------------------
		$ git clone https://git-wip-us.apache.org/repos/asf/incubator-crunch.git
		$ cd incubator-crunch
		$ mvn install -Phadoop-2 -DskipTests

Build Polaris
-------------
Go to Polaris project root and run:

		$ mvn package

Run
---
1. Build index

		$ ./polaris index path-to-project1 path-to-project2 ... 

	The index files will be stored in "index/".

2. Start searcher

		$ ./polaris devserver

    Navigate to http://localhost:8080.
