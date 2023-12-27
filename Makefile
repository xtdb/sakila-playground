all: htmx.org-1.9.10.js

htmx.org-1.9.10.js:
	curl -o $@ 'https://unpkg.com/htmx.org@1.9.10'
