.DEFAULT_GOAL := build-run

clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew clean install

run-dist:
	build/install/app/bin/app

run:
	./gradlew run

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

lint:
	./gradlew checkstyleMain checkstyleTest

refresh-deps:
	./gradlew clean build --refresh-dependencies

start:
	APP_ENV=development ./gradlew run

start-dist:
	APP_ENV=production ./build/install/app/bin/app

build-run: build run

.PHONY: build
