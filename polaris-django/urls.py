from django.conf.urls import patterns, url, include

urlpatterns = patterns('',
    (r'^$', 'frontend.views.index'),
    (r'^index2$', 'frontend.views.index2'),
    (r'^about$', 'frontend.views.about'),
    (r'^hello$', 'frontend.views.hello'),
    (r'^search$', 'frontend.views.search'),
    (r'^ajax/complete$', 'frontend.views.ajax_complete'),
    (r'^ajax/layout/(?P<project>\w+)/(?P<path>.*)$', 'frontend.views.ajax_layout'),
    (r'^ajax/search$', 'frontend.views.ajax_search'),
    (r'^goto/source/(?P<file_id>\d+)$', 'frontend.views.goto_source_by_id'),
    (r'^goto/source/(?P<project>\w+)/(?P<path>.*)$', 'frontend.views.goto_source_by_path'),
    (r'^goto/type/(?P<type_id>\d+)$', 'frontend.views.goto_type'),
    (r'^goto/field/(?P<field_id>\d+)$', 'frontend.views.goto_field'),
    (r'^goto/method/(?P<method_id>\d+)$', 'frontend.views.goto_method'),
    (r'^static/(?P<path>.*)$', 'django.views.static.serve')
  )


# vim: ts=2 sw=2 et
