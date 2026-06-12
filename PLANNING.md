# Multi-tenant SaaS Dashboard — Planejamento por Fases

> **Objetivo:** Construir um painel SaaS multi-tenant completo com isolamento de dados por organização, roles/permissions, integração Stripe, e deploy com Docker + AWS.
>
> **Stack:** Spring Boot · Angular · PostgreSQL (Row Level Security) · Stripe · Docker · AWS
>
> **Regra de ouro:** Não avance para a próxima fase sem concluir e commitar a anterior. Cada fase tem um entregável verificável.

---

## Fase 1 — Fundação do Projeto e Infraestrutura Local

**O que será feito:**
- Inicializar o monorepo com estrutura `backend/` (Spring Boot) e `frontend/` (Angular)
- Configurar Docker Compose com PostgreSQL 16, pgAdmin e a aplicação Spring Boot
- Criar `Dockerfile` otimizado para o backend (multi-stage build)
- Configurar profiles do Spring (`application.yml`, `application-dev.yml`, `application-docker.yml`)
- Implementar health check endpoint (`/actuator/health`)
- Criar o `docker-compose.yml` com network isolada e volumes persistentes
- Configurar Flyway para versionamento de migrações do banco

**Entregável:** `docker compose up` sobe o sistema completo e o health check responde `200 OK`.

**Commit:** `feat: initialize project structure with Docker and Spring Boot`

---

## Fase 2 — Multi-tenancy com Row Level Security

**O que será feito:**
- Projetar o schema multi-tenant: tabela `organizations` como tenant root
- Implementar Row Level Security (RLS) no PostgreSQL com políticas por `tenant_id`
- Criar migration Flyway que ativa RLS e cria as policies
- Implementar `TenantContext` (ThreadLocal) no Spring Boot para propagar o tenant
- Criar `TenantFilter` (Servlet Filter) que seta `app.current_tenant` na sessão do PostgreSQL via `SET LOCAL`
- Criar interceptor JPA/Hibernate que injeta automaticamente o `tenant_id` em queries
- Escrever testes de integração que provam o isolamento (Tenant A não vê dados do Tenant B)

**Entregável:** Teste automatizado que insere dados em dois tenants e prova que RLS bloqueia acesso cruzado.

**Commit:** `feat: implement multi-tenant isolation with PostgreSQL RLS`

---

## Fase 3 — Autenticação e Autorização (JWT + RBAC)

**O que será feito:**
- Implementar registro de usuário e organização (`POST /api/auth/register`)
- Implementar login com geração de JWT (`POST /api/auth/login`)
- Criar sistema de roles: `OWNER`, `ADMIN`, `MEMBER`, `VIEWER`
- Implementar `@PreAuthorize` com custom security expressions por role
- Criar endpoint de convite de membro à organização com token de convite
- Implementar refresh token com rotação
- Configurar Spring Security filter chain completa
- Escrever testes para cada nível de permissão

**Entregável:** Fluxo completo de registro → login → acesso por role funcionando via API, com testes.

**Commit:** `feat: add JWT authentication with role-based access control`

---

## Fase 4 — API Core e CRUD de Recursos do Tenant

**O que será feito:**
- Criar entidades de domínio do dashboard: `Project`, `Member`, `ActivityLog`
- Implementar CRUD completo com validação (`@Valid`, Bean Validation)
- Adicionar paginação e ordenação nos endpoints de listagem
- Implementar filtros de busca com Specification pattern
- Criar DTOs com MapStruct para separar camada de API da persistência
- Adicionar audit trail automático (`created_at`, `updated_at`, `created_by`)
- Implementar tratamento global de exceções (`@ControllerAdvice`) com respostas padronizadas
- Documentar API com SpringDoc/OpenAPI (Swagger UI)

**Entregável:** Swagger UI acessível com todos os endpoints documentados e funcionais, dados isolados por tenant.

**Commit:** `feat: implement core API with CRUD, validation and OpenAPI docs`

---

## Fase 5 — Integração Stripe (Billing e Subscription Tiers)

