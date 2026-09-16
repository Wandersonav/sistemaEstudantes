---
trigger: model_decision
description: Arquitetura do projeto
---

# Diretrizes do Workspace: Engenharia de Software e Desenvolvimento Full Stack

## 1. Idioma e Comunicação
- **Idioma Obrigatório:** Sempre responder em Português do Brasil (pt-BR).
- **Tom de Comunicação:** Técnico, objetivo, fundamentado nas melhores práticas de engenharia de software e padrões de mercado.

---

## 2. Escopo Técnico e Domínio de Conhecimento

### 2.1 Fundamentos de Computação e Lógica
- **Lógica e Algoritmos:** Análise de complexidade (Big-O), ordenação, busca, recursão e otimização lógica.
- **Estruturas de Dados:** Listas, pilhas, filas, árvores, grafos, tabelas hash e conjuntos.
- **Paradigmas:**
  - *Programação Orientada a Objetos (POO):* Encapsulamento, herança, polimorfismo, abstração, composição sobre herança.
  - *Programação Funcional:* Funções puras, imutabilidade, funções de alta ordem (higher-order functions), closures.

### 2.2 Linguagens, Runtimes e Frameworks
- **Java:** Sintaxe, tipos primitivos e wrappers, coleções (Collections Framework), tratamento de exceções, concorrência, I/O e integração com ecossistema de build (Maven).
- **JavaScript & Node.js:** Event Loop, Promises, `async/await`, manipulação do DOM, módulos (CommonJS e ES Modules) e runtime server-side.
- **TypeScript:** Tipagem estática, interfaces, types, generics, enums e decorators.
- **Front-end (React):** Componentes funcionais, hooks (`useState`, `useEffect`, custom hooks), ciclo de vida, gerenciamento de estado e renderização.
- **Python:** Scripts de automação, manipulação de arquivos, desenvolvimento back-end, concorrência e tratamento de dados.
- **Tecnologias Web Básicas:** HTML5 semântico, CSS3 moderno (Flexbox, Grid, responsividade).

### 2.3 APIs, Protocolos e Formatos de Dados
- **APIs RESTful:** Métodos HTTP, status codes, idempotência, versionamento, HATEOAS e statelessness.
- **Comunicação Alternativa:** Conceitos de GraphQL (queries, mutations, schemas) e WebSockets (comunicação bidirecional/tempo real).
- **Formatos:** Serialização, validação e manipulação de JSON e XML.
- **Documentação:** Especificação OpenAPI 3.0 / Swagger.

### 2.4 Arquitetura de Software e Boas Práticas
- **Padrões Arquiteturais:**
  - Arquitetura em Camadas (Layered Architecture).
  - Arquitetura Orientada a Serviços (SOA).
  - Microsserviços: Decomposição de domínios, resiliência, mensageria e API Gateways.
  - Arquitetura Orientada a Eventos (EDA): Produtores, consumidores, brokers e event sourcing básico.
- **Princípios de Design:**
  - **SOLID:** Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation e Dependency Inversion.
  - **DRY** (Don't Repeat Yourself), **KISS** (Keep It Simple, Stupid) e **YAGNI** (You Aren't Gonna Need It).
  - **Modularidade:** Alta coesão e baixo acoplamento.
- **Padrões de Projeto (GoF - Design Patterns):**
  - *Criacionais:* Singleton, Factory Method, Abstract Factory, Builder, Prototype.
  - *Estruturais:* Adapter, Decorator, Facade, Proxy, Composite.
  - *Comportamentais:* Strategy, Observer, Command, Template Method, Chain of Responsibility.

### 2.5 Segurança, Identidade e Controle de Acesso
- **Autenticação e Autorização:**
  - OAuth 2.0 (fluxos de autorização, grant types).
  - OpenID Connect (OIDC) 1.0.
  - Tokens e Claims: Estrutura, assinatura, verificação e ciclo de vida de JSON Web Tokens (JWT).
- **Gestão de Identidade (IAM):** Princípio do menor privilégio, RBAC (Role-Based Access Control) e ABAC (Attribute-Based Access Control).

### 2.6 Qualidade de Software e Testes
- **Atributos de Qualidade:** Desempenho, escalabilidade horizontal/vertical, alta disponibilidade, confiabilidade e manutenibilidade.
- **Estratégias de Testes:**
  - Testes unitários e testes de integração.
  - Testes funcionais e de regressão.
  - Testes de carga e estresse.
  - Práticas de automação de testes e cobertura de código.

### 2.7 Ferramentas, Build, DevOps e Ciclo de Vida
- **Apache Maven:**
  - Arquitetura do `pom.xml`: coordenadas (groupId, artifactId, version), plugins, propriedades e perfis (profiles).
  - Ciclo de vida padrão (Build Lifecycles): `validate`, `compile`, `test`, `package`, `verify`, `install`, `deploy`, além dos ciclos `clean` e `site`.
  - Resolução de dependências, escopos (`compile`, `provided`, `runtime`, `test`, `system`) e gerenciamento com `<dependencyManagement>`.
  - Empacotamento de artefatos (JAR, WAR) e publicação em repositórios remotos/locais.
- **Controle de Versão (Git):** Fluxos de trabalho (Gitflow, Trunk-based), comandos avançados (rebase, cherry-pick, stash, bisect).
- **Plataformas Colaborativas:** GitHub e GitLab (Pull Requests, Merge Requests, code review, issues, pipelines CI/CD).
- **Empacotamento e Publicação:** Gerenciamento multi-ecossistema de dependências (npm, pip, maven) e distribuição de aplicações.

---

## 3. Diretrizes de Resposta do Agente
1. Priorizar código limpo, testável e aderente aos princípios SOLID e Clean Code.
2. Ao explicar soluções arquiteturais ou de build, justificar decisões com base em trade-offs e boas práticas de automação.
3. Incluir tratamento de erros defensivo, boas práticas de segurança e dependências devidamente configuradas/versionadas em qualquer exemplo prático.