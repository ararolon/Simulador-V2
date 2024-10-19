package py.una.pol.simulador.eon.models.enums;

/**
 * Enumerator for the RSAs supported on the simulator
 *
 * @author Néstor E. Reinoso Wood //Version base
 * @author Aramy Rolon  /*V2 del simulador */

/*
  Se modifica la estrucutura , solamente se recibe la etiqueta de RSA con conmutacion de nucleos
 */
 public enum RSAEnum {

    // RSA con conmutación de núcleos
    
    MULTIPLES_CORES("Múltiples Cores", "ruteoCoreMultiple");

    private final String label;

    private final String method;

    /**
     * Etiquieta del RSA
     *
     * @return Etiquieta del RSA
     */
    public String label() {
        return label;
    }

    /**
     * Nombre del método a ejecutar
     *
     * @return Nombre del método a ejecutar
     */
    public String method() {
        return method;
    }

    /**
     * Enum constructor
     *
     * @param label Label of the algorithm
     * @param method Name of the RSA method
     */
    private RSAEnum(String label, String method) {
        this.label = label;
        this.method = method;
    }
}
