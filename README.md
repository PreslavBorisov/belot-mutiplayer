# Belot Multiplayer — incomplete prototype, not production-ready

This repository contains a Java 21 dependency-free engine with an executable smoke test, a Spring Boot HTTP room server prototype and a React/Vite UI prototype. **This is NOT a complete playable Belot.BG game.** In particular authentication, persistence, declarations, WebSockets, and correct official scoring are missing. Do not expose the server to the public internet.

## Engine smoke test (no dependencies)
```bash
mkdir -p backend/game-engine/target/classes
javac -d backend/game-engine/target/classes $(find backend/game-engine/src/main/java -name '*.java')
javac -cp backend/game-engine/target/classes -d backend/game-engine/target/classes backend/game-engine/src/test/java/bg/belot/engine/EngineSmoke.java
java -cp backend/game-engine/target/classes bg.belot.engine.EngineSmoke
```

## Local prototype (requires Maven and npm registry access)
```bash
cd backend && mvn clean package
java -jar game-server/target/game-server-0.1.0.jar
# in another terminal
cd frontend && npm install && npm run dev
```
Open http://localhost:5173. Four browser sessions may join the same room by code; **no secure identity checks** and polling is used instead of WebSockets. This has not been integration-tested.

## Deployment
No public deployment was performed. Do not deploy this prototype until security, rules and persistence gaps in RULES.md are closed.
