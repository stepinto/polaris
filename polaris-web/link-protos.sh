#!/bin/sh

PROTO_DIR=$(dirname $0)/../polaris-core/src/main/proto
test -d $PROTO_DIR || (echo 'Bad proto dir: ' $PROTO_DIR; exit 1)
PROTO_DIR_ABS=$(readlink -e $PROTO_DIR)
TARGET_DIR=$(dirname $0)/public/protos
TARGET_DIR_ABS=$(readlink -e $TARGET_DIR)

for PROTO_FILE in $PROTO_DIR_ABS/*.proto; do
  echo 'Linking' $(basename $PROTO_FILE) 'to' $TARGET_DIR_ABS
  ln -sf $PROTO_FILE $TARGET_DIR_ABS || exit 1
done
