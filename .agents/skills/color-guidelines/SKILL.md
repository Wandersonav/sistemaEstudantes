---
name: color-guidelines
description: >-
  Diretrizes e paleta oficial de cores do Sistema de Estudantes (Swing e Web).
  Utilize esta skill sempre que for criar, estilizar ou ajustar componentes de interface,
  gráficos, temas, estados de sessão ou elementos visuais do sistema.
---

# Diretrizes de Cores do Sistema

Guia oficial para padronização cromática e identidade visual da aplicação (Desktop Swing e Web).

---

## 1. Visão Geral e Princípios

**Conceito:** visual sereno, limpo e arejado, inspirado em névoa, água geotérmica e céu nublado. Muito espaço em branco, contraste suave e transições esfumaçadas entre as cores. A paleta é baseada em **branco e azul-acinzentado**, com um único acento saturado (azul-petróleo).

**Princípios:**
- **Proporção sugerida:** 70% branco, 20% azul-acinzentado claro, 7% cinza-grafite (texto), 3% azul-petróleo (acentos).
- **Contraste:** texto grafite sobre fundos claros; nunca usar texto branco sobre azul-lagoa claro. Manter contraste mínimo WCAG AA (4.5:1) para texto corrente.
- **Gradientes:** sempre suaves, do azul-lagoa para o branco, simulando névoa. Evitar transições bruscas.
- **Sobreposições:** blocos em azul-gelo com opacidade parcial (60–80%) sobre imagens ou fundo branco.
- **Tipografia:** sans-serif de traço limpo, títulos em semibold e textos em peso leve, na cor grafite.
- **Sensação geral:** calma, pureza, bem-estar, tecnologia leve e profissional. Evitar cores quentes ou saturadas, exceto o azul-petróleo pontual e as cores semânticas da seção 3.
- O azul-lagoa (`#9CC5DE` a `#7FB3D1`) é decorativo: usar em degradês, ilustrações e fundos, **nunca para texto**.

---

## 2. Paleta Base e Fundos (Tokens de Interface)

| Elemento | Token / Variável | Hex / RGB | Uso / Descrição |
| :--- | :--- | :--- | :--- |
| Background da Aplicação | `--color-bg-app` | `#FFFFFF` / rgb(255, 255, 255) | Fundo principal da janela/página |
| Fundo Alternado (branco-névoa) | `--color-bg-alt` | `#F4F7FA` / rgb(244, 247, 250) | Seções alternadas e destaques suaves |
| Superfície / Card | `--color-bg-surface` | `#FFFFFF` / rgb(255, 255, 255) | Fundo de painéis e cartões (usar com borda ou sombra leve) |
| Painel Azul-gelo | `--color-bg-panel` | `#E3ECF3` / rgb(227, 236, 243) | Painéis, caixas de destaque, faixas translúcidas |
| Azul-névoa | `--color-bg-mist` | `#D3E2EC` / rgb(211, 226, 236) | Blocos sobrepostos, hover de painéis, cabeçalhos de tabela |
| Azul-lagoa (decorativo) | `--color-lagoon` | `#9CC5DE` / rgb(156, 197, 222) | Gradientes, ilustrações, fundos (nunca texto) |
| Azul-lagoa profundo | `--color-lagoon-deep` | `#7FB3D1` / rgb(127, 179, 209) | Fim de gradientes, elementos decorativos |
| Borda Padrão | `--color-border` | `#D3E2EC` / rgb(211, 226, 236) | Divisórias e bordas de containers |
| Texto Principal (títulos) | `--color-text-title` | `#3E4042` / rgb(62, 64, 66) | Títulos e textos de destaque |
| Texto Corrente | `--color-text-body` | `#5A5D60` / rgb(90, 93, 96) | Parágrafos, subtítulos, rótulos |

---

## 3. Cores Semânticas e Estados

