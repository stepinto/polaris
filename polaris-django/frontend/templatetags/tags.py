from urllib import quote
from django import template
from polaris.parser.ttypes import *
from polaris.search.ttypes import *

register = template.Library()

@register.inclusion_tag('search_result.html')
def render_search_result(hit):
  assert isinstance(hit, THit)
  title = hit.project + hit.path
  jump_target = hit.jumpTarget
  kind = '[unknown]'
  if hit.classType != None:
    kind = class_type_kind_to_str(hit.classType.kind)
  url = '/goto/source/%d?offset=%d' % (jump_target.fileId, jump_target.offset)
  return {'title': title, 'kind': kind, 'url': url, 'summary': hit.summary}

@register.inclusion_tag('search_box.html')
def render_search_box(query = ''):
  return {'query': query}

def class_type_kind_to_str(kind):
  return '[' + TClassTypeKind._VALUES_TO_NAMES[kind].lower() + ']'

# vim: ts=2 sw=2 et
