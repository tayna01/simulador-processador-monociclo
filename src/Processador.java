public class Processador {
    private final Memoria memoria;
    private final Registrador registrador;
    private final boolean debugMode;
    private boolean running = true;
    private int pc = 0;
    private int cycleCount = 0;

    private static final int MAX_CYCLES = 100000;

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
        if (debugMode) {
            System.out.printf("Ciclo %d - PC: %d\n", cycleCount, pc);
        }

        short rawInstruction = fetch();

        Instrucao instruction = decode(rawInstruction);

        if (debugMode) {
            System.out.println("  Decodificada: " + instruction.toString());
        }
        ExecutionResult result = execute(instruction);
        memoryAccess(result);
        writeBack(result);
        updatePC(result);

        if (result.terminate) {
            running = false;
        }
        if (result.error) {
            throw new ProcessorException(result.errorMessage);
        }
    }

    private short fetch() throws ProcessorException {
        try {
            return memoria.readInstruction(pc);
        } catch (Exception e) {
            throw new ProcessorException("Erro ao buscar instrução no endereço " + pc + ": " + e.getMessage());
        }
    }

    private Instrucao decode(short raw) {
        return new Instrucao(raw);
    }

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
            case 0:
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 + val2);
                if (debugMode) {
                    System.out.printf("  ADD: R%d = R%d(%d) + R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 1:
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 - val2);
                if (debugMode) {
                    System.out.printf("  SUB: R%d = R%d(%d) - R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 2:
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 * val2);
                if (debugMode) {
                    System.out.printf("  MUL: R%d = R%d(%d) * R%d(%d) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 3:
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

            case 4:
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 == val2 ? 1 : 0);
                if (debugMode) {
                    System.out.printf("  CMP_EQ: R%d = (R%d(%d) == R%d(%d)) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 5:
                result.writeRegister = true;
                result.registerIndex = rd;
                result.registerValue = (short) (val1 != val2 ? 1 : 0);
                if (debugMode) {
                    System.out.printf("  CMP_NEQ: R%d = (R%d(%d) != R%d(%d)) = %d\n",
                            rd, rs1, val1, rs2, val2, result.registerValue);
                }
                break;

            case 15:
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

            case 16:
                result.accessMemory = true;
                result.isMemoryWrite = true;
                result.memoryAddress = val1;
                result.memoryValue = val2;
                if (debugMode) {
                    System.out.printf("  STORE: MEM[R%d(%d)] = R%d(%d)\n", rs1, val1, rs2, val2);
                }
                break;

            case 63:
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
            case 0:
                result.jump = true;
                result.jumpAddress = imediato;
                if (debugMode) {
                    System.out.printf("  JUMP: PC = %d\n", imediato);
                }
                break;

            case 1:
                result.conditionalJump = true;
                result.conditionRegister = rd;
                result.conditionalJumpAddress = imediato;
                if (debugMode) {
                    System.out.printf("  JUMP_COND: se R%d == 1 então PC = %d\n", rd, imediato);
                }
                break;

            case 3:
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


    private void memoryAccess(ExecutionResult result) throws ProcessorException {
        if (result.accessMemory) {
            try {
                if (result.isMemoryWrite) {
                    memoria.writeData(result.memoryAddress, result.memoryValue);
                } else {
                    result.registerValue = memoria.readData(result.memoryAddress);
                }
            } catch (Exception e) {
                result.error = true;
                result.errorMessage = "Erro no acesso à memória no endereço " +
                        result.memoryAddress + ": " + e.getMessage();
            }
        }
    }

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

    private void handleSyscall(ExecutionResult result) {
        short service = registrador.get(0);

        if (debugMode) {
            System.out.printf("  SYSCALL: serviço %d\n", service);
        }

        switch (service) {
            case 0:
                result.terminate = true;
                System.out.println("Programa encerrado via syscall");
                break;

            case 1:
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

            case 2:
                System.out.println();
                break;

            case 3:
                System.out.println(registrador.get(1));
                break;

            case 6:
                try {
                    int sleepTime = registrador.get(1);
                    Thread.sleep(sleepTime * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result.error = true;
                    result.errorMessage = "Sleep interrompido";
                }
                break;

            case 7:
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