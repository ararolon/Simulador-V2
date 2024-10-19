package py.una.pol.simulador.eon.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Ruta establecida por un algoritmo RSA
 *
 * @author Néstor E. Reinoso Wood
 */
@Data
@AllArgsConstructor
public class EstablishedRoute {

    /**
     * Índice inicial del bloque de ranuras de frecuencias que ocupa la conexión
     */
    private Integer fsIndexBegin;
    /**
     * Cantidad de ranuras que ocupa la conexión
     */
    private Integer fsWidth;
    /**
     * Tiempo de vida restante de la conexión
     */
    private Integer lifetime;
    /**
     * Nodo origen
     */
    private Integer from;
    /**
     * Nodo destino
     */
    private Integer to;
    /**
     * Enlaces de la ruta
     */
    private List<Link> path;
    /**
     * Núcleos de los enlaces de la ruta
     */
    private List<Integer> pathCores;

    /**
     * Constructor vacío
     */
    public EstablishedRoute() {
    }

    /**
     * Constructor con parámetros
     *
     * @param path Enlaces de la ruta establecida
     * @param fsIndexBegin Indice inicial del bloque de frecuencias utilizado
     * @param fsWidth Cantidad de ranuras de frecuencia a utilizar
     * @param lifetime Tiempo de vida de la demanda en la ruta
     * @param from Nodo origen
     * @param to Nodo destino
     * @param pathCores Núcleos a los que pertenecen los enlaces de la lista
     * path
     */
    public EstablishedRoute(List<Link> path, Integer fsIndexBegin, Integer fsWidth, Integer lifetime, Integer from, Integer to, List<Integer> pathCores) {
        this.path = path;
        this.fsIndexBegin = fsIndexBegin;
        this.fsWidth = fsWidth;
        this.lifetime = lifetime;
        this.from = from;
        this.to = to;
        this.pathCores = pathCores;
    }

    /**
     * Resta una unidad de tiempo a la conexión
     */
    public void subLifeTime() {
        this.lifetime--;
    }

    @Override
    public String toString() {
        String asd = "EstablisedRoute{"
                + "path=" + path
                + ", fsIndexBegin=" + fsIndexBegin
                + ", fsWidth=" + fsWidth
                + ", tl=" + lifetime
                + ", from=" + from
                + ", to=" + to
                + "}";
        for (Link link : path) {
            asd = asd + link.toString();
        }
        return asd;
    }

}
