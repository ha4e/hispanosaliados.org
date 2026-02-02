.PHONY: build serve clean lint plugin

build:
	bb bb/build.clj

plugin:
	cd plugins/cache-images && npm install && npm run build

serve: build
	cd public && python3 -m http.server 8000

clean:
	rm -rf public

lint:
	clj-kondo --lint bb/build.clj

help:
	@echo "Available commands:"
	@echo "  make build   - Build the static site"
	@echo "  make serve   - Build and serve locally on port 8000"
	@echo "  make clean   - Remove build output"
	@echo "  make lint    - Lint bb/build.clj with clj-kondo"
	@echo "  make plugin  - Build cache-images Netlify plugin (CLJS → JS)"
