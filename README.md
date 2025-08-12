# 📚 Simulador de Processador Monociclo - Documentação Completa

## 🎯 Introdução

Este documento descreve um *Simulador de Processador Monociclo* desenvolvido em Java que implementa uma arquitetura hipotética de 16 bits. O simulador executa programas em formato binário e demonstra claramente todas as etapas do pipeline monociclo.

### Características Principais:
- *Arquitetura*: 16 bits
- *Registradores*: 8 registradores (R0-R7)
- *Memória*: Separada em instruções e dados
- *Pipeline*: Monociclo com 5 etapas
- *ISA*: 12 instruções implementadas
- *Debug*: Sistema completo de visualização

---

## 🏗️ Arquitetura do Processador

### Especificações Técnicas:
- *Largura da palavra*: 16 bits
- *Número de registradores*: 8 (R0 a R7)
- *Tamanho da memória*: 64KB (65.536 endereços)
- *Formato das instruções*: R-Type e I-Type

### Banco de Registradores:

R0: Registrador para syscalls
R1: Registrador de propósito geral / parâmetro syscall
R2: Registrador de propósito geral
R3: Registrador de propósito geral
R4: Registrador de propósito geral
R5: Registrador de propósito geral
R6: Registrador de propósito geral
R7: Registrador de propósito geral


---

## 🔄 Monociclo

O processador implementa um pipeline monociclo com *5 etapas* executadas sequencialmente:

### 1. *FETCH* 🎯
- *Função*: Buscar a próxima instrução da memória
- *Entrada*: Program Counter (PC)
- *Saída*: Instrução de 16 bits
- *Operação*: instrução = memória[PC]

### 2. *DECODE* 🔍
- *Função*: Decodificar a instrução e extrair campos
- *Entrada*: Instrução de 16 bits
- *Saída*: Campos decodificados (opcode, registradores, imediato)
- *Operação*: Análise dos bits para determinar tipo e campos

### 3. *EXECUTE* ⚡
- *Função*: Executar a operação
- *Entrada*: Campos decodificados + valores dos registradores
- *Saída*: Resultado da operação
- *Operação*: ALU, comparações, preparação de endereços

### 4. *MEMORY ACCESS* 💾
- *Função*: Acessar memória (se necessário)
- *Entrada*: Endereço e dados (para store) ou endereço (para load)
- *Saída*: Dados lidos (para load)
- *Operação*: load ou store na memória de dados

### 5. *WRITE BACK* ✍️
- *Função*: Escrever resultado nos registradores
- *Entrada*: Resultado da operação + índice do registrador
- *Saída*: Registrador atualizado
- *Operação*: registrador[índice] = resultado

---

## 📋 Conjunto de Instruções (ISA)

### Formato das Instruções:

#### *Tipo R* (bit 15 = 0):

15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
 0 |    opcode(6)   | rd(3) | rs1(3)| rs2(3) |


#### *Tipo I* (bit 15 = 1):

15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
 1 | op(2) | rd(3) |      imediato(10)          |


### Instruções Implementadas:

| Instrução | Tipo | Opcode | Formato | Descrição |
|-----------|------|--------|---------|-----------|
| add | R | 0 | add rd, rs1, rs2 | rd = rs1 + rs2 |
| sub | R | 1 | sub rd, rs1, rs2 | rd = rs1 - rs2 |
| mul | R | 2 | mul rd, rs1, rs2 | rd = rs1 * rs2 |
| div | R | 3 | div rd, rs1, rs2 | rd = rs1 / rs2 |
| cmp_equal | R | 4 | cmp_equal rd, rs1, rs2 | rd = (rs1 == rs2) ? 1 : 0 |
| cmp_neq | R | 5 | cmp_neq rd, rs1, rs2 | rd = (rs1 != rs2) ? 1 : 0 |
| load | R | 15 | load rd, rs1 | rd = mem[rs1] |
| store | R | 16 | store rs1, rs2 | mem[rs1] = rs2 |
| syscall | R | 63 | syscall | Chama serviço do sistema |
| jump | I | 0 | jump addr | PC = addr |
| jump_cond | I | 1 | jump_cond rd, addr | se rd == 1: PC = addr |
| mov | I | 3 | mov rd, imm | rd = imm |

### Syscalls Implementadas:

