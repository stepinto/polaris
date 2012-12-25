build:
	thrift --gen js:node ../polaris-core/src/main/thrift/parser.thrift
	thrift --gen js:node ../polaris-core/src/main/thrift/search.thrift
	thrift --gen js:node ../polaris-core/src/main/thrift/sourcedb.thrift
	thrift --gen js:node ../polaris-core/src/main/thrift/typedb.thrift
	thrift --gen js:node ../polaris-core/src/main/thrift/usagedb.thrift

clean:
	rm -rf gen-nodejs
