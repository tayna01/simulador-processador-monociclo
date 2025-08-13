public class Memoria {
    private static final int MEMORY_SIZE = 65536; // 64KB
    private static final int INSTRUCTION_START = 0;
    private static final int DATA_START = 1024; // Área de dados começa em 1024

    private final short[] memory;
    private int instructionCount = 0;

    public Memoria(String binaryPath) throws ProcessorException {
        this.memory = new short[MEMORY_SIZE];
        loadBinary(binaryPath);
    }

    private void loadBinary(String binaryPath) throws ProcessorException {
        try {
            if (binaryPath == null || !binaryPath.toLowerCase().endsWith(".bin")) {
                throw new ProcessorException("O arquivo informado não possui extensão .bin: " + binaryPath);
            }

            Lib loader = new Lib();
            short[] loadedInstructions = loader.load_binary(binaryPath);
            System.arraycopy(loadedInstructions, 0, memory, INSTRUCTION_START, loadedInstructions.length);
            instructionCount = loadedInstructions.length;

            for (int i = DATA_START; i < MEMORY_SIZE; i++) {
                memory[i] = 0;
            }

            System.out.println("Binário carregado: " + instructionCount + " instruções");

        } catch (Exception e) {
            throw new ProcessorException("Erro ao carregar binário: " + e.getMessage());
        }
    }

    public short readInstruction(int address) throws ProcessorException {
        if (address < 0 || address >= instructionCount) {
            throw new ProcessorException("Endereço de instrução inválido: " + address);
        }
        return memory[INSTRUCTION_START + address];
    }

    public short readData(int address) throws ProcessorException {
        int realAddress = DATA_START + address;
        if (realAddress < DATA_START || realAddress >= MEMORY_SIZE) {
            throw new ProcessorException("Endereço de dados inválido: " + address);
        }
        return memory[realAddress];
    }

    public void writeData(int address, short value) throws ProcessorException {
        int realAddress = DATA_START + address;
        if (realAddress < DATA_START || realAddress >= MEMORY_SIZE) {
            throw new ProcessorException("Endereço de dados inválido para escrita: " + address);
        }
        memory[realAddress] = value;
    }
}