# 💬 Guia do Projeto: Bate-papo
## Microserviço de Chat Real-time

> **🎯 Contexto**: Microserviço responsável pelo sistema de chat em tempo real da plataforma, incluindo salas, mensagens, usuários online e moderação.

---

## 📋 INFORMAÇÕES DO PROJETO

### **Identificação:**
- **Nome**: conexao-de-sorte-backend-batepapo
- **Porta**: 8079
- **Rede Principal**: conexao-network-swarm
- **Database**: conexao_chat (MySQL 8.4)
- **Runner**: `[self-hosted, Linux, X64, conexao, conexao-de-sorte-backend-batepapo]`

### **Tecnologias Específicas:**
- Spring Boot 3.5.5 + Spring WebFlux (reativo)
- WebSocket + STOMP (real-time messaging)
- R2DBC MySQL (persistência reativa)
- Redis (cache mensagens + usuários online)
- Rate limiting por usuário

---

## 🗄️ ESTRUTURA DO BANCO DE DADOS

### **Database**: `conexao_chat`

#### **Tabelas:**
1. **`salas_chat`** - Salas de bate-papo
2. **`mensagens_chat`** - Mensagens enviadas
3. **`mensagens_simples_chat`** - Cache de mensagens
4. **`usuarios_online_chat`** - Status online dos usuários

#### **Relacionamentos:**
- mensagens_chat ← resposta_para_id (self-referencing)

#### **Relacionamentos Inter-Serviços:**
- mensagens_chat.usuario_id → autenticacao.usuarios.id
- salas_chat.criada_por → autenticacao.usuarios.id

### **Configuração R2DBC:**
```yaml
r2dbc:
  url: r2dbc:mysql://mysql-proxy:6033/conexao_chat
  pool:
    initial-size: 1
    max-size: 15
```

---

## 🔐 SECRETS ESPECÍFICOS

### **Azure Key Vault Secrets Utilizados:**
```yaml
# Database
conexao-de-sorte-database-r2dbc-url
conexao-de-sorte-database-username
conexao-de-sorte-database-password

# Redis Cache
conexao-de-sorte-redis-host
conexao-de-sorte-redis-password
conexao-de-sorte-redis-port

# JWT for service-to-service
conexao-de-sorte-jwt-secret
conexao-de-sorte-jwt-verification-key

# Rate Limiting
conexao-de-sorte-api-rate-limit-key

# WebSocket Security
conexao-de-sorte-session-secret
```

### **Cache Redis Específico:**
```yaml
redis:
  database: 4
  cache-names:
    - chat:mensagens
    - chat:salas
    - chat:usuarios-online
    - chat:rate-limit
```

---

## 🌐 INTEGRAÇÃO DE REDE