| Serviço | R0 | Descrição | Parâmetros |
|---------|----|-----------| -----------|
| Terminar | 0 | Encerra o programa | - |
| Print String | 1 | Imprime string | R1 = endereço da string |
| Print Newline | 2 | Imprime nova linha | - |
| Print Integer | 3 | Imprime inteiro | R1 = valor |
| Sleep | 6 | Pausa execução | R1 = segundos |
| Get Time | 7 | Obtém timestamp | Retorna em R1 |

---

## 🔧 Classes e Responsabilidades

### *Main.java*
- *Responsabilidade*: Interface com o usuário
- *Funcionalidades*:
  - Solicitar caminho do arquivo binário
  - Configurar modo debug
  - Inicializar componentes
  - Tratar exceções principais

### *Processador.java*
- *Responsabilidade*: Controle do pipeline e execução
- *Funcionalidades*:
  - Implementar as 5 etapas do pipeline
  - Controlar o fluxo de execução
  - Gerenciar syscalls
  - Sistema de debug e logging
  - Tratamento de erros

### *Memoria.java*
- *Responsabilidade*: Gerenciamento da memória
- *Funcionalidades*:
  - Separar instruções e dados
  - Validar endereços
  - Operações de leitura/escrita
  - Carregamento de binários

### *Registrador.java*
- *Responsabilidade*: Banco de registradores
- *Funcionalidades*:
  - Armazenar 8 registradores de 16 bits
  - Validar índices
  - Operações get/set
  - Dump para debug

### *Instrucao.java*
- *Responsabilidade*: Decodificação de instruções
- *Funcionalidades*:
  - Detectar tipo R ou I
  - Extrair campos (opcode, registradores, imediato)
  - Extensão de sinal
  - Representação textual

### *Lib.java*
- *Responsabilidade*: Utilitários
- *Funcionalidades*:
  - Carregar arquivos binários
  - Manipulação de bits
  - Conversão little-endian

### *ProcessorException.java*
- *Responsabilidade*: Tratamento de erros
- *Funcionalidades*:
  - Exceções específicas do simulador
  - Messages detalhadas de erro
  - Rastreamento de causas

---

## 🌊 Fluxo de Execução

### Inicialização:

1. Usuário fornece arquivo binário
2. Carregamento do binário na memória
3. Inicialização dos registradores (todos = 0)
4. PC = 0


### Loop Principal:

ENQUANTO (programa rodando E ciclos < limite):
    1. FETCH: instrução = memória[PC]
    2. DECODE: campos = decodificar(instrução)
    3. EXECUTE: resultado = executar(campos)
    4. MEMORY: acessar_memória(resultado)
    5. WRITE BACK: escrever_registrador(resultado)
    6. Atualizar PC
    7. Verificar condições de parada


### Condições de Parada:
- Syscall de término (R0 = 0)
- Erro de execução
- Limite máximo de ciclos (100.000)

---

## 🚀 Como Usar

### Compilação:
bash
javac *.java


### Execução:
bash
java Main


### Interação:

Binary path: programa.bin
Enable debug mode? (y/n): y


### Modos de Execução:

#### *Modo Normal*:
- Execução rápida
- Mostra apenas o resultado final
- Indicador de progresso a cada 1000 ciclos

#### *Modo Debug*:
- Mostra todas as 5 etapas de cada ciclo
- Pausa entre ciclos (pressione Enter)
- Estado completo dos registradores
- Ideal para aprendizado e depuração

---

## 🔧 Solução de problemas

### Problemas Comuns:

#### *Erro: "Arquivo não encontrado"*
- Verifique se o caminho do arquivo está correto
- Certifique-se de que o arquivo .bin existe
- Use caminho absoluto se necessário

#### *Erro: "Divisão por zero"*
- Verifique se o registrador rs2 não é zero antes da divisão
- Use cmp_equal para verificar antes de dividir

#### *Erro: "Endereço de memória inválido"*
- Endereços de dados devem estar entre 0 e (65536-1024)
- Endereços de instruções são limitados pelo tamanho do programa

#### *Erro: "Loop infinito detectado"*
- Verifique se há uma condição de parada no programa
- Use syscall 0 para terminar explicitamente
- Aumente o limite de ciclos se necessário

#### *Programa não termina*
- Certifique-se de incluir syscall com R0=0 no final
- Verifique jumps condicionais
- Use modo debug para rastrear execução

### Dicas de Debug:

1. *Use sempre o modo debug* para entender o fluxo
2. *Verifique o estado dos registradores* após cada operação
3. *Acompanhe o PC* para detectar loops
4. *Valide endereços de memória* antes de usar
5. *Teste instruções individualmente* antes de programas complexos
---

*Versão*: 1.01
