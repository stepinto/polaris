from django.conf.urls import patterns, url, include

urlpatterns = patterns('',
    (r'^$', 'frontend.views.index'),
    (r'^about$', 'frontend.views.about'),
    (r'^hello$', 'frontend.views.hello'),
    (r'^search$', 'frontend.views.search'),
    (r'^source$', 'frontend.views.source'),
    (r'^ajax/complete$', 'frontend.views.ajax_complete'),
    (r'^ajax/layout$', 'frontend.views.ajax_layout'),
    (r'^static/(?P<path>.*)$', 'django.views.static.serve')
  )


# vim: ts=2 sw=2 et
