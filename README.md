Polaris Code Search
===================

A code search engine for Java

Prestiques
---------
* JDK 1.6+
* Maven 2+
* Git
* GNU Toolchain (gcc, g++, make, automake, autoconf, libtool...)

Build Apache Thrift
-------------------
		$ git clone https://git-wip-us.apache.org/repos/asf/thrift.git
		$ cd thrift
		$ curl https://issues.apache.org/jira/secure/attachment/12562777/thrift-1816.patch | patch -p1
		$ ./bootstrap.sh
		$ ./configure  # you need to compile the thrift compiler at least
		$ make
		$ sudo make install

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

		$ ./polaris searchserver

3. Start web UI
	
TBD
