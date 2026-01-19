.PHONY: build serve clean

build:
	bb bb/build.clj

serve: build
	cd public && python3 -m http.server 8080

clean:
	rm -rf public

help:
	@echo "Available commands:"
	@echo "  make build  - Build the static site"
	@echo "  make serve  - Build and serve locally on port 8080"
	@echo "  make clean  - Remove build output"
