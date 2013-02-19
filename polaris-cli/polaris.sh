#!/bin/sh

exec java -Xmx512m -Xms256m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -verbose:gc -Xloggc:/tmp/polaris-gc.log -jar "$0" $@

# END OF SCRIPT
