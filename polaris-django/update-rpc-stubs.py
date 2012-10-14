#!/usr/bin/env python

import subprocess
import os
import shutil

PROTO_DIR = '../polaris-core/src/main/thrift'
THRIFT_BIN = 'thrift'
OUTPUT_DIR = 'thriftgen'

if os.path.exists(OUTPUT_DIR):
  shutil.rmtree(OUTPUT_DIR)
os.mkdir(OUTPUT_DIR)
files = []
for file in os.listdir(PROTO_DIR):
  print 'Found thrift protocol: ' + file
  file = os.path.join(PROTO_DIR, file)
  subprocess.check_call(
      [THRIFT_BIN, '--out', OUTPUT_DIR, '--gen', 'py', file])

# Since "from" is a python keyword, we have to replace Span#from
# Span#to _from to get it compiled.
file_to_fix = os.path.join(OUTPUT_DIR, 'polaris/token/ttypes.py')
f = open(file_to_fix, 'r')
s = f.read()
f.close()
s = s.replace('from=None', '_from=None')
s = s.replace('self.from = from', 'self._from = _from')
s = s.replace('self.from', 'self._from')
f = open(file_to_fix, 'w')
f.write(s)
f.close()

# vim: ts=2 sw=2 et
