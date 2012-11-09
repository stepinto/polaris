Polaris Code Search
===================

A code search engine for Java

Build
-----
Install Apache Thrift.
Run mvn package at project root.

Run
---
1. Index

		$ ./polaris index path-to-project1 path-to-project2 ... 

2. Start searcher

		$ ./polaris searchserver

3. Start web UI

		$ cd polaris-django
		$ ./manage.py runserver
