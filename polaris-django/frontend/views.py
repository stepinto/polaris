# Create your views here.

# TODO: socket leaked after init_rpc

import os
import sys
from urllib import quote
from django.http import HttpResponse
from django.shortcuts import render_to_response
from django.shortcuts import redirect 
from django.template.loader import render_to_string
from django.forms.widgets import Widget
from django.http import Http404
from thrift import Thrift
from thrift.transport import TTransport
from thrift.transport import TSocket
from thrift.protocol.TBinaryProtocol import TBinaryProtocolAccelerated
from polaris.search import *
from polaris.search.ttypes import *
from settings import RPC_SERVER_HOST
from settings import RPC_SERVER_PORT
import json
from xml.dom import minidom

SEARCH_RESULT_PER_PAGE = 10

def init_rpc():
  socket = TSocket.TSocket(RPC_SERVER_HOST, RPC_SERVER_PORT)
  transport = TTransport.TBufferedTransport(socket)
  transport.open()
  protocol = TBinaryProtocolAccelerated(transport)
  return TCodeSearchService.Client(protocol)

def index(req):
  return render_to_response('index.html')

def hello(req):
  return HttpResponse('Hello, world!')

def about(req):
  return render_to_response('about.html')

def search(req):
  query = str(req.GET['q'])
  page_no = int(req.GET.get('page', '0'))
  rpc = init_rpc()
  rpc_req = TSearchRequest()
  rpc_req.query = query
  rpc_req.rankFrom = SEARCH_RESULT_PER_PAGE * page_no
  rpc_req.rankTo = rpc_req.rankFrom + SEARCH_RESULT_PER_PAGE
  rpc_resp = rpc.search(rpc_req)
  check_rpc_status(rpc_resp.status)
  next_page_url = '/search?q=%s&page=%d' % (quote(query), page_no + 1)
  return render_to_response('search.html', \
      {'query': query, 'next_page_url': next_page_url, 'resp':rpc_resp})

def goto_type(req, type_id):
  type_id = int(type_id)
  rpc = init_rpc()
  rpc_req = TGetTypeRequest()
  rpc_req.typeId = type_id
  rpc_resp = rpc.getType(rpc_req)
  check_rpc_status(rpc_resp.status)
  jump_target = rpc_resp.classType.jumpTarget
  return redirect('/goto/source/%d?offset=%d' % (jump_target.fileId, jump_target.offset))

def goto_field(req, field_id):
  field_id = int(field_id)
  rpc = init_rpc()
  rpc_req = TGetFieldRequest()
  rpc_req.fieldId = field_id
  rpc_resp = rpc.getField(rpc_req)
  check_rpc_status(rpc_resp.status)
  jump_target = rpc_resp.field.jumpTarget
  return redirect('/goto/source/%d?offset=%d' % (jump_target.fileId, jump_target.offset))

def goto_method(req, method_id):
  method_id = int(method_id)
  rpc = init_rpc()
  rpc_req = TGetMethodRequest()
  rpc_req.methodId = method_id
  rpc_resp = rpc.getMethod(rpc_req)
  check_rpc_status(rpc_resp.status)
  jump_target = rpc_resp.method.jumpTarget
  return redirect('/goto/source/%d?offset=%d' % (jump_target.fileId, jump_target.offset))
  pass

def goto_source_by_id(req, file_id):
  rpc_req = TSourceRequest()
  rpc_req.fileId = int(file_id)
  return goto_source_helper(req, rpc_req)

def goto_source_by_path(req, project, path):
  rpc_req = TSourceRequest()
  rpc_req.projectName = project
  rpc_req.fileName = '/' + path
  return goto_source_helper(req, rpc_req)