**O que será feito:**
- Configurar Stripe SDK e chaves via variáveis de ambiente
- Criar modelo de subscription tiers: `FREE`, `PRO`, `ENTERPRISE`
- Implementar Stripe Checkout Session para upgrade de plano
- Implementar webhook handler (`POST /api/stripe/webhook`) com verificação de assinatura
- Tratar eventos: `checkout.session.completed`, `invoice.paid`, `invoice.payment_failed`, `customer.subscription.deleted`
- Criar `SubscriptionService` que atualiza o tier da organização baseado no status do Stripe
- Implementar feature flags por tier (ex: FREE = 3 projetos, PRO = 50, ENTERPRISE = ilimitado)
- Criar endpoint `GET /api/billing/status` para consulta do plano atual

**Entregável:** Fluxo completo de checkout → webhook → atualização de plano funcionando em modo Stripe Test.

**Commit:** `feat: integrate Stripe billing with subscription tiers`

---

## Fase 6 — Frontend Angular (Dashboard UI)

**O que será feito:**
- Inicializar projeto Angular com Angular CLI e configurar proxy para o backend
- Implementar módulo de autenticação: login, registro, guards de rota
- Criar layout principal com sidebar, header com info do tenant e user
- Implementar dashboard com cards de métricas e gráficos (Chart.js ou ng2-charts)
- Criar módulo de gerenciamento de membros (convidar, alterar role, remover)
- Implementar página de billing mostrando plano atual e botão de upgrade (redirect para Stripe)
- Criar interceptor HTTP para injetar JWT e tratar 401 (redirect para login)
- Implementar lazy loading por módulo e responsive design

**Entregável:** Dashboard funcional no browser acessando o backend, com login, visualização de dados por tenant e página de billing.

**Commit:** `feat: implement Angular dashboard with auth and billing UI`

---

## Fase 7 — Testes, Segurança e Qualidade

**O que será feito:**
- Backend: testes unitários (JUnit 5 + Mockito) com cobertura ≥ 80%
- Backend: testes de integração com Testcontainers (PostgreSQL real)
- Frontend: testes unitários com Jasmine/Karma para componentes críticos
- Implementar rate limiting com Bucket4j ou Resilience4j
- Adicionar CORS configurável, CSRF protection, security headers (CSP, HSTS)
- Configurar análise estática com SpotBugs ou SonarQube (via Docker)
- Implementar logging estruturado com correlação de request ID
- Criar `SECURITY.md` documentando decisões de segurança

**Entregável:** Pipeline de testes verde, relatório de cobertura ≥ 80%, `SECURITY.md` completo.

**Commit:** `feat: add comprehensive testing, security hardening and quality gates`

---

## Fase 8 — CI/CD, Docker Production e Deploy AWS

**O que será feito:**
- Criar GitHub Actions workflow: build → test → análise → Docker build → push
- Configurar multi-stage Docker build otimizado para produção (JRE slim)
- Criar `docker-compose.prod.yml` com configurações de produção
- Configurar deploy para AWS (ECS Fargate ou EC2 com Docker Compose)
- Configurar RDS PostgreSQL com backups automáticos
- Implementar health checks e readiness probes
- Configurar variáveis de ambiente e secrets via AWS Parameter Store ou Secrets Manager
- Escrever `README.md` de portfólio com arquitetura, screenshots, e instruções de setup

**Entregável:** Push para `main` dispara pipeline completa, aplicação rodando em AWS com URL pública.

**Commit:** `feat: add CI/CD pipeline and AWS deployment`

---

## Resumo Rápido

| Fase | Foco                          | Entregável-chave                              |
|------|-------------------------------|-----------------------------------------------|
| 1    | Fundação e Docker             | `docker compose up` funcional                 |
| 2    | Multi-tenancy + RLS           | Teste de isolamento passando                  |
| 3    | Auth JWT + RBAC               | Fluxo registro → login → role                 |
| 4    | API Core + CRUD               | Swagger UI com todos endpoints                |
| 5    | Stripe Billing                | Checkout → webhook → plano atualizado         |
| 6    | Frontend Angular              | Dashboard funcional no browser                |
| 7    | Testes + Segurança            | Cobertura ≥ 80% + SECURITY.md                 |
| 8    | CI/CD + AWS                   | Deploy automático com URL pública              |

---

## Como usar

Diga ao Claude qual fase deseja iniciar. Exemplo:

> "Vamos iniciar a Fase 1"

O Claude vai explicar a arquitetura e decisões primeiro, e só vai gerar código quando você pedir explicitamente.
