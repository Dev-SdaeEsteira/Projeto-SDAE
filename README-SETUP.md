# Configuração do SDAE — Sistema de Automação de Esteiras

## Variáveis de Ambiente Obrigatórias

O sistema requer as seguintes variáveis de ambiente configuradas antes de iniciar:

### Banco de Dados MySQL

```bash
# Variáveis de conexão com o MySQL
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/sdae?createDatabaseIfNotExist=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="sua_senha_segura_aqui"
```

### Exemplo para Windows (PowerShell)

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/sdae?createDatabaseIfNotExist=true"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "sua_senha_segura_aqui"

# Depois execute:
mvn spring-boot:run
```

### Exemplo para Linux/Mac (Bash)

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/sdae?createDatabaseIfNotExist=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="sua_senha_segura_aqui"

mvn spring-boot:run
```

### Usando arquivo `.env` (opcional, com Spring Boot)

Se quiser usar um arquivo `.env`, adicione a dependência `spring-dotenv` ao `pom.xml` e crie um arquivo `.env` na raiz do projeto:

```
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sdae?createDatabaseIfNotExist=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=sua_senha_segura_aqui
```

**⚠️ Importante:** Nunca versione o arquivo `.env` ou arquivos contendo credenciais reais. Adicione-os ao `.gitignore`.

## Inicialização do Sistema

1. Certifique-se de que o MySQL está rodando
2. Configure as variáveis de ambiente conforme descrito acima
3. Execute: `mvn spring-boot:run`
4. Acesse: `http://localhost:8080`

## Segurança

- **Nunca** comita credenciais reais no repositório Git
- Use variáveis de ambiente para todas as informações sensíveis
- Em produção, utilize gerenciadores de secrets (AWS Secrets Manager, Azure Key Vault, etc.)
