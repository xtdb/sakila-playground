all: external/htmx.org-1.9.10.js external/missing.css-1.1.1.js

external/htmx.org-1.9.10.js:
	mkdir -p external
	curl -o $@ -L 'https://unpkg.com/htmx.org@1.9.10'

external/missing.css-1.1.1.js:
	mkdir -p external
	curl -o $@ -L 'https://unpkg.com/missing.css@1.1.1'
