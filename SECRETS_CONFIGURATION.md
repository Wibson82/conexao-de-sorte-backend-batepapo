# 🔐 Configuração de Segredos - Microserviço Bate-Papo

## ✅ Status da Padronização
Todos os arquivos de configuração foram atualizados para usar os segredos padronizados conforme especificado.

## 📋 Segredos do GitHub (GitHub Secrets)

Configure os seguintes segredos no GitHub Actions:

### 🔑 Autenticação Azure
```
AZURE_CLIENT_ID         # ID do service principal para OIDC
AZURE_TENANT_ID         # ID do tenant Azure
AZURE_SUBSCRIPTION_ID   # ID da subscription Azure
AZURE_KEYVAULT_NAME     # Nome do Azure Key Vault
```

### 🐳 Docker Registry
```
DOCKER_REGISTRY         # URL do registry (ex: ghcr.io)
DOCKER_USERNAME         # Username para o registry
DOCKER_PASSWORD         # Token/senha para o registry
```

## 🏦 Segredos do Azure Key Vault

Configure os seguintes segredos no Azure Key Vault:

### 🗄️ Database (MySQL/R2DBC)
```
conexao-de-sorte-database-r2dbc-url    # URL R2DBC: r2dbc:pool:mysql://host:3306/database
conexao-de-sorte-database-username     # Username do banco
conexao-de-sorte-database-password     # Senha do banco
conexao-de-sorte-database-jdbc-url     # URL JDBC/Flyway: jdbc:mysql://host:3306/database
```

### 🔐 JWT Authentication
```
conexao-de-sorte-jwt-jwks-uri          # URI para JWKS
conexao-de-sorte-jwt-issuer            # Issuer do JWT
conexao-de-sorte-jwt-signing-key       # Chave para assinatura
conexao-de-sorte-jwt-verification-key  # Chave para verificação
conexao-de-sorte-jwt-key-id           # ID da chave JWT
conexao-de-sorte-jwt-secret           # Secret JWT
```

### 🚀 Redis Cache
```
conexao-de-sorte-redis-host            # Host do Redis
conexao-de-sorte-redis-port            # Porta do Redis (padrão: 6379)
conexao-de-sorte-redis-password        # Senha do Redis
conexao-de-sorte-redis-database        # Número do database Redis
```

### 🌐 Server Configuration
```
conexao-de-sorte-server-port           # Porta do servidor (padrão: 8083)
```

### 🛡️ CORS Configuration
```
conexao-de-sorte-cors-allowed-origins  # Origens permitidas para CORS
```

## 📁 Arquivos Atualizados

### ✅ Configuração Principal
- `src/main/resources/application.yml` - Configuração base
- `src/main/resources/application-azure.yml` - Configuração Azure
- `src/main/resources/application-staging.yml` - Configuração Staging
- `src/main/resources/application-prod.yml` - Configuração Produção

### ✅ Pipeline CI/CD
- `.github/workflows/ci-cd.yml` - GitHub Actions workflow

### ✅ Docker
- `docker-compose.yml` - Configuração local com variáveis padronizadas

### ✅ Scripts
- `scripts/setup-secrets.sh` - Script para configurar segredos (já atualizado)

## 🔄 Mapeamento de Variáveis

### Desenvolvimento Local (docker-compose.yml)
```yaml
environment:
  - conexao-de-sorte-database-r2dbc-url=${DATABASE_URL}
  - conexao-de-sorte-database-username=${DATABASE_USER}
  - conexao-de-sorte-database-password=${DATABASE_PASSWORD}
  - conexao-de-sorte-redis-host=${REDIS_HOST}
  - conexao-de-sorte-redis-port=${REDIS_PORT}
  - conexao-de-sorte-redis-password=${REDIS_PASSWORD}
  - conexao-de-sorte-redis-database=${REDIS_DATABASE}
```

### Azure Container Instances
As variáveis são automaticamente injetadas via configtree em `/run/secrets/`

### GitHub Actions
As variáveis são recuperadas do Azure Key Vault e configuradas como environment variables.

## 🧪 Verificação

Para verificar se os segredos estão corretos:

1. **Localmente:**
   ```bash
   docker-compose up -d
   curl http://localhost:8079/rest/v1/chat/actuator/health
   ```

2. **Staging:**
   ```bash
   ./scripts/verify-staging.sh
   ```

3. **Produção:**
   ```bash
   curl https://api.conexaodesorte.com.br/rest/v1/chat/actuator/health
   ```

## 🚨 Notas Importantes

1. **Segurança:** Nunca commite valores reais de segredos no código
2. **Ambientes:** Cada ambiente pode ter valores diferentes para os mesmos segredos
3. **Backup:** Mantenha backup dos segredos em local seguro
4. **Rotação:** Considere rotacionar segredos periodicamente
5. **Acesso:** Limite o acesso aos segredos apenas aos serviços necessários

## 📞 Troubleshooting

### Erro de Conexão com Database
- Verifique `conexao-de-sorte-database-r2dbc-url`
- Confirme `conexao-de-sorte-database-username` e `conexao-de-sorte-database-password`

### Erro de Redis
- Verifique `conexao-de-sorte-redis-host` e `conexao-de-sorte-redis-port`
- Confirme `conexao-de-sorte-redis-password`

### Erro de JWT
- Verifique `conexao-de-sorte-jwt-jwks-uri`
- Confirme `conexao-de-sorte-jwt-issuer`

### Erro de CORS
- Verifique `conexao-de-sorte-cors-allowed-origins`
- Confirme se as origens estão corretas para o ambiente
