---
name: design-guidelines
description: >-
  Diretrizes de design, layout, tipografia e componentes do Sistema de Estudantes (Swing e Web).
  Utilize esta skill sempre que for criar, estilizar ou refatorar telas, formulários, cards,
  botões, inputs, menus laterais, cabeçalhos, tabelas ou qualquer elemento de interface,
  e quando for pedido para padronizar o visual conforme a tela de Planejamento & Agenda de Estudos.
  As cores vêm da skill color-guidelines; esta skill define todo o restante.
---

# Diretrizes de Design do Sistema

Guia oficial de estrutura visual, hierarquia e componentes da aplicação (Desktop Swing e Web). Todas as cores devem vir dos tokens da skill `color-guidelines`; esta skill não define cores novas.

---

## 1. Visão Geral e Princípios

**Conceito:** interface moderna, limpa e minimalista, em light mode, com muito espaço em branco e hierarquia visual clara. A tela de referência é "Planejamento & Agenda de Estudos": sidebar à esquerda, cabeçalho de página e conteúdo em cards arredondados.

**Princípios:**
- **Menos é mais:** cada tela tem um título, uma ação principal e o restante em segundo plano.
- **Espaço em branco:** respeitar a escala de espaçamento; evitar blocos densos e bordas desnecessárias.
- **Hierarquia:** tamanho, peso e cor guiam o olhar (título > rótulo > texto de apoio).
- **Consistência:** mesmos raios, alturas, espaçamentos e ícones em todo o sistema; reutilizar componentes antes de criar novos.
- **Acento único:** apenas `--color-primary` como cor de ação; sem gradientes fortes, sombras pesadas ou cores extras.
- **Usabilidade:** rótulos sempre visíveis, estados claros (foco, hover, desabilitado, erro) e textos em português do Brasil.

---

## 2. Tipografia

**Fonte:** Plus Jakarta Sans (pesos 400, 500, 600, 700). Fallback: `system-ui, sans-serif` (Web) e `SansSerif` (Swing).

| Elemento | Token / Variável | Tamanho | Peso | Cor |
| :--- | :--- | :--- | :--- | :--- |
| Título da página (h1) | `--text-h1` | 26px | 700, letter-spacing -0.02em | `--color-text-title` |
| Título de card (h2) | `--text-h2` | 18px | 700, letter-spacing -0.01em | `--color-text-title` |
| Nome do app / destaque | `--text-lg` | 16px | 700 | `--color-text-title` |
| Corpo, inputs, menu | `--text-md` | 14px | 500 | `--color-text-title` |
| Rótulo de campo | `--text-label` | 13px | 600 | `--color-text-title` |
| Texto de apoio, subtítulo | `--text-sm` | 12–14px | 400–500 | `--color-text-body` |
| Eyebrow (ex.: "MÓDULOS") | `--text-eyebrow` | 11px | 600, caixa alta, letter-spacing 0.08em | `--color-text-body` |

---

## 3. Espaçamento, Forma e Elevação (Tokens)

| Elemento | Token / Variável | Valor | Uso / Descrição |
| :--- | :--- | :--- | :--- |
| Escala de espaçamento | `--space-1` a `--space-12` | 4, 8, 12, 16, 20, 24, 28, 32, 40, 48px | Múltiplos de 4px; nunca usar valores fora da escala |
| Raio pequeno | `--radius-sm` | 10px | Inputs, botões, itens de menu |
| Raio grande | `--radius-lg` | 16px | Cards e painéis |
| Raio pílula | `--radius-pill` | 999px | Badges e pills de status |
| Sombra de card | `--shadow-card` | `0 1px 2px rgba(62,64,66,.04), 0 8px 24px rgba(62,64,66,.04)` | Elevação sutil de cards |
| Altura de controle | `--control-h` | 44px (inputs), 40px (botões do header), 46px (botão primário de formulário) | Altura mínima de alvos clicáveis |

---

## 4. Layout

- **Estrutura:** sidebar fixa de 264px (`--color-bg-surface`, borda direita `--color-border`) + coluna principal sobre `--color-bg-alt`.
- **Cabeçalho da página:** fundo `--color-bg-surface`, padding 28px 48px, borda inferior; título e subtítulo à esquerda, ações à direita (tema, status, ação rápida).
- **Conteúdo:** padding 40px 48px; cards com `max-width` de ~1000px.
- **Card:** fundo `--color-bg-surface`, borda 1px `--color-border`, `--radius-lg`, padding 32px, `--shadow-card`. Cabeçalho do card com h2 e subtítulo (margem inferior 28px).
- **Grids de formulário:** `grid-template-columns: repeat(auto-fit, minmax(170px, 1fr))` com gap de 20px; campos longos em `minmax(280px, 1fr)`.
- **Rodapé de ação:** separado por borda superior, opção/checkbox à esquerda e botão primário à direita.
- **Responsivo:** abaixo de 860px a sidebar é ocultada (ou vira menu recolhível); grids empilham; tabelas largas rolam dentro do próprio container.

---

## 5. Componentes

### 5.1 Input, Select, Data e Hora

| Propriedade | Valor |
| :--- | :--- |
| Altura / padding | 44px / 0 14px |
| Borda | 1px `--color-border` (foco: `--color-primary`) |
| Raio / fundo | `--radius-sm` / `--color-bg-surface` |
| Texto | 14px, peso 500, `--color-text-title` |
| Placeholder | `--color-text-body` |
| Rótulo | Sempre acima do campo (gap de 8px); nunca usar o placeholder como rótulo |
| Select | Sem aparência nativa + chevron de 16px à direita (padding-right 40px) |
| Foco | Anel de 3px com `--color-primary` a 25% de opacidade |
| Erro | Borda `--color-danger` e mensagem de 12px abaixo do campo |

