public class Instrucao {
    private final short raw;
    private final int unsignedRaw;

    private final int formatBit;
    private final int opcode;

    // campos R
    private final int rd;
    private final int rs1;
    private final int rs2;

    // campos I
    private final int immediate10;

    public Instrucao(short raw) {
        this.raw = raw;
        this.unsignedRaw = raw & 0xFFFF;
        this.formatBit = (unsignedRaw >> 15) & 0x1;

        if (formatBit == 0) { // formato R
            this.opcode = (unsignedRaw >> 9) & 0x3F; // 6 bits
            this.rd = (unsignedRaw >> 6) & 0x7;      // 3 bits
            this.rs1 = (unsignedRaw >> 3) & 0x7;     // 3 bits
            this.rs2 = unsignedRaw & 0x7;            // 3 bits
            this.immediate10 = 0;
        } else { // formato I
            this.opcode = (unsignedRaw >> 13) & 0x3; // 2 bits
            this.rd = (unsignedRaw >> 10) & 0x7;     // 3 bits
            this.rs1 = 0;
            this.rs2 = 0;
            this.immediate10 = unsignedRaw & 0x3FF;  // 10 bits
        }
    }

    // Getters básicos
    public short getRaw() { return raw; }
    public int getUnsignedRaw() { return unsignedRaw; }
    public int getFormatBit() { return formatBit; }
    public boolean isFormatR() { return formatBit == 0; }
    public boolean isFormatI() { return formatBit == 1; }
    public int getOpcode() { return opcode; }

    // Campos formato R
    public int getRd() { return rd; }
    public int getRs1() { return rs1; }
    public int getRs2() { return rs2; }

    // Campos formato I
    public int getImmediateUnsigned() { return immediate10; }

    public short getImmediateSigned() {
        // Extensão de sinal para 10 bits
        if ((immediate10 & 0x200) != 0) { // bit 9 = 1 (negativo)
            return (short) (immediate10 | 0xFC00); // preenche bits altos com 1
        } else {
            return (short) immediate10;
        }
    }

    @Override
    public String toString() {
        if (isFormatR()) {
            return String.format("R-Type[raw=0x%04X, op=%d, rd=R%d, rs1=R%d, rs2=R%d]",
                    unsignedRaw, opcode, rd, rs1, rs2);
        } else {
            return String.format("I-Type[raw=0x%04X, op=%d, rd=R%d, imm=%d]",
                    unsignedRaw, opcode, rd, immediate10);
        }
    }
}