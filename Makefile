.PHONY: all

all: external/htmx.org-1.9.10.js external/missing.css-1.1.1.css external/hyperscript.org-0.9.12.js external/site.css

external/htmx.org-1.9.10.js:
	mkdir -p external
	curl -o $@ -L 'https://unpkg.com/htmx.org@1.9.10'

external/missing.css-1.1.1.css:
	mkdir -p external
	curl -o $@ -L 'https://unpkg.com/missing.css@1.1.1'

external/hyperscript.org-0.9.12.js:
	mkdir -p external
	curl -o $@ -L 'https://unpkg.com/hyperscript.org@0.9.12'

external/site.css:
	mkdir -p external
	curl -o $@ https://raw.githubusercontent.com/bigskysoftware/contact-app/master/static/site.css