### 5.2 Botões

| Tipo | Estilo | Uso |
| :--- | :--- | :--- |
| Primário | Fundo `--color-primary`, texto branco, 14px/600, `--radius-sm`, ícone de 16–18px à esquerda; hover `--color-primary-hover` | Uma única ação principal por área visível |
| Secundário | Fundo `--color-bg-surface`, borda `--color-border`, texto `--color-text-title`, 13px/600 | Ações de apoio (ex.: tema, cancelar) |
| Perigo | Texto/borda `--color-danger`, fundo branco | Ações destrutivas |
| Desabilitado | Opacidade 50%, sem sombra, cursor padrão | Ação indisponível |

### 5.3 Pills e Badges de Status

| Variante | Cores | Exemplo |
| :--- | :--- | :--- |
| Sucesso / online | Texto `--color-success`, fundo `--color-success` a 12% | "API conectada", "Ativo" |
| Neutro (contador) | Texto `--color-text-body`, fundo `--color-bg-panel` | "8", "0d", "25 min" |
| Alerta | Texto `--color-warning` escurecido, fundo `--color-warning` a 15% | Pendências |
| Perigo | Texto `--color-danger`, fundo `--color-danger` a 12% | Cancelado |

Formato: `--radius-pill`; no cabeçalho com 40px de altura, em listas compacto (padding 2px 8px, 12px/600). Pills de sucesso levam um ponto de 8px à esquerda.

### 5.4 Sidebar

- Logo de 40px com `--radius-sm` em `--color-primary`, nome do app em 16px/700 e subtítulo em 12px.
- Itens: padding 11px 12px, `--radius-sm`, ícone de 20px, texto de 14px, badge alinhado à direita.
- **Ativo:** fundo `--color-bg-panel`, texto `--color-primary`, peso 600.
- **Inativo:** texto `--color-text-body`; hover com fundo `--color-bg-alt`.

### 5.5 Checkbox e Opções

18px, cor de destaque `--color-primary`; título de 14px/600 e linha de apoio de 12px (`--color-text-body`) abaixo.

### 5.6 Tabelas e Listas

- Cabeçalho de 12px/600 em `--color-text-body`, fundo `--color-bg-mist` ou transparente.
- Linhas com divisória de 1px `--color-border`, altura mínima de 52px, hover `--color-bg-alt`.
- Ações de linha como botões secundários pequenos ou ícones com `aria-label`.

---

## 6. Ícones

- Estilo de traço (stroke) de 1.8px, linhas arredondadas (família Lucide/Feather), `currentColor`.
- Tamanhos: 20px no menu, 16–18px em botões, 16px em campos.
- Nunca usar emoji na interface.

---

## 7. Estados e Interação

- **Foco:** sempre visível (anel de 3px); nunca remover o outline sem substituto.
- **Hover:** mudança sutil de cor ou fundo (`--color-primary-hover`, `--color-bg-alt`); sem animações chamativas.
- **Transições:** 120–180ms, `ease-out`, apenas em cor, fundo e sombra.
- **Carregamento:** indicar com spinner discreto no botão e desabilitar a ação durante o envio.
- **Vazio:** texto de apoio em `--color-text-body` e uma ação clara; sem ilustrações pesadas.

---

## 8. Acessibilidade

- Usar elementos semânticos (`<button>`, `<a>`, `<label for>`); botões só com ícone levam `aria-label`.
- Contraste mínimo de 4.5:1 para texto corrente (3:1 a partir de 24px), conforme `color-guidelines`.
- Alvos clicáveis com no mínimo 40px de altura.
- Não depender apenas da cor para comunicar estado: acompanhar de texto ou ícone.

---

## 9. Instruções Adicionais

**Web:**
- Importar a fonte e declarar espaçamentos, raios e sombras como variáveis CSS em `:root`, junto aos tokens de cor.
- Tema escuro: redefinir apenas variáveis sob `[data-theme="dark"]`, sem alterar os componentes.

**Swing (Desktop):**
- Centralizar medidas em uma classe única (ex.: `AppMetrics`) com raios, espaçamentos e alturas; cores continuam em `AppColors`.
- Cantos arredondados via `Border` customizado (ex.: `RoundedBorder(radius)`) ou `FlatLaf`, quando disponível.
- Fonte Plus Jakarta Sans carregada com `Font.createFont`; fallback `SansSerif`.

**Regras gerais:**
- Ao alterar telas existentes, manter comportamento e lógica; alterar apenas estrutura visual e estilos.
- Não criar valores soltos (cores, px, raios) fora dos tokens.

**Observação:** os valores desta skill foram extraídos do redesenho da tela "Planejamento & Agenda de Estudos". As cores são herdadas da skill `color-guidelines`; medidas, pesos e raios podem ser ajustados conforme a necessidade.

---

## 10. Checklist Antes de Finalizar

- [ ] Nenhuma cor, raio, sombra ou espaçamento fora dos tokens
- [ ] Inputs com 44px e rótulos acima
- [ ] Apenas um botão primário por área
- [ ] Estados implementados: hover, foco, desabilitado e erro
- [ ] Layout validado em ~1440px e ~390px (Web) ou redimensionando a janela (Swing)
