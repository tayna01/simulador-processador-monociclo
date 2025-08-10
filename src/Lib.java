import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class Lib {
    public short extract_bits(short value, int bstart, int blength) {
        if (blength <= 0 || blength > 16 || bstart < 0 || bstart >= 16) {
            throw new IllegalArgumentException("Parâmetros inválidos para extract_bits");
        }

        short mask = (short) ((1 << blength) - 1);
        return (short) ((value >> bstart) & mask);
    }

    public short[] load_binary(String binary_name) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(binary_name);
             DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            long fileSize = fileInputStream.getChannel().size();
            if (fileSize % 2 != 0) {
                throw new IOException("Arquivo binário tem tamanho ímpar, não é válido para instruções de 16 bits");
            }

            int numInstructions = (int) (fileSize / 2);
            short[] instructions = new short[numInstructions];

            for (int i = 0; i < numInstructions; i++) {
                // Lê 2 bytes e converte para short (little-endian)
                int low = dataInputStream.readByte() & 0xFF;
                int high = dataInputStream.readByte() & 0xFF;
                instructions[i] = (short) (low | (high << 8));
            }

            return instructions;

        } catch (IOException e) {
            throw new IOException("Erro ao ler arquivo binário '" + binary_name + "': " + e.getMessage());
        }
    }
}
