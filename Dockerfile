FROM clojure:temurin-21-tools-deps

WORKDIR /usr/local/lib/xtdb

ENTRYPOINT ["clojure", "-M:xtdb:nrepl"]

HEALTHCHECK --start-period=15s --timeout=3s \
    CMD curl -f http://localhost:3000/status || exit 1

EXPOSE 3000

RUN mkdir -p /var/lib/xtdb
VOLUME /var/lib/xtdb

ADD deps.edn deps.edn

RUN clojure -P

ADD src src
ADD resources resources
