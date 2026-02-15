FROM eclipse-temurin:17-jdk-alpine AS builder
RUN apk add --no-cache bash make
WORKDIR /app
COPY . .
RUN make -C app build
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/app/build/distributions/app-1.0-SNAPSHOT.tar .
RUN mkdir -p dist && \
    tar -xvf app-1.0-SNAPSHOT.tar -C dist --strip-components=1 && \
    rm app-1.0-SNAPSHOT.tar && \
    ln -s dist/bin/app /app/app
EXPOSE 5000
CMD ["./app"]
