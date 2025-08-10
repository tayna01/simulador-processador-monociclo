public class Registrador {
    private static final int NUM_REGISTERS = 8;
    private final short[] registers;

    public Registrador() {
        this.registers = new short[NUM_REGISTERS];
        reset();
    }

    public short get(int index) {
        validateIndex(index);
        return registers[index];
    }

    public void set(int index, short value) {
        validateIndex(index);
        registers[index] = value;
    }

    public void reset() {
        for (int i = 0; i < NUM_REGISTERS; i++) {
            registers[i] = 0;
        }
    }

    public int size() {
        return NUM_REGISTERS;
    }

    public void dump() {
        System.out.println("=== Registradores ===");
        for (int i = 0; i < NUM_REGISTERS; i++) {
            System.out.printf("R%d: %6d (0x%04X)%n", i, registers[i], registers[i] & 0xFFFF);
        }
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Índice de registrador inválido: " + index +
                    " (deve estar entre 0 e " + (NUM_REGISTERS-1) + ")");
        }
    }
}