def goto_source_helper(req, rpc_req):
  def trim_package(type_name):
    return type_name.split('.')[-1]
  def trim_type(member_name):
    return member_name.split('#')[-1]

  offset = int(req.GET.get('offset', '0'))
  rpc = init_rpc()
  rpc_resp = rpc.source(rpc_req)
  check_rpc_status(rpc_resp.status)
  source = rpc_resp.source
  html = render_annotated_source(source.annotatedSource)
  line_no = convert_offset_to_line_no(source.source, int(req.GET.get('offset', '0')))
  line_no = max(0, line_no - 10) # show the line in center
  line_count = source.source.count('\n')
  line_no_html = ''
  for i in xrange(line_count):
    line_no_html += '<li id="line_no_%d">%d</li>' % (i, i)
  path_parts = [source.project] + filter(lambda x: x != '', source.path.split('/'))
  rpc_req2 = TListTypesInFileRequest()
  rpc_req2.fileId = source.id
  rpc_req2.limit = sys.maxint
  rpc_resp2 = rpc.listTypesInFile(rpc_req2)
  check_rpc_status(rpc_resp2.status)
  for class_type in rpc_resp2.classTypes:
    class_type.display_name = trim_package(class_type.handle.name)
    for field in (class_type.fields or []):
      field.dipslay = trim_type(field.handle.name) + ': ' + trim_package(field.type.name)
    for method in (class_type.methods or []):
      name = trim_type(method.handle.name)
      return_type = trim_package(method.returnType.name)
      if method.parameters:
        method.display_name = '%s(%s): %s' % ( \
            name, \
            ', '.join([trim_package(p.type.name) for p in method.parameters]), \
            return_type)
      else:
        method.display_name = '%s: %s' % (name, return_type)
  return render_to_response('source.html', {
      'project': source.project,
      'path_parts': path_parts,
      'dir': os.path.dirname(source.path),
      'source_html': html,
      'line_no': line_no,
      'line_no_html': line_no_html,
      'types': rpc_resp2.classTypes
      })

def ajax_complete(req):
  rpc = init_rpc()
  rpc_req = TCompleteRequest()
  rpc_req.query = req.GET['q']
  rpc_req.limit = int(req.GET['n'])
  rpc_resp = rpc.complete(rpc_req)
  check_rpc_status(rpc_resp.status)
  return HttpResponse(json.dumps(rpc_resp.entries))

def ajax_layout(req, project, path):
  path = '/' + path
  rpc = init_rpc()
  rpc_req = TLayoutRequest()
  rpc_req.projectName = project
  rpc_req.directoryName = path
  rpc_resp = rpc.layout(rpc_req)
  check_rpc_status(rpc_resp.status)
  result = []
  for child in rpc_resp.children:
    child = remove_start(child, path + '/')
    if child.endswith('/'):
      has_children = True
      text = remove_end(child, '/')
    else:
      has_children = False
      text = "<a href=/goto/source/%s%s>%s</a>" % (quote(project),
          quote(os.path.join(path, child)), quote(child))
    result.append({'text': text, 'hasChildren': has_children})
  return HttpResponse(json.dumps(result))

def hex_encode(s):
  return s.encode('hex')

def hex_decode(s):
  return s.decode('hex')

def check_rpc_status(status): 
  class RpcError:
    def __init__(self, status):
      self.status = status
  if status == TStatusCode.OK:
    return
  elif status == TStatusCode.FILE_NOT_FOUND:
    raise Http404
  else:
    raise RpcError(status)

def render_annotated_source(source):
  # TODO: Dom may be slow. Consider to use Sax.
  def clear_attributes(node):
    for a in node.attributes.keys():
      node.removeAttribute(a)
    return

  dom = minidom.parseString(source)
  for node in dom.getElementsByTagName('field-declaration'):
    field_name = node.getAttribute('field')
    clear_attributes(node)
    node.tagName = 'a'
    node.setAttribute('href', '/search?q=' + field_name)
  for node in dom.getElementsByTagName('type-usage'):
    resolved = bool(node.getAttribute('resolved'))
    type_id = int(node.getAttribute('type-id'))
    clear_attributes(node)
    if resolved and type_id != -1:
      node.tagName = 'a'
      node.setAttribute('href', '/goto/type/%d' % type_id)
    else:
      node.tagName = 'span'
  result = dom.toxml()
  result = remove_start(result, '<?xml version="1.0" ?><source>')
  result = remove_end(result, '</source>')
  return result

def convert_offset_to_line_no(source, offset):
  line_no = 0
  for i in xrange(min(len(source), offset)):
    if source[i] == '\n':
      line_no += 1
  return line_no

def remove_start(s, t):
  if s.startswith(t):
    return s[len(t):]
  else:
    return s

def remove_end(s, t):
  if s.endswith(t):
    return s[0:len(s) - len(t)]
  else:
    return s

# vim: ts=2 sw=2 et