### **Comunicação Entrada (Server):**
- **Gateway** → Bate-papo (rotas /chat/*)
- **Frontend** → Bate-papo (WebSocket connections)
- **Moderação** → Bate-papo (admin functions)

### **Comunicação Saída (Client):**
- Bate-papo → **Autenticação** (validação JWT)
- Bate-papo → **Auditoria** (eventos de moderação)
- Bate-papo → **Notificações** (alertas de mensagens)

### **Portas e Endpoints:**
```yaml
server.port: 8079

# HTTP Endpoints:
GET    /chat/salas
POST   /chat/salas
GET    /chat/mensagens/{salaId}
POST   /chat/mensagens
DELETE /chat/mensagens/{id}
GET    /chat/usuarios-online

# WebSocket Endpoints:
/ws/chat                    # WebSocket connection
/topic/sala/{salaId}        # Subscribe to room
/app/enviar-mensagem        # Send message
/app/entrar-sala           # Join room
/app/sair-sala             # Leave room
```

---

## 🔗 DEPENDÊNCIAS CRÍTICAS

### **Serviços Dependentes (Upstream):**
1. **MySQL** (mysql-proxy:6033) - Persistência principal
2. **Redis** (conexao-redis:6379) - Cache e usuários online
3. **Autenticação** (8081) - Validação JWT
4. **Azure Key Vault** - Secrets management

### **Serviços Consumidores (Downstream):**
- **Frontend** - Interface de chat
- **Gateway** - Roteamento de mensagens
- **Auditoria** - Logs de moderação
- **Notificações** - Alertas de mensagens

### **Ordem de Deploy:**
```
1. MySQL + Redis (infrastructure)
2. Autenticação (JWT validation)
3. Bate-papo (chat service)
4. Frontend (chat interface)
```

---

## 🚨 ESPECIFICIDADES DO CHAT

### **Rate Limiting:**
- **Mensagens**: 10/minuto por usuário
- **Criação de salas**: 2/hora por usuário
- **WebSocket connections**: 5 simultâneas por usuário
- **Uploads**: 1/minuto por usuário

### **Moderação Automática:**
```yaml
moderacao:
  palavras-proibidas: true
  spam-detection: true
  flood-protection: true
  max-mensagem-length: 500
  auto-ban-threshold: 10
```

### **WebSocket Configuration:**
```yaml
websocket:
  allowed-origins: ${conexao-de-sorte-cors-allowed-origins}
  heartbeat: 30s
  disconnect-timeout: 60s
  max-connections-per-user: 5
```

---

## 📊 MÉTRICAS ESPECÍFICAS

### **Custom Metrics:**
- `chat_mensagens_total{sala,usuario}` - Mensagens enviadas
- `chat_usuarios_online` - Usuários online simultâneos
- `chat_websocket_connections` - Conexões WebSocket ativas
- `chat_salas_ativas` - Salas com atividade
- `chat_moderacao_actions{type}` - Ações de moderação
- `chat_cache_hits_total{cache_name}` - Cache hits por tipo

### **Alertas Configurados:**
- WebSocket disconnects > 10%
- Message delivery failures > 1%
- Cache miss rate > 40%
- Response time P95 > 200ms
- Spam detection triggers > 5/min

---

## 🔧 CONFIGURAÇÕES ESPECÍFICAS

### **Application Properties:**
```yaml
# Chat Configuration
chat:
  max-mensagem-length: 500
  max-salas-por-usuario: 10
  historico-mensagens: 100
  tempo-vida-sala-inativa: PT2H
  
# WebSocket
websocket:
  allowed-origins: "*"
  heartbeat-interval: 30000
  disconnect-delay: 60000
  
# Moderação
moderacao:
  enabled: true
  auto-ban: true
  palavras-filtro: classpath:palavras-proibidas.txt
  
# Rate Limiting
rate-limit:
  mensagens-por-minuto: 10
  salas-por-hora: 2
  uploads-por-minuto: 1
```

### **WebSocket Security:**
```yaml
# STOMP Configuration
stomp:
  relay:
    enabled: false  # Using in-memory broker
  application-destination-prefix: /app
  user-destination-prefix: /user
  
security:
  websocket:
    csrf-disabled: true
    jwt-required: true
```

---

## 🧪 TESTES E VALIDAÇÕES

### **Health Checks:**
```bash
# Health principal
curl -f http://localhost:8079/actuator/health

# Database connectivity
curl -f http://localhost:8079/actuator/health/db

# Redis connectivity
curl -f http://localhost:8079/actuator/health/redis

# WebSocket health
curl -f http://localhost:8079/chat/health/websocket
```

### **Smoke Tests Pós-Deploy:**
```bash
# 1. Listar salas
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8079/chat/salas

# 2. Testar WebSocket (via browser)
wscat -c ws://localhost:8079/ws/chat \
  -H "Authorization: Bearer $JWT"

# 3. Verificar usuários online
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8079/chat/usuarios-online
```

### **Load Tests WebSocket:**
```bash
# Teste de carga WebSocket
node websocket-load-test.js \
  --url ws://localhost:8079/ws/chat \
  --connections 100 \
  --duration 60s
```

---

## ⚠️ TROUBLESHOOTING

### **Problema: WebSocket Não Conecta**
```bash
# 1. Verificar origem CORS
curl -H "Origin: https://conexaodesorte.com.br" \
  http://localhost:8079/chat/health

# 2. Verificar JWT
curl -H "Authorization: Bearer $JWT" \
  http://localhost:8079/chat/salas

# 3. Logs WebSocket
docker service logs conexao-batepapo | grep WebSocket
```

### **Problema: Mensagens Perdidas**
```bash
# Cache Redis mensagens
redis-cli -a $REDIS_PASS HGETALL "chat:mensagens:sala:123"

# Verificar database
mysql -e "SELECT COUNT(*) FROM mensagens_chat WHERE sala = 'sala-123'"

# Logs delivery
docker service logs conexao-batepapo | grep "message delivery"
```

### **Problema: Performance WebSocket**
```bash
# Conexões ativas
curl http://localhost:8079/actuator/metrics/websocket.connections

# Memory usage
curl http://localhost:8079/actuator/metrics/jvm.memory.used

# GC pressure
curl http://localhost:8079/actuator/metrics/jvm.gc.pause
```

---

## 📋 CHECKLIST PRÉ-DEPLOY

### **Configuração:**
- [ ] Database `conexao_chat` criado
- [ ] Redis cache configurado (database 4)
- [ ] JWT secrets no Key Vault
- [ ] CORS origins configuradas
- [ ] Rate limiting configurado

### **WebSocket:**
- [ ] STOMP broker configurado
- [ ] Security JWT habilitada
- [ ] Heartbeat configurado
- [ ] Origins permitidas

### **Moderação:**
- [ ] Filtros de palavras configurados
- [ ] Auto-ban habilitado
- [ ] Flood protection ativo
- [ ] Spam detection funcionando

---

## 🔄 DISASTER RECOVERY

### **Backup Crítico:**
1. **Database `conexao_chat`** (mensagens e salas)
2. **Redis cache** (usuários online - pode ser perdido)
3. **Configurações de moderação**
4. **WebSocket sessions** (estado em memória - perdido)

### **Recovery Procedure:**
1. Restore database mensagens
2. Clear Redis cache (usuários online)
3. Restart chat service
4. Verify WebSocket endpoints
5. Test message delivery
6. Validate moderação rules

### **Estado Perdido Aceitável:**
- Usuários online (reconstruído em login)
- Conexões WebSocket (reconectam automaticamente)
- Cache mensagens (reload do database)

---

## 💡 OPERATIONAL NOTES

### **Escalabilidade:**
- **Horizontal**: Múltiplas instâncias com sticky sessions
- **WebSocket**: Load balancer com session affinity
- **Cache**: Redis compartilhado entre instâncias
- **Database**: Connection pooling configurado

### **Monitoramento 24/7:**
- WebSocket connection health
- Message delivery rate
- Cache hit ratio
- Spam/abuse detection
- User activity patterns

---

**📅 Última Atualização**: Setembro 2025  
**🏷️ Versão**: 1.0  
**💬 Criticidade**: ALTA - Chat em tempo real crítico para UX