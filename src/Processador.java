// ============================================================================
// PROCESSADOR.JAVA - COM ETAPAS SEPARADAS DO PIPELINE MONOCICLO
// ============================================================================
public class Processador {
    private final Memoria memoria;
    private final Registrador registrador;
    private final boolean debugMode;
    private boolean running = true;
    private int pc = 0;
    private int cycleCount = 0;

    // Constantes para controle
    private static final int MAX_CYCLES = 100000;

    // Estrutura para resultados da execução
    private static class ExecutionResult {
        boolean writeRegister = false;
        int registerIndex = 0;
        short registerValue = 0;

        boolean accessMemory = false;
        boolean isMemoryWrite = false;
        int memoryAddress = 0;
        short memoryValue = 0;

        boolean jump = false;
        int jumpAddress = 0;

        boolean conditionalJump = false;
        int conditionRegister = 0;
        int conditionalJumpAddress = 0;

        boolean terminate = false;
        boolean error = false;
        String errorMessage = "";
    }

    public Processador(Memoria memoria, Registrador registrador, boolean debugMode) {
        this.memoria = memoria;
        this.registrador = registrador;
        this.debugMode = debugMode;
    }

    public void run() {
        System.out.println("=== Iniciando Simulador de Processador Monociclo ===");
        if (debugMode) {
            System.out.println("Modo DEBUG ativado");
        }

        while (running && cycleCount < MAX_CYCLES) {
            cycleCount++;

            try {
                executeCycle();
            } catch (ProcessorException e) {
                System.err.println("Erro no ciclo " + cycleCount + ": " + e.getMessage());
                running = false;
            }

            if (debugMode && cycleCount % 1000 == 0) {
                System.out.println("Executando... Ciclo: " + cycleCount);
            }
        }

        if (cycleCount >= MAX_CYCLES) {
            System.out.println("Limite máximo de ciclos atingido. Possível loop infinito.");
        }

        System.out.println("=== Execução finalizada após " + cycleCount + " ciclos ===");
        printFinalState();
    }

    private void executeCycle() throws ProcessorException {
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("CICLO %d - MONOCICLO\n", cycleCount);
        System.out.println("=".repeat(80));

        // === ETAPA 1: FETCH ===
        System.out.printf("1. FETCH: PC = %d\n", pc);
        System.out.print("   Buscando instrução na memória... ");

        short rawInstruction = fetch();

        System.out.printf("Instrução bruta: 0x%04X (%d)\n", rawInstruction & 0xFFFF, rawInstruction);

        // === ETAPA 2: DECODE ===
        System.out.print("2. DECODE: Decodificando instrução... ");

        Instrucao instruction = decode(rawInstruction);

        System.out.println("OK");
        System.out.println("   " + instruction.toString());

        // Mostrar detalhes da decodificação
        if (instruction.isFormatR()) {
            System.out.printf("   Formato R: opcode=%d, rd=R%d, rs1=R%d, rs2=R%d\n",
                    instruction.getOpcode(), instruction.getRd(),
                    instruction.getRs1(), instruction.getRs2());
            System.out.printf("   Valores: R%d=%d, R%d=%d\n",
                    instruction.getRs1(), registrador.get(instruction.getRs1()),
                    instruction.getRs2(), registrador.get(instruction.getRs2()));
        } else {
            System.out.printf("   Formato I: opcode=%d, rd=R%d, imediato=%d\n",
                    instruction.getOpcode(), instruction.getRd(),
                    instruction.getImmediateUnsigned());
            if (instruction.getOpcode() != 0) { // não é jump
                System.out.printf("   Valor: R%d=%d\n",
                        instruction.getRd(), registrador.get(instruction.getRd()));
            }
        }

        // === ETAPA 3: EXECUTE ===
        System.out.print("3. EXECUTE: Executando operação... ");

        ExecutionResult result = execute(instruction);

        System.out.println("OK");
        printExecutionDetails(instruction, result);

        // === ETAPA 4: MEMORY ACCESS ===
        System.out.print("4. MEMORY: ");
        if (result.accessMemory) {
            if (result.isMemoryWrite) {
                System.out.printf("Escrevendo MEM[%d] = %d... ", result.memoryAddress, result.memoryValue);
            } else {
                System.out.printf("Lendo MEM[%d]... ", result.memoryAddress);
            }
        } else {
            System.out.print("Nenhum acesso à memória... ");
        }

        memoryAccess(result);

        System.out.println("OK");
        if (result.accessMemory && !result.isMemoryWrite) {
            System.out.printf("   Valor lido: %d\n", result.registerValue);
        }

        // === ETAPA 5: WRITE BACK ===
        System.out.print("5. WRITE BACK: ");
        if (result.writeRegister) {
            System.out.printf("Escrevendo R%d = %d... ", result.registerIndex, result.registerValue);
        } else {
            System.out.print("Nenhuma escrita em registrador... ");
        }

        writeBack(result);

        System.out.println("OK");

        // === CONTROLE DE FLUXO ===
        System.out.print("6. PC UPDATE: ");
        int oldPC = pc;
        updatePC(result);

        if (result.jump) {
            System.out.printf("JUMP para %d (PC: %d -> %d)\n", result.jumpAddress, oldPC, pc);
        } else if (result.conditionalJump) {
            short condition = registrador.get(result.conditionRegister);
            if (condition == 1) {
                System.out.printf("JUMP CONDICIONAL executado para %d (PC: %d -> %d)\n",
                        result.conditionalJumpAddress, oldPC, pc);
            } else {
                System.out.printf("JUMP CONDICIONAL não executado (condição = %d) (PC: %d -> %d)\n",
                        condition, oldPC, pc);
            }
        } else {
            System.out.printf("Incremento normal (PC: %d -> %d)\n", oldPC, pc);
        }

        // Mostrar estado atual dos registradores
        if (debugMode) {
            System.out.println("\n   Estado dos Registradores:");
            for (int i = 0; i < registrador.size(); i++) {
                System.out.printf("   R%d: %6d (0x%04X)  ", i, registrador.get(i), registrador.get(i) & 0xFFFF);
                if ((i + 1) % 4 == 0) System.out.println();
            }
            if (registrador.size() % 4 != 0) System.out.println();
        }

        // === VERIFICAR TÉRMINO/ERRO ===
        if (result.terminate) {
            System.out.println("\n>>> PROGRAMA TERMINADO <<<");
            running = false;
        }
        if (result.error) {
            System.out.println("\n>>> ERRO: " + result.errorMessage + " <<<");
            throw new ProcessorException(result.errorMessage);
        }

        // Pausa para visualização (opcional)
        if (debugMode) {
            System.out.print("\nPressione Enter para continuar...");
            try {
                System.in.read();
            } catch (Exception e) {
                // Ignora erro de input
            }
        }
    }

    // ========================================================================
    // ETAPA 1: FETCH
    // ========================================================================
    private short fetch() throws ProcessorException {
        try {
            return memoria.readInstruction(pc);
        } catch (Exception e) {
            throw new ProcessorException("Erro ao buscar instrução no endereço " + pc + ": " + e.getMessage());
        }
    }

    // ========================================================================
    // ETAPA 2: DECODE
    // ========================================================================
    private Instrucao decode(short raw) {
        return new Instrucao(raw);
    }

    // ========================================================================
    // ETAPA 3: EXECUTE
    // ========================================================================
    private ExecutionResult execute(Instrucao instr) throws ProcessorException {
        ExecutionResult result = new ExecutionResult();

        validateInstruction(instr);

        if (instr.isFormatR()) {
            executeFormatR(instr, result);
        } else {
            executeFormatI(instr, result);
        }

        return result;
    }

    private void executeFormatR(Instrucao instr, ExecutionResult result) throws ProcessorException {
        int opcode = instr.getOpcode();
        int rd = instr.getRd();
        int rs1 = instr.getRs1();
        int rs2 = instr.getRs2();

        short val1 = registrador.get(rs1);
        short val2 = registrador.get(rs2);

        switch (opcode) {
            case 0: // add
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 + val2);
                if (debugMode) {
                    System.out.printf("  ADD: R%d = R%d(%d) + R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 1: // sub
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 - val2);
                if (debugMode) {
                    System.out.printf("  SUB: R%d = R%d(%d) - R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 2: // mul
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 * val2);
                if (debugMode) {
                    System.out.printf("  MUL: R%d = R%d(%d) * R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 3: // div
                if (val2 == 0) {
                    result.error = true;
                    result.errorMessage = "Divisão por zero: R" + rs2 + " = 0";
                } else {
                    result.writeRegister = true;
                    result.registerIndex = rd;
                    result.registerValue = (short) (val1 / val2);
                    if (debugMode) {
                        System.out.printf("  DIV: R%d = R%d(%d) / R%d(%d) = %d\n",
                                rd, rs1, val1, rs2, val2, result.registerValue);
                    }
                }
                break;

            case 4: // cmp_equal
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 == val2 ? 1 : 0);
                if (debugMode) {
                    System.out.printf("  CMP_EQ: R%d = (R%d(%d) == R%d(%d)) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 5: // cmp_neq
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 != val2 ? 1 : 0);
                if (debugMode) {
                    System.out.printf("  CMP_NEQ: R%d = (R%d(%d) != R%d(%d)) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 15: // load
                result.accessMemory = true;
                result.isMemoryWrite = false;
                result.memoryAddress = val1;
                result.writeRegister = true;
                result.registerIndex = rd;
                // o valor será lido na etapa de memory access
                if (debugMode) {
                    System.out.printf("  LOAD: R%d = MEM[R%d(%d)]\n", rd, rs1, val1);
                }
                break;

            case 16: // store
                result.accessMemory = true;
                result.isMemoryWrite = true;
                result.memoryAddress = val1;
                result.memoryValue = val2;
                if (debugMode) {
                    System.out.printf("  STORE: MEM[R%d(%d)] = R%d(%d)\n", rs1, val1, rs2, val2);
                }
                break;

            case 63: // syscall
                handleSyscall(result);
                break;

            default:
                result.error = true;
                result.errorMessage = "Opcode desconhecido no formato R: " + opcode;
                break;
        }
    }

    private void executeFormatI(Instrucao instr, ExecutionResult result) throws ProcessorException {
        int opcode = instr.getOpcode();
        int rd = instr.getRd();
        int imediato = instr.getImmediateUnsigned();

        switch (opcode) {
            case 0: // jump
                result.jump = true;
                result.jumpAddress = imediato;
                if (debugMode) {
                    System.out.printf("  JUMP: PC = %d\n", imediato);
                }
                break;

            case 1: // jump_cond (se rd == 1 pula)
                result.conditionalJump = true;
                result.conditionRegister = rd;
                result.conditionalJumpAddress = imediato;
                if (debugMode) {
                    System.out.printf("  JUMP_COND: se R%d == 1 então PC = %d\n", rd, imediato);
                }
                break;

            case 3: // mov imediato
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) imediato;
                if (debugMode) {
                    System.out.printf("  MOV: R%d = %d\n", rd, imediato);
                }
                break;

            default:
                result.error = true;
                result.errorMessage = "Opcode desconhecido no formato I: " + opcode;
                break;
        }
    }

    // ========================================================================
    // ETAPA 4: MEMORY ACCESS
    // ========================================================================
    private void memoryAccess(ExecutionResult result) throws ProcessorException {
        if (result.accessMemory) {
            try {
                if (result.isMemoryWrite) {
                    memoria.writeData(result.memoryAddress, result.memoryValue);
                } else {
                    // Para load, ler da memória e salvar o valor
                    result.registerValue = memoria.readData(result.memoryAddress);
                }
            } catch (Exception e) {
                result.error = true;
                result.errorMessage = "Erro no acesso à memória no endereço " +
                        result.memoryAddress + ": " + e.getMessage();
            }
        }
    }

    // ========================================================================
    // ETAPA 5: WRITE BACK
    // ========================================================================
    private void writeBack(ExecutionResult result) throws ProcessorException {
        if (result.writeRegister) {
            try {
                registrador.set(result.registerIndex, result.registerValue);
            } catch (Exception e) {
                result.error = true;
                result.errorMessage = "Erro ao escrever no registrador R" +
                        result.registerIndex + ": " + e.getMessage();
            }
        }
    }

    // ========================================================================
    // CONTROLE DE FLUXO
    // ========================================================================
    private void updatePC(ExecutionResult result) {
        if (result.jump) {
            pc = result.jumpAddress;
        } else if (result.conditionalJump) {
            short condition = registrador.get(result.conditionRegister);
            if (condition == 1) {
                pc = result.conditionalJumpAddress;
                if (debugMode) {
                    System.out.printf("  Condição verdadeira, saltando para %d\n", pc);
                }
            } else {
                pc++;
                if (debugMode) {
                    System.out.printf("  Condição falsa, continuando para %d\n", pc);
                }
            }
        } else {
            pc++;
        }
    }

    // ========================================================================
    // SYSCALLS
    // ========================================================================
    private void handleSyscall(ExecutionResult result) {
        short service = registrador.get(0);

        if (debugMode) {
            System.out.printf("  SYSCALL: serviço %d\n", service);
        }

        switch (service) {
            case 0: // encerra o programa
                result.terminate = true;
                System.out.println("Programa encerrado via syscall");
                break;

            case 1: // print string (endereço em r1)
                try {
                    int addr = registrador.get(1);
                    StringBuilder sb = new StringBuilder();
                    short c;
                    while ((c = memoria.readData(addr)) != 0) {
                        sb.append((char) c);
                        addr++;
                    }
                    System.out.print(sb.toString());
                } catch (Exception e) {
                    result.error = true;
                    result.errorMessage = "Erro na syscall print_string: " + e.getMessage();
                }
                break;

            case 2: // print newline
                System.out.println();
                break;

            case 3: // print integer em r1
                System.out.println(registrador.get(1));
                break;

            case 6: // sleep
                try {
                    int sleepTime = registrador.get(1); // tempo em segundos
                    Thread.sleep(sleepTime * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.error = true;
                    result.errorMessage = "Sleep interrompido";
                }
                break;

            case 7: // get time
                int currentTime = (int) (System.currentTimeMillis() / 1000);
                result.writeRegister = true;
                result.registerIndex = 1;
                result.registerValue = (short) currentTime;
                break;

            default:
                System.out.println("Syscall não implementado: " + service);
                break;
        }
    }

    // ========================================================================
    // MÉTODO AUXILIAR PARA MOSTRAR DETALHES DA EXECUÇÃO
    // ========================================================================
    private void printExecutionDetails(Instrucao instr, ExecutionResult result) {
        if (instr.isFormatR()) {
            int opcode = instr.getOpcode();
            int rd = instr.getRd();
            int rs1 = instr.getRs1();
            int rs2 = instr.getRs2();

            switch (opcode) {
                case 0: // add
                    System.out.printf("   ADD: R%d = R%d(%d) + R%d(%d) = %d\n",
                            rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    break;
                case 1: // sub
                    System.out.printf("   SUB: R%d = R%d(%d) - R%d(%d) = %d\n",
                            rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    break;
                case 2: // mul
                    System.out.printf("   MUL: R%d = R%d(%d) * R%d(%d) = %d\n",
                            rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    break;
                case 3: // div
                    if (!result.error) {
                        System.out.printf("   DIV: R%d = R%d(%d) / R%d(%d) = %d\n",
                                rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    }
                    break;
                case 4: // cmp_equal
                    System.out.printf("   CMP_EQ: R%d = (R%d(%d) == R%d(%d)) = %d\n",
                            rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    break;
                case 5: // cmp_neq
                    System.out.printf("   CMP_NEQ: R%d = (R%d(%d) != R%d(%d)) = %d\n",
                            rd, rs1, registrador.get(rs1), rs2, registrador.get(rs2), result.registerValue);
                    break;
                case 15: // load
                    System.out.printf("   LOAD: R%d = MEM[R%d(%d)]\n", rd, rs1, registrador.get(rs1));
                    break;
                case 16: // store
                    System.out.printf("   STORE: MEM[R%d(%d)] = R%d(%d)\n", rs1, registrador.get(rs1), rs2, registrador.get(rs2));
                    break;
                case 63: // syscall
                    System.out.printf("   SYSCALL: serviço R0(%d)\n", registrador.get(0));
                    break;
            }
        } else {
            int opcode = instr.getOpcode();
            int rd = instr.getRd();
            int imm = instr.getImmediateUnsigned();

            switch (opcode) {
                case 0: // jump
                    System.out.printf("   JUMP: Saltar para endereço %d\n", imm);
                    break;
                case 1: // jump_cond
                    System.out.printf("   JUMP_COND: Se R%d(%d) == 1, saltar para %d\n",
                            rd, registrador.get(rd), imm);
                    break;
                case 3: // mov
                    System.out.printf("   MOV: R%d = %d (imediato)\n", rd, imm);
                    break;
            }
        }
    }
    private void validateInstruction(Instrucao instr) throws ProcessorException {
        if (instr.isFormatR()) {
            validateRegisterIndex(instr.getRd(), "rd");
            validateRegisterIndex(instr.getRs1(), "rs1");
            validateRegisterIndex(instr.getRs2(), "rs2");
        } else {
            validateRegisterIndex(instr.getRd(), "rd");
        }
    }

    private void validateRegisterIndex(int index, String name) throws ProcessorException {
        if (index < 0 || index >= registrador.size()) {
            throw new ProcessorException("Índice de registrador inválido para " + name + ": " + index);
        }
    }

    // ========================================================================
    // UTILITÁRIOS
    // ========================================================================
    private void printFinalState() {
        System.out.println("=== Estado Final dos Registradores ===");
        registrador.dump();

        if (debugMode) {
            System.out.println("\n=== Estatísticas ===");
            System.out.println("Total de ciclos executados: " + cycleCount);
            System.out.println("PC final: " + pc);
        }
    }
}