| Estado | Token / Constante | Cor | Significado |
| :--- | :--- | :--- | :--- |
| Primária / Acento | `--color-primary` | `#3A7D8C` | Botões principais, links ativos, ícones, setas, estados de interação |
| Primária (hover) | `--color-primary-hover` | `#2F6672` | Hover e pressed de botões e links |
| Sucesso | `--color-success` | `#4C9A7A` | Sessão concluída, confirmações (verde suave, em harmonia com o petróleo) |
| Alerta / Atenção | `--color-warning` | `#D9A441` | Avisos, pendências (âmbar dessaturado) |
| Perigo / Erro | `--color-danger` | `#C65D5D` | Cancelamentos, ações destrutivas (vermelho suave, nunca saturado) |

> As cores de sucesso, alerta e perigo foram escolhidas com saturação reduzida para não quebrar a calma da paleta. O azul-petróleo é o único acento saturado da identidade.

---

## 4. Ciclo Pomodoro

- **Modo Foco:** `#3A7D8C` (azul-petróleo), concentração e ação.
- **Pausa Curta (Short Break):** `#7FB3D1` (azul-lagoa profundo), respiro leve.
- **Pausa Longa (Long Break):** `#9CC5DE` (azul-lagoa), descanso amplo. Usar texto grafite `#3E4042` sobre este fundo.

---

## 5. Heatmap de Atividade (Níveis de Intensidade)

Escala monocromática do branco-névoa ao azul-petróleo, intensificando conforme o tempo de estudo.

- **Nível 0 (Sem estudo):** `#EEF2F6`
- **Nível 1 (1 - 59 min):** `#D3E2EC`
- **Nível 2 (60 - 119 min):** `#9CC5DE`
- **Nível 3 (120 - 179 min):** `#5A9FB5`
- **Nível 4 (180+ min):** `#3A7D8C`

---

## 6. Cores das Disciplinas (Subjects)

Cores de disciplinas devem ser **suaves e dessaturadas**, na família azul-acinzentada, para manter a harmonia visual. Sugestão de mapeamento (ajustar conforme as matérias cadastradas):

| Ordem | Cor | Hex |
| :--- | :--- | :--- |
| 1 | Azul-petróleo | `#3A7D8C` |
| 2 | Azul-lagoa | `#7FB3D1` |
| 3 | Verde-sálvia | `#7FA99B` |
| 4 | Cinza-azulado | `#8A9BAA` |
| 5 | Azul-aço | `#5F7F99` |
| 6 | Areia suave | `#C9B99A` |

**Regras:**
- Evitar cores quentes saturadas e neons.
- Texto sobre a cor da disciplina deve ter contraste adequado: grafite `#3E4042` em tons claros, branco `#FFFFFF` apenas em tons escuros (petróleo, azul-aço).
- Cada disciplina mantém a mesma cor em todo o sistema (gráficos, listas, calendário, heatmap por matéria).

---

## 7. Instruções Adicionais

**Tema Claro (Light) — padrão:**
- Fundo `#FFFFFF`, alternado `#F4F7FA`, painéis `#E3ECF3`.
- Texto em grafite (`#3E4042` / `#5A5D60`), acento `#3A7D8C`.

**Tema Escuro (Dark) — sugestão derivada:**
- Fundo da aplicação `#1B2329`, superfície/card `#242E36`, borda `#34424C`.
- Texto principal `#E6EDF2`, texto corrente `#B5C2CC`.
- Acento primário clareado para `#5FB0C0`, mantendo contraste mínimo de 4.5:1.
- Semânticas levemente clareadas: sucesso `#6DBB9A`, alerta `#E6B85C`, perigo `#D97878`.
- Heatmap invertido: nível 0 `#242E36` até nível 4 `#5FB0C0`.

**Swing (Desktop):**
- Definir as cores como constantes `Color` centralizadas em uma classe única (ex.: `AppColors`), nunca valores hexadecimais soltos nos componentes.
- Aplicar via `UIManager` ou LookAndFeel quando possível.

**Web:**
- Declarar os tokens como variáveis CSS em `:root` e redefinir sob `[data-theme="dark"]`.

**Observação:** os valores da paleta base foram estimados visualmente a partir da imagem de referência. As cores semânticas, do Pomodoro, heatmap, disciplinas e tema escuro são sugestões coerentes com a paleta e podem ser ajustadas conforme a necessidade.