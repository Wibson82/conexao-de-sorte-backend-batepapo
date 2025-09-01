# 🔒 VERIFICAÇÃO DE SEGURANÇA - COMANDOS PÓS-DEPLOY

## **1. VERIFICAÇÃO DE ASSINATURA DE IMAGEM (COSIGN)**

```bash
# Instalar cosign se necessário
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign

# Verificar assinatura keyless da imagem
cosign verify \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/batepapo-microservice:latest

# Verificar SBOM
cosign verify-attestation \
  --type="https://spdx.dev/Document" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/batepapo-microservice:latest

# Verificar proveniência
cosign verify-attestation \
  --type="https://slsa.dev/provenance/v1" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/batepapo-microservice:latest
```

## **2. VERIFICAÇÃO DE AUSÊNCIA DE SEGREDOS EM VARIÁVEIS DE AMBIENTE**

```bash
# Verificar que não há segredos em env vars do container
docker inspect batepapo-microservice | jq '.[]|.Config.Env[]' | \
  grep -v -E "(JAVA_OPTS|TZ|SPRING_PROFILES_ACTIVE|SERVER_PORT|ENVIRONMENT)" | \
  grep -i -E "(password|secret|key|token|credential)"

# Deve retornar vazio ou só variáveis não sensíveis
# Se encontrar algo, é uma falha de segurança
```

## **3. VERIFICAÇÃO DE PERMISSÕES DOS SECRETS**

```bash
# Verificar estrutura de diretórios de secrets
ls -la /run/secrets/batepapo/
# Deve mostrar:
# -r--------  1 root root  <size> <date> DB_PASSWORD
# -r--------  1 root root  <size> <date> REDIS_PASSWORD
# -r--------  1 root root  <size> <date> CHAT_ENCRYPTION_KEY
# etc.

# Verificar permissões específicas
stat /run/secrets/batepapo/DB_PASSWORD
# Deve mostrar: Access: (0400/-r--------) Uid: (0/root) Gid: (0/root)

# Verificar que arquivos não estão vazios
find /run/secrets/batepapo -type f -empty
# Deve retornar vazio (nenhum arquivo vazio)

# Verificar conteúdo sem expor (apenas tamanho)
wc -c /run/secrets/batepapo/* | grep -v " 0 "
# Deve mostrar arquivos com tamanho > 0
```

## **4. VERIFICAÇÃO DE ENDPOINTS ACTUATOR SEGUROS**

```bash
# Health check deve funcionar
curl -f http://localhost:8083/actuator/health
# Deve retornar: {"status":"UP"}

# Endpoints sensíveis devem estar bloqueados
curl -s http://localhost:8083/actuator/env && echo "❌ ENV ENDPOINT EXPOSTO" || echo "✅ ENV protegido"
curl -s http://localhost:8083/actuator/configprops && echo "❌ CONFIGPROPS EXPOSTO" || echo "✅ CONFIGPROPS protegido"
curl -s http://localhost:8083/actuator/beans && echo "❌ BEANS EXPOSTO" || echo "✅ BEANS protegido"
curl -s http://localhost:8083/actuator/threaddump && echo "❌ THREADDUMP EXPOSTO" || echo "✅ THREADDUMP protegido"

# Info deve funcionar (não sensível)
curl -f http://localhost:8083/actuator/info
```

## **5. VERIFICAÇÃO DE VAZAMENTO NOS LOGS**

```bash
# Verificar logs recentes não contêm secrets
docker logs batepapo-microservice --since="1h" 2>&1 | \
  grep -i -E "(password|secret|key|credential|token)" | \
  grep -v -E "(jwt.*validation|key.*rotation|secret.*loaded)" && \
  echo "❌ POSSÍVEL VAZAMENTO NOS LOGS" || echo "✅ Logs seguros"

# Verificar logs de sistema
journalctl -u docker --since="1h" | \
  grep -i -E "(password|secret|key)" && \
  echo "❌ POSSÍVEL VAZAMENTO NO SISTEMA" || echo "✅ Sistema seguro"
```

## **6. VERIFICAÇÃO DE CARREGAMENTO DO CONFIGTREE**

```bash
# Verificar que Spring está carregando secrets via configtree
docker logs batepapo-microservice 2>&1 | grep -i configtree
# Deve mostrar: "Loading configuration from configtree"

# Verificar que não há erros de carregamento de propriedades
docker logs batepapo-microservice 2>&1 | grep -i -E "(error.*property|failed.*load|configuration.*error)"
# Não deve mostrar erros relacionados a propriedades

# Verificar conexão com banco de dados funcionando
curl -f http://localhost:8083/actuator/health/db
# Deve retornar: {"status":"UP"}
```

## **7. VERIFICAÇÃO ESPECÍFICA DE CHAT/WEBSOCKET**

```bash
# Verificar endpoint de saúde do WebSocket
curl -f http://localhost:8083/rest/v1/chat/health
# Deve retornar status das funcionalidades de chat

# Verificar conectividade com Redis
curl -f http://localhost:8083/actuator/health/redis
# Deve retornar: {"status":"UP"}

# Testar endpoint de WebSocket info (sem se conectar)
curl -s http://localhost:8083/rest/v1/chat/info | jq .
# Deve retornar informações sobre capacidade, usuários online, etc.

# Verificar métricas de WebSocket (se habilitadas)
curl -s http://localhost:8083/actuator/metrics/websocket.connections.current
curl -s http://localhost:8083/actuator/metrics/chat.messages.sent.total
```

## **8. VERIFICAÇÃO DE CONECTIVIDADE REDIS**

```bash
# Se tiver acesso direto ao Redis, testar conectividade
redis-cli -h <redis-host> -p <redis-port> -a <redis-password> ping
# Deve retornar: PONG

# Verificar se Redis está sendo usado para sessões de chat
redis-cli -h <redis-host> -p <redis-port> -a <redis-password> keys "chat:*" | wc -l
# Deve mostrar número de chaves relacionadas ao chat

# Verificar TTL das sessões de chat
redis-cli -h <redis-host> -p <redis-port> -a <redis-password> ttl "chat:session:*"
```

## **9. TESTE DE FUNCIONALIDADE WEBSOCKET**

```bash
# Testar conexão WebSocket com wscat (instalar se necessário)
# npm install -g wscat
wscat -c ws://localhost:8083/rest/v1/chat/websocket
# Deve conectar sem erros

# Testar autenticação WebSocket com JWT
wscat -c "ws://localhost:8083/rest/v1/chat/websocket" \
  -H "Authorization: Bearer <valid-jwt-token>"
# Deve conectar e permitir envio de mensagens
```

## **10. VERIFICAÇÃO DE CONECTIVIDADE JWT**

```bash
# Testar endpoint protegido com JWT válido
curl -H "Authorization: Bearer <test-jwt-token>" \
  http://localhost:8083/rest/v1/chat/rooms
# Deve retornar lista de salas disponíveis ou erro 401 sem token

# Testar criação de sala de chat
curl -X POST http://localhost:8083/rest/v1/chat/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{"name":"Teste","description":"Sala de teste","type":"PUBLIC"}'
```

## **11. VERIFICAÇÃO DE ROTAÇÃO DE CHAVES**

```bash
# Verificar data de criação das chaves JWT no Key Vault
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-jwt-key-id" --query "attributes.created" -o tsv

# Verificar próxima data de rotação
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-encryption-master-key" \
  --query "attributes.expires" -o tsv

# Verificar chaves específicas do chat
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-chat-encryption-key" \
  --query "attributes.created" -o tsv

# Verificar secrets do Redis
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-redis-password" \
  --query "attributes.created" -o tsv
```

## **12. SCRIPT DE VERIFICAÇÃO COMPLETA**

```bash
#!/bin/bash
# verify-security-batepapo.sh - Script de verificação completa

set -euo pipefail

echo "🔒 VERIFICAÇÃO COMPLETA DE SEGURANÇA - BATEPAPO"
echo "=============================================="

# 1. Verificar container está rodando
if ! docker ps | grep -q batepapo-microservice; then
    echo "❌ Container não está rodando"
    exit 1
fi
echo "✅ Container está rodando"

# 2. Verificar health
if curl -f -s http://localhost:8083/actuator/health > /dev/null; then
    echo "✅ Health check passou"
else
    echo "❌ Health check falhou"
    exit 1
fi

# 3. Verificar endpoints sensíveis bloqueados
if curl -f -s http://localhost:8083/actuator/env > /dev/null; then
    echo "❌ Endpoint /env está exposto"
    exit 1
else
    echo "✅ Endpoint /env está protegido"
fi

# 4. Verificar secrets existem e têm permissões corretas
if [[ ! -d "/run/secrets/batepapo" ]]; then
    echo "❌ Diretório de secrets não existe"
    exit 1
fi

for secret in DB_PASSWORD REDIS_PASSWORD CHAT_ENCRYPTION_KEY ENCRYPTION_MASTER_KEY; do
    if [[ ! -f "/run/secrets/batepapo/$secret" ]]; then
        echo "❌ Secret $secret não existe"
        exit 1
    fi
    
    PERMS=$(stat -c "%a" "/run/secrets/batepapo/$secret")
    if [[ "$PERMS" != "400" ]]; then
        echo "❌ Secret $secret tem permissões incorretas: $PERMS"
        exit 1
    fi
done
echo "✅ Todos os secrets existem com permissões corretas"

# 5. Verificar não há vazamento em env vars
if docker inspect batepapo-microservice | jq '.[]|.Config.Env[]' | \
   grep -i -E "(password|secret|key)" | \
   grep -v -E "(JAVA_OPTS|SPRING_|TZ)" > /dev/null; then
    echo "❌ Possível vazamento em variáveis de ambiente"
    exit 1
else
    echo "✅ Nenhum segredo em variáveis de ambiente"
fi

# 6. Verificar funcionalidades específicas de chat
echo "💬 Verificando funcionalidades de chat..."

# Testar conectividade Redis
if curl -f -s http://localhost:8083/actuator/health/redis > /dev/null; then
    echo "✅ Conectividade Redis funcionando"
else
    echo "⚠️ Redis não acessível (pode ser normal se não configurado)"
fi

# Testar endpoint de info do chat
if curl -f -s http://localhost:8083/rest/v1/chat/info > /dev/null; then
    echo "✅ Endpoint de info do chat funcionando"
else
    echo "⚠️ Endpoint de info do chat não disponível (pode ser normal se não implementado)"
fi

# 7. Verificar portas WebSocket
if netstat -tuln | grep -q ":8083"; then
    echo "✅ Porta 8083 está aberta para WebSocket"
else
    echo "❌ Porta 8083 não está aberta"
    exit 1
fi

echo ""
echo "🎉 VERIFICAÇÃO COMPLETA: TODAS AS CHECAGENS PASSARAM"
echo "✅ Sistema de chat está seguro e em conformidade"
```

## **13. MONITORAMENTO CONTÍNUO**

```bash
# Configurar alertas para expiração de chaves (crontab)
0 9 * * * /usr/local/bin/check-key-expiration-batepapo.sh

# Script de monitoramento de expiração específico para chat
cat > /usr/local/bin/check-key-expiration-batepapo.sh << 'EOF'
#!/bin/bash
VAULT_NAME="kv-conexao-de-sorte"
DAYS_WARNING=30

# Verificar chaves específicas de chat/Redis
SECRETS=("conexao-de-sorte-redis-password" "conexao-de-sorte-chat-encryption-key" "conexao-de-sorte-moderation-api-key")

for SECRET in "${SECRETS[@]}"; do
    EXPIRES=$(az keyvault secret show --vault-name "$VAULT_NAME" \
      --name "$SECRET" --query "attributes.expires" -o tsv 2>/dev/null)
    
    if [[ -n "$EXPIRES" ]]; then
        EXPIRES_EPOCH=$(date -d "$EXPIRES" +%s)
        NOW_EPOCH=$(date +%s)
        DAYS_LEFT=$(( (EXPIRES_EPOCH - NOW_EPOCH) / 86400 ))
        
        if [[ $DAYS_LEFT -le $DAYS_WARNING ]]; then
            echo "⚠️ ALERTA: Secret $SECRET expira em $DAYS_LEFT dias!"
            # Enviar alerta (email, Slack, etc.)
        fi
    fi
done
EOF
chmod +x /usr/local/bin/check-key-expiration-batepapo.sh
```

## **14. TESTES DE FUNCIONALIDADE ESPECÍFICA**

```bash
# Testar criação de sala de chat (requer autenticação)
curl -X POST http://localhost:8083/rest/v1/chat/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "name": "Teste Segurança",
    "description": "Sala de teste para verificação de segurança",
    "type": "PUBLIC",
    "maxUsers": 10
  }'

# Testar listagem de salas
curl -H "Authorization: Bearer <valid-jwt-token>" \
  http://localhost:8083/rest/v1/chat/rooms

# Testar envio de mensagem
curl -X POST http://localhost:8083/rest/v1/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "roomId": "test-room-id",
    "content": "Mensagem de teste de segurança",
    "type": "TEXT"
  }'

# Verificar histórico de mensagens
curl -H "Authorization: Bearer <valid-jwt-token>" \
  "http://localhost:8083/rest/v1/chat/messages?roomId=test-room-id&limit=10"

# Verificar métricas de chat
curl -s http://localhost:8083/actuator/metrics/chat.active.connections
curl -s http://localhost:8083/actuator/metrics/chat.messages.processed.total
curl -s http://localhost:8083/actuator/metrics/chat.errors.total
```

## **15. VERIFICAÇÃO DE MODERAÇÃO E RATE LIMITING**

```bash
# Testar rate limiting (deve bloquear após muitas tentativas)
for i in {1..20}; do
  curl -X POST http://localhost:8083/rest/v1/chat/messages \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <valid-jwt-token>" \
    -d '{"roomId":"test","content":"spam test '$i'","type":"TEXT"}' \
    -w "Response: %{http_code}\n"
done

# Testar moderação de conteúdo (deve bloquear conteúdo impróprio)
curl -X POST http://localhost:8083/rest/v1/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{
    "roomId": "test-room",
    "content": "teste de moderação com palavras impróprias",
    "type": "TEXT"
  }'

# Verificar logs de moderação
docker logs batepapo-microservice --since="10m" | grep -i "moderation\|blocked\|filtered"
```
