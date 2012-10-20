from urllib import quote
from django import template
from polaris.search.ttypes import TSearchResultEntry
from polaris.token.ttypes import TTokenKind

register = template.Library()

@register.inclusion_tag('search_result.html')
def render_search_result(search_result):
  assert isinstance(search_result, TSearchResultEntry)
  title = search_result.projectName + search_result.fileName
  kind = kind_to_str(search_result.kind)
  # url = '/source?f=' + search_result.fileId + '&o=' + str(search_result.offset);
  url = '/source?project=%s&path=%s&o=%d' % (quote(search_result.projectName),
    quote(search_result.fileName), search_result.offset)
  summary = search_result.summary
  return {'title': title, 'kind': kind, 'url': url, 'summary': summary}

@register.inclusion_tag('search_box.html')
def render_search_box(query = ''):
  return {'query': query}

def kind_to_str(kind):
  if kind == TTokenKind.CLASS_DECLARATION:
    return '[class]'
  if kind == TTokenKind.INTERFACE_DECLARATION:
    return '[interface]'
  if kind == TTokenKind.ENUM_DECLARATION:
    return '[enum]'
  if kind == TTokenKind.ANNOTATION_DECLARATION:
    return '[annotation]'
  elif kind == TTokenKind.METHOD_DECLARATION:
    return '[method]'
  elif kind == TTokenKind.FIELD_DECLARATION:
    return '[field]'
  else:
    return '[misc]'

# vim: ts=2 sw=2 et
