THRIFT = thrift

build:
	${THRIFT} --gen js:node ../polaris-core/src/main/thrift/parser.thrift
	${THRIFT} --gen js:node ../polaris-core/src/main/thrift/search.thrift
	${THRIFT} --gen js:node ../polaris-core/src/main/thrift/sourcedb.thrift
	${THRIFT} --gen js:node ../polaris-core/src/main/thrift/typedb.thrift
	${THRIFT} --gen js:node ../polaris-core/src/main/thrift/usagedb.thrift

clean:
	rm -rf gen-nodejs
