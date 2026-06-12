# SUMMARY.md — Estado do Projeto

## Última Fase Concluída
**Fase 1 — Fundação do Projeto e Infraestrutura Local**

## O que foi implementado
- Estrutura de monorepo com `backend/` (Spring Boot 3.3.5, Java 21)
- `pom.xml` com dependências: web, actuator, data-jpa, flyway-core, flyway-database-postgresql, postgresql
- Multi-stage `Dockerfile` (build: `maven:3.9-eclipse-temurin-21-alpine` → runtime: `eclipse-temurin:21-jre-alpine`)
- `docker-compose.yml` com três serviços na network `saas-network`:
  - `postgres:16-alpine` com healthcheck via `pg_isready` e volume persistente
  - `pgadmin4` acessível em `localhost:5050`
  - `backend` com `depends_on: condition: service_healthy` no Postgres
- Profiles do Spring:
  - `application.yml` — base: actuator expõe só `/health`, `open-in-view: false`, ddl-auto: validate
  - `application-dev.yml` — localhost:5432, show-sql, clean-on-validation-error, DEBUG logging
  - `application-docker.yml` — postgres:5432 (hostname Docker), INFO logging
- Flyway com migration `V1__init.sql` (habilita extensão `uuid-ossp`)
- `.gitignore` configurado

## Decisões Técnicas

### Multi-stage Dockerfile
Imagem final ~200 MB vs ~600 MB com JDK completo. A layer cache do pom.xml separado do src evita re-download de dependências em rebuilds.

### depends_on com condition: service_healthy
Sem a condição, o Spring Boot tenta conectar ao Postgres antes de aceitar conexões e falha no startup. O healthcheck com `pg_isready` garante que o banco está pronto.

### flyway-database-postgresql separado
Flyway 10+ separou os drivers por banco de dados. Sem esse artefato, o Flyway não reconhece a URL JDBC do PostgreSQL.

### open-in-view: false
Evita o antipattern de lazy loading fora da transação (N+1 silencioso). Erros de acesso fora de transação aparecem explicitamente.

### Actuator restrito a /health
Apenas o endpoint `health` exposto. `info`, `env`, `beans`, etc. ficam fechados — não vazam detalhes de infraestrutura.

## Próximos Passos
**Fase 2 — Multi-tenancy com Row Level Security**
- Tabela `organizations` como tenant root (migration V2)
- RLS policies no PostgreSQL via migration V3
- `TenantContext` (ThreadLocal) no Spring
- `TenantFilter` (Servlet Filter) que executa `SET LOCAL app.current_tenant`
- Testes de integração com Testcontainers provando isolamento entre tenants

## Débitos Técnicos
- Maven Wrapper não gerado: rodar `mvn wrapper:wrapper` em `backend/` para gerar `mvnw` e usar `./mvnw spring-boot:run`
- pgAdmin sem servidor pré-cadastrado: cadastrar `postgres` / porta `5432` manualmente na primeira abertura em `localhost:5050`
