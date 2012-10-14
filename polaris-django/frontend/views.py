# Create your views here.

from django.http import HttpResponse
from django.shortcuts import render_to_response
from django.template.loader import render_to_string
from django.forms.widgets import Widget
from django.http import Http404
from thrift import Thrift
from thrift.transport import TTransport
from thrift.transport import TSocket
from thrift.protocol.TBinaryProtocol import TBinaryProtocolAccelerated
from polaris.search import *
from polaris.search.ttypes import TCompleteRequest
from polaris.search.ttypes import TCompleteResponse
from polaris.search.ttypes import TSearchRequest
from polaris.search.ttypes import TSearchResponse
from polaris.search.ttypes import TSearchResultEntry
from polaris.search.ttypes import TSourceRequest
from polaris.search.ttypes import TSourceResponse
from polaris.search.ttypes import TStatusCode
from polaris.token import *
from settings import RPC_SERVER_HOST
from settings import RPC_SERVER_PORT
import json
from xml.dom import minidom

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
  rpc = init_rpc()
  rpc_req = TSearchRequest()
  rpc_req.query = str(req.GET['q'])
  rpc_resp = rpc.search(rpc_req)
  check_rpc_status(rpc_resp.status)
  for e in rpc_resp.entries:
    e.fileId = hex_encode(e.fileId)
  # TODO: socket leaked
  return render_to_response('search.html', {'resp':rpc_resp})

def source(req):
  rpc = init_rpc()
  rpc_req = TSourceRequest()
  rpc_req.fileId = hex_decode(req.GET['f'])
  rpc_resp = rpc.source(rpc_req)
  check_rpc_status(rpc_resp.status)
  # f = open('/tmp/aa', 'w')
  # f.write(rpc_resp.annotations)
  # f.close()
  html = render_annotated_source(rpc_resp.annotations)
  # TODO: socket leaked
  return render_to_response('source.html', {'resp': rpc_resp, 'source_html': html})

def ajax_complete(req):
  rpc = init_rpc()
  rpc_req = TCompleteRequest()
  rpc_req.query = req.GET['q']
  rpc_req.limit = int(req.GET['n'])
  rpc_resp = rpc.complete(rpc_req)
  # TODO: socket leaked
  return HttpResponse(json.dumps(rpc_resp.entries))

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
  elif status == TStatusCode.UNKNOWN_ERROR:
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
    print 'field_name = ' + field_name
    clear_attributes(node)
    node.tagName = 'a'
    node.setAttribute('href', '/search?q=' + field_name)
  for node in dom.getElementsByTagName('type-usage'):
    type_name = node.getAttribute('type')
    resolved = bool(node.attributes['resolved'])
    clear_attributes(node)
    node.tagName = 'a'
    node.setAttribute('href', '/search?q=' + type_name)
  return dom.toxml() \
    .lstrip('<?xml version="1.0" ?><source>') \
    .rstrip('</source>')

# vim: ts=2 sw=2 et
