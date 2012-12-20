Polaris Code Search
===================

A code search engine for Java

Prestiques
---------
* JDK 1.6+
* Maven 2+
* Thrift 0.9.0 (http://thrift.apache.org/)

Build
-----
Go to project root and run:

		$ mvn install

Run
---
1. Build index

		$ ./polaris index path-to-project1 path-to-project2 ... 

	The index files will be stored in "index/".

2. Start searcher

		$ ./polaris searchserver

3. Start web UI

		$ mvn jetty:run -pl polaris-webui
		
	Then a jetty server will be ready at http://localhost:8080.
