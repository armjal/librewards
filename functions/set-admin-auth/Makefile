.PHONY: zip
zip:
	uv export --no-emit-workspace --no-default-groups --output-file src/requirements.txt --no-header --quiet --no-hashes
	cd src && zip -r ../function.zip .
	rm src/requirements.txt
