from django.conf.urls import patterns, url, include

urlpatterns = patterns('',
    (r'^$', 'frontend.views.index'),
    (r'^about$', 'frontend.views.about'),
    (r'^hello$', 'frontend.views.hello'),
    (r'^search$', 'frontend.views.search'),
    (r'^ajax/complete$', 'frontend.views.ajax_complete'),
    (r'^ajax/layout/(?P<project>\w+)/(?P<path>.*)$$', 'frontend.views.ajax_layout'),
    (r'^goto/source/(?P<file_id>\d+)$', 'frontend.views.goto_source_by_id'),
    (r'^goto/source/(?P<project>\w+)/(?P<path>.*)$', 'frontend.views.goto_source_by_path'),
    (r'^goto/type/(?P<type_id>\d+)$', 'frontend.views.goto_type'),
    (r'^static/(?P<path>.*)$', 'django.views.static.serve')
  )


# vim: ts=2 sw=2 et
