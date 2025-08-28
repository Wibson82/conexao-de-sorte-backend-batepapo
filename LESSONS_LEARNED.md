# 📚 LIÇÕES APRENDIDAS - MICROSERVIÇO BATE-PAPO

> **INSTRUÇÕES PARA AGENTES DE IA:** Este arquivo contém lições aprendidas críticas deste microserviço. SEMPRE atualize este arquivo após resolver problemas, implementar correções ou descobrir melhores práticas. Use o formato padronizado abaixo.

---

## 🎯 **METADADOS DO MICROSERVIÇO**
- **Nome:** conexao-de-sorte-backend-batepapo
- **Responsabilidade:** Chat em tempo real, WebSocket, mensagens
- **Tecnologias:** Spring Boot 3.5.5, WebFlux, JPA, Java 24
- **Porta:** 8083
- **Última Atualização:** 2025-08-27

---

## ✅ **CORREÇÕES APLICADAS (2025-08-27)**

### 🔧 **1. Configuração Redis Crítica**
**Problema:** Usava `redis.url` ao invés de `spring.data.redis` + pool inadequado para chat
**Solução Antes:**
```yaml
redis:
  url: redis://redis:6379
  lettuce:
    pool:
      max-active: 8  # INADEQUADO para chat
      min-idle: 0    # SEM conexões mínimas
```
**Solução Depois:**
```yaml
spring:
  data:
    redis:
      host: redis
      database: 1  # DB 1 dedicado para chat
      lettuce:
        pool:
          max-active: 20  # Chat precisa + conexões
          min-idle: 2     # Manter conexões ativas
```
**Lição CRÍTICA:** Chat = muito mais conexões simultâneas que outros microserviços

### 🎭 **2. ReactiveRedisTemplate Conflicts**
**Problema:** Conflitos de tipos entre templates String/String vs String/Object
**Solução:** Criado CacheConfig.java com dois beans:
- `@Primary ReactiveRedisTemplate<String, String>` para cache
- `@Bean("reactiveRedisObjectTemplate") ReactiveRedisTemplate<String, Object>` para streaming

### 📊 **3. SerializationContext Corrigido**
**Problema:** Faltava `hashKey` e `hashValue` serializers
**Solução:** Adicionado `.hashKey()` e `.hashValue()` em ambos os templates

---

## 🚨 **PROBLEMAS CONHECIDOS & SOLUÇÕES**

### ❌ **Pool Redis Insuficiente para Chat**
**Sintoma:** Timeouts em Redis durante picos de usuários online
**Causa:** Chat requer MUITO mais conexões simultâneas
**Solução:** `max-active: 20` mínimo, `min-idle: 2`

### ❌ **WebSocket + Redis Integration**
**Sintoma:** Mensagens perdidas ou duplicadas
**Causa:** Redis database compartilhado com outros serviços
**Solução:** Database dedicado (`database: 1`) + pool otimizado

---

## 🎯 **BOAS PRÁTICAS IDENTIFICADAS**

### ✅ **Configuração Redis Otimizada (Chat):**
```yaml
spring:
  data:
    redis:
      database: 1  # DEDICADO para chat
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20    # Chat = + conexões
          max-idle: 8
          min-idle: 2       # SEMPRE manter ativas
          max-wait: 3000ms
```

### ✅ **CacheConfig Template Pattern:**
```java
@Bean
@Primary  // Para operações de cache simples
public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(...)

@Bean("reactiveRedisObjectTemplate")  // Para streaming complexo
public ReactiveRedisTemplate<String, Object> reactiveRedisObjectTemplate(...)
```

---

## 🔄 **HISTÓRICO DE MUDANÇAS**

### **2025-08-27**
- ✅ .dockerignore adicionado e versionado
- ✅ Rede Docker: usar host interno para DB/Redis (ex.: `conexao-mysql`, `conexao-redis`) em vez de `localhost`
- ✅ Key Vault + configtree (`/run/secrets`) para segredos; aceitar `SPRING_DATASOURCE_*`/`DB_*`
- ✅ R2DBC (r2dbc://) e Flyway (jdbc://) com URLs separadas quando aplicável
- ✅ Desabilitar Redis (auto-config/health) quando o serviço não estiver presente na rede
### **2025-08-27**
- ✅ Redis: `redis.url` → `spring.data.redis` + database=1
- ✅ Pool: max-active 8→20, min-idle 0→2 (otimizado para chat)
- ✅ Templates: Resolvido conflito String/String vs String/Object
- ✅ Serialization: Adicionado hashKey/hashValue

---

## 📋 **CHECKLIST PARA FUTURAS ALTERAÇÕES**

**Específico para Chat/WebSocket:**
- [ ] Pool Redis adequado para picos (min max-active: 20)
- [ ] Database Redis separado (database: 1)
- [ ] Templates Redis corretos (@Primary vs @Bean nomeado)
- [ ] SerializationContext completo (key, value, hashKey, hashValue)
- [ ] WebSocket timeouts adequados
- [ ] Rate limiting configurado para mensagens

**Monitoramento Chat:**
- [ ] Conexões Redis ativas durante picos
- [ ] Latência de mensagens WebSocket
- [ ] Memory usage dos templates Redis
- [ ] Rate limiting efetivo

---

## 🤖 **INSTRUÇÕES PARA AGENTES DE IA**

**ATENÇÃO ESPECIAL - MICROSERVIÇO DE CHAT:**
1. **Redis é CRÍTICO** - qualquer problema = usuários desconectados
2. **Pool settings** devem ser GENEROSOS (20+ conexões)
3. **Database separado** é obrigatório (database: 1)
4. **Templates** - sempre verificar conflitos String/Object
5. **WebSocket** - testar sob carga antes de deploy

**Quando mexer no Redis:**
- Testar com múltiplos usuários simultâneos
- Verificar latência de mensagens
- Monitorar pool exhaustion

---

*📝 Arquivo gerado automaticamente em 2025-08-27 por Claude Code*
*🚨 PRIORIDADE ALTA: Chat = serviço crítico para experiência do usuário*
