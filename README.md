# 💬 Microserviço de Bate-papo

[![Java](https://img.shields.io/badge/Java-24-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg)](https://spring.io/projects/spring-boot)
[![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-blue.svg)](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
[![R2DBC](https://img.shields.io/badge/R2DBC-Reactive-purple.svg)](https://r2dbc.io/)
[![WebSocket](https://img.shields.io/badge/WebSocket-RealTime-red.svg)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
[![SSE](https://img.shields.io/badge/SSE-Streaming-yellow.svg)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

Microserviço **100% reativo** para bate-papo em tempo real, construído com Spring WebFlux, Server-Sent Events (SSE), WebSockets e Redis Streams.

## 🎯 Características Principais

- **💬 Chat em Tempo Real**: SSE + WebSocket para comunicação instantânea
- **🚀 100% Reativo**: Spring WebFlux + R2DBC para máxima performance
- **🔐 Segurança JWT**: Validação via JWKS do microserviço de autenticação
- **📡 Streaming Distribuído**: Redis Streams para broadcasting entre instâncias
- **👥 Presença de Usuários**: Controle de usuários online com heartbeat
- **🏠 Gestão de Salas**: Criação e gerenciamento de salas de chat
- **📈 Observabilidade**: Actuator + Prometheus + Grafana
- **⚡ Cache Inteligente**: Redis distribuído + Caffeine local
- **🐳 Containerizado**: Docker + Docker Compose
- **📊 API Documentada**: OpenAPI 3 + Swagger UI
- **🧪 Testado**: Testes unitários e de integração
- **🔄 Anti-extração**: Mantém funcionalidade no monólito

## 🏗️ Arquitetura

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │◄──▶│ Microserviço     │───▶│    MySQL        │
│   (WebSocket/   │    │   Bate-papo      │    │   (R2DBC)       │
│     SSE)        │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │                         │
                              ▼                         ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   Redis Streams  │    │   Flyway        │
                       │   (Broadcasting) │    │  (Migrations)   │
                       └──────────────────┘    └─────────────────┘
```

## 🛠️ Stack Tecnológica

- **Java 24** - Linguagem principal
- **Spring Boot 3.5+** - Framework base
- **Spring WebFlux** - Programação reativa
- **Spring Security** - Segurança JWT
- **R2DBC MySQL** - Acesso reativo ao banco
- **Redis Streams** - Broadcasting distribuído
- **Server-Sent Events** - Streaming de eventos
- **WebSockets** - Comunicação bidirecional
- **Caffeine** - Cache local
- **Flyway** - Migrations de banco
- **Docker** - Containerização
- **Testcontainers** - Testes de integração

## 🚀 Início Rápido

### Pré-requisitos

- Java 24+
- Docker e Docker Compose
- Maven 3.9+

### 1. Clone e Execute

```bash
# Clone o projeto
cd /Volumes/NVME/Projetos/conexao-de-sorte-backend-batepapo

# Execute com Docker Compose
docker-compose up -d

# Ou execute localmente
mvn spring-boot:run
```

### 2. Acesse os Serviços

- **API**: http://localhost:8083/rest/v1/chat
- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **Actuator**: http://localhost:8083/actuator
- **Grafana**: http://localhost:3002 (admin:admin123!)
- **Prometheus**: http://localhost:9092

## 📋 Endpoints da API

### Mensagens e Chat

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/rest/v1/chat/mensagem` | Enviar mensagem |
| `GET` | `/rest/v1/chat/mensagens/{sala}` | Histórico paginado |
| `PUT` | `/rest/v1/chat/mensagem/{id}` | Editar mensagem |
| `DELETE` | `/rest/v1/chat/mensagem/{id}` | Excluir mensagem |

### Streaming em Tempo Real

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/rest/v1/chat/stream/{sala}` | SSE stream de mensagens |
| `GET` | `/rest/v1/chat/stream/{sala}/eventos` | SSE stream completo |
| `GET` | `/rest/v1/chat/stream/presenca` | Stream global de presença |

### Salas e Usuários

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/rest/v1/chat/salas` | Listar salas disponíveis |
| `POST` | `/rest/v1/chat/salas` | Criar nova sala |
| `GET` | `/rest/v1/chat/online/{sala}` | Usuários online |
| `POST` | `/rest/v1/chat/entrar/{sala}` | Entrar em sala |
| `DELETE` | `/rest/v1/chat/sair/{sala}` | Sair de sala |
| `PUT` | `/rest/v1/chat/heartbeat` | Atualizar heartbeat |

## 🎮 Exemplos de Uso

### Enviar Mensagem

```bash
curl -X POST "http://localhost:8083/rest/v1/chat/mensagem" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "conteudo": "Olá pessoal!",
    "sala": "geral"
  }'
```

### Conectar ao Stream SSE

```javascript
const eventSource = new EventSource(
  'http://localhost:8083/rest/v1/chat/stream/geral',
  {
    headers: {
      'Authorization': 'Bearer YOUR_JWT_TOKEN'
    }
  }
);

eventSource.onmessage = function(event) {
  const chatEvent = JSON.parse(event.data);
  console.log('Nova mensagem:', chatEvent);
};
```

### WebSocket Connection

```javascript
const socket = new WebSocket('ws://localhost:8083/ws/chat');

socket.onopen = function() {
  console.log('Conectado ao chat');
  
  // Enviar mensagem
  socket.send(JSON.stringify({
    tipo: 'NOVA_MENSAGEM',
    conteudo: 'Olá via WebSocket!',
    sala: 'geral'
  }));
};

socket.onmessage = function(event) {
  const mensagem = JSON.parse(event.data);
  console.log('Mensagem recebida:', mensagem);
};
```

## 🔧 Configuração

### Variáveis de Ambiente

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=conexao_sorte_batepapo
DB_USERNAME=batepapo_user
DB_PASSWORD=batepapo_pass123!

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_pass123!

# JWT
JWT_JWKS_URI=http://localhost:8081/.well-known/jwks.json
JWT_ISSUER=https://auth.conexaodesorte.com

# Features
FEATURE_BATEPAPO_MS=true
FEATURE_WEBSOCKET=true
FEATURE_SSE=true
FEATURE_MESSAGE_PERSISTENCE=true
FEATURE_REDIS_STREAMS=true
```

### Chat Configuration

```yaml
chat:
  max-message-length: 500
  max-rooms-per-user: 5
  message-retention-days: 30
  online-timeout-minutes: 5
  heartbeat-interval-seconds: 30
  max-connections-per-user: 3
```

## 🧪 Testes

```bash
# Testes unitários
mvn test

# Testes de integração
mvn verify

# Testes com Testcontainers
mvn test -Dtest=**/*IntegrationTest
```

## 📊 Monitoramento

### Métricas Customizadas

- `chat.mensagens.total` - Total de mensagens enviadas
- `chat.usuarios.online` - Usuários online em tempo real  
- `chat.conexoes.sse` - Conexões SSE ativas
- `chat.salas.ativas` - Salas com atividade

### Health Checks

- **Database**: Conectividade R2DBC
- **Redis**: Streams e cache disponível
- **Chat**: Status das salas e usuários

## 🚀 Performance

### Características de Performance

- **Streaming Reativo**: SSE não-bloqueante
- **Broadcasting Distribuído**: Redis Streams para múltiplas instâncias
- **Cache Inteligente**: Mensagens frequentes em cache
- **Connection Pooling**: R2DBC otimizado
- **Rate Limiting**: Proteção contra spam

### Configurações de Produção

```yaml
# Performance otimizada
spring:
  r2dbc:
    pool:
      initial-size: 20
      max-size: 100
      
redis:
  streams:
    block-duration: 5000
    max-pending: 1000
    
rate-limit:
  endpoints:
    "/rest/v1/chat/mensagem":
      requests-per-minute: 10
```

## 🔐 Segurança

- **JWT Validation**: Via JWKS endpoint
- **Rate Limiting**: Por usuário e endpoint
- **CORS**: Configurado para WebSocket/SSE
- **Security Headers**: CSP adaptado para streaming
- **Input Validation**: Sanitização de mensagens
- **Anti-Spam**: Controle de frequência

## 🐳 Docker

### Build Local

```bash
# Build da imagem
docker build -t batepapo-microservice .

# Executar container
docker run -p 8083:8083 \
  -e FEATURE_BATEPAPO_MS=true \
  -e REDIS_HOST=redis \
  batepapo-microservice
```

### Docker Compose Completo

```bash
# Subir ambiente completo
docker-compose up -d

# Ver logs em tempo real
docker-compose logs -f batepapo-service

# Parar ambiente
docker-compose down
```

## 🔄 Integração com Monólito

- **Feature Flag**: `FEATURE_BATEPAPO_MS=false` por padrão
- **Anti-extração**: Funcionalidade preservada no monólito
- **Migração Gradual**: Ativação por feature flag
- **Rollback Seguro**: Desativação instantânea

## 🎯 Casos de Uso

### Chat Público
- Salas abertas para todos os usuários
- Mensagens em tempo real via SSE
- Histórico persistido no banco

### Chat Privado
- Salas com acesso restrito
- Moderação automática
- Controle de presença

### Suporte ao Cliente
- Sala dedicada para suporte
- Persistência de conversas
- Métricas de atendimento

## 🤝 Contribuição

1. Clone o repositório
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Faça commit: `git commit -m 'Adiciona chat em grupo'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.

---

**💬 Microserviço Bate-papo** - Sistema de Migração R2DBC v1.0
## ✅ Qualidade e Segurança (CI)

- Cobertura: JaCoCo ≥ 80% (gate no workflow Maven Verify).
- SAST: CodeQL ativo.

## 🧪 Staging: Integrações, Robustez e Cache

- Integrações: Autenticação (JWT via JWKS), Redis Streams/Sessions — validar fluxo.
- Resilience4j: validar CB/Retry para operações externas.
- Cache: validar TTLs e comportamento sob carga (WebSocket e REST).
