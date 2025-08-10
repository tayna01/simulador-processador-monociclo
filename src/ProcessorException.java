/**
 * Exceção customizada para erros do simulador de processador.
 *
 * Esta exceção é lançada quando ocorrem erros específicos durante
 * a execução do simulador, como:
 * - Endereços de memória inválidos
 * - Índices de registradores inválidos
 * - Divisão por zero
 * - Instruções inválidas
 * - Erros de carregamento de binário
 *
 * @author Seu Nome
 * @version 1.0
 */
public class ProcessorException extends Exception {

    /**
     * Constrói uma nova ProcessorException com a mensagem especificada.
     *
     * @param message a mensagem de erro detalhada
     */
    public ProcessorException(String message) {
        super(message);
    }

    /**
     * Constrói uma nova ProcessorException com a mensagem e causa especificadas.
     *
     * @param message a mensagem de erro detalhada
     * @param cause a causa da exceção
     */
    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constrói uma nova ProcessorException com a causa especificada.
     *
     * @param cause a causa da exceção
     */
    public ProcessorException(Throwable cause) {
        super(cause);
    }
}