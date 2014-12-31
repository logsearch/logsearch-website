FROM ruby:2.1

RUN apt-get update \
  && apt-get install -y nodejs python-pygments \
  && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN gem install \
  github-pages \
  jekyll \
  jekyll-redirect-from \
  kramdown

ADD . /task/in
WORKDIR /task/in

ENTRYPOINT [ "./bin/build" ]
