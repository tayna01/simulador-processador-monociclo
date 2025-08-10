import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        /**
         * Entrada do arquivo binário
         */
        Scanner sc = new Scanner(System.in);
        System.out.print("Adicione o caminho para o código binário: ");
        String path = sc.nextLine();

        System.out.print("Habilitar o modo debug? (y/n): ");
        boolean debugMode = sc.nextLine().toLowerCase().startsWith("y");
        sc.close();

        try {
            /**
             * Inicialização dos módulos
             */
            Memoria memoria = new Memoria(path);
            Registrador registrador = new Registrador();

            Processador processador = new Processador(memoria, registrador, debugMode);
            processador.run();
        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}