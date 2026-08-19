# customer-grpc

Projeto de estudo de gRPC em Java, usando Maven multi-módulo, `grpc-netty-shaded`,
persistência com JDBI + PostgreSQL e migrations com Flyway.

## Módulos

- **customer-grpc-contract**: `customer.proto` (mensagens `Customer`/`Address`/`CustomerStatus`
  e o serviço `CustomerService`) e a geração dos stubs Java via `protobuf-maven-plugin`.
- **customer-grpc-server**: implementação do `CustomerService`, persistindo em PostgreSQL via JDBI.
- **customer-grpc-client**: cliente de linha de comando que demonstra as chamadas ao server.

## Como rodar

### 1. Subir o PostgreSQL local

```sh
docker compose up -d
```

Isso sobe um Postgres em `localhost:5432` com o banco `customersdb` e usuário/senha
`user`/`password` (veja `docker-compose.yml`). Essas credenciais são os defaults lidos pelo server
via variáveis de ambiente (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`),
que podem ser sobrescritas se necessário.

### 2. Build

```sh
mvn install
```

Isso gera os stubs gRPC do contrato (`customer-grpc-contract/target/generated-sources`)
e builda os três módulos.

### 3. Rodar o server

```sh
mvn -pl customer-grpc-server exec:java -Dexec.mainClass=com.sonnesen.CustomerServer
```

ou, usando o jar executável gerado pelo `maven-shade-plugin`:

```sh
java -jar customer-grpc-server/target/customer-grpc-server-1.0.0-SNAPSHOT.jar
```

Na inicialização o server roda as migrations do Flyway automaticamente e sobe
o gRPC server na porta `50052` (configurável via `-Dserver.port`).

### 4. Rodar a demo do client

```sh
mvn -pl customer-grpc-client exec:java -Dexec.mainClass=com.sonnesen.Main
```

A demo cria um cliente, busca ele de volta por id e lista todos os clientes,
imprimindo os resultados no console.

### 5. Testes

```sh
mvn test
```

`customer-grpc-server` tem testes unitários (`CustomerMapperTest`) e testes de
integração que sobem um PostgreSQL descartável via [Testcontainers](https://testcontainers.com/)
(`JdbiCustomerRepositoryTest`, e `CustomerServiceImplTest` que chama as RPCs de
ponta a ponta sobre um canal in-process). É necessário ter o Docker rodando.

### 6. Explorando o server sem o client

O server expõe [gRPC reflection](https://grpc.io/docs/guides/reflection/) e o
serviço padrão `grpc.health.v1.Health`, então dá pra inspecionar/chamar as RPCs
sem precisar do `.proto` nem do client Java, por exemplo com o
[grpcurl](https://github.com/fullstorydev/grpcurl):

```sh
grpcurl -plaintext localhost:50052 list
grpcurl -plaintext localhost:50052 grpc.health.v1.Health/Check
```

## Tratamento de erros

`CustomerServiceImpl` lança exceções de domínio (`CustomerNotFoundException`,
`DuplicateEmailException`, `IllegalArgumentException`) em vez de montar `Status`
manualmente; o `ExceptionHandlingInterceptor` centraliza a tradução para
`NOT_FOUND`/`ALREADY_EXISTS`/`INVALID_ARGUMENT`/`INTERNAL`, logando o stack trace
completo só no servidor (sem vazar detalhes internos pro cliente).

## Roadmap

O plano de evolução completo (fases de qualidade/infra e novas funcionalidades:
CRUD completo com paginação, streaming, segurança/observabilidade) está descrito
à parte no planejamento do projeto.
