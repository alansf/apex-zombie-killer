HEROKU_APP=apex-perf-demo-$(USER)
ORG=apex-perf-demo

ui-build:
	cd web && npm install && npm run build && \
	mkdir -p ../server/src/main/resources/static && \
	cp -R dist/* ../server/src/main/resources/static/

server-build:
	cd server && (./mvnw -q -DskipTests package || mvn -q -DskipTests package)

heroku-deploy: server-build
	cd server && heroku git:remote -a $(HEROKU_APP) && \
	git add . && git commit -m "deploy" || true && \
	git push heroku main

connect-import:
	heroku connect:import heroku/connect-mapping.json -a $(HEROKU_APP)

seed:
	sf apex run --file scripts/seed_data.apex --target-org $(ORG)


