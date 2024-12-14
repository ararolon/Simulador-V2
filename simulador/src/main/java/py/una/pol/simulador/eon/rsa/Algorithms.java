/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;

import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.utils.Utils;

/**
 *
 * @author Néstor E. Reinoso Wood ,version base
 * @author  Aramy Rolon , optimizacion del RSA con conmutacion de nucleos
 */
public class Algorithms {    

    /* una variable global de la clase en el cual se guardara el crosstalk min encontrado en un core de un enlace*/   
    // se declara como estatica para poder acceder a la variable en todos los metodos de la clase que es lo que se necesita.
    private static BigDecimal CrosstalkMinCore =  BigDecimal.ZERO;
    //entre todos los cores candidatos , ir identificando cual tiene el crosstalk min , variable aux del indice
    private static int MinCoreIndex = 0;


    /**
     * Algoritmo RSA con conmutación de núcleos
     *
     * @param graph Grafo de la topología de la red
     * @param demand Demanda a insertar
     * @param capacity Capacidad de la red
     * @param cores Cantidad total de núcleos
     * @param maxCrosstalk Máximo nivel de crosstalk permitido
     * @param crosstalkPerUnitLength Crosstalk por unidad de longitud (h) de la fibra
     * @return Ruta establecida, o null si hay bloqueo
     */
    public static EstablishedRoute ruteoCoreMultiple(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {
        System.out.println("entro");
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        Integer fsIndexBegin = null;
        Integer selectedIndex;
        // Iteramos los KSP elegidos
        //k caminos más cortos entre source y destination de la demanda actual

        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5); // aca se corre el algoritmo y encuentra lo  5 caminos
        while (k < kspaths.size() && kspaths.get(k) != null) {  // asume que si o si encuentra un camino porque al hacer get no esta vacio.
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k); // aca va iterar los caminos (empieza por el primer camino y asi sucesivamente , k es el indice de recorrido)
            // Recorremos los FS
            for (int i = 0; i < capacity - demand.getFs(); i++) { // porque hace saltos igual a la cantidad del valor de slots necesarios por demanda obtenido en demand.getFs
                List<Link> enlacesLibres = new ArrayList<>(); //lista auxiliar de enlaces para la ruta del ksp recorrido
                List<Integer> kspCores = new ArrayList<>();  // lista auxiliar de los nucleos aceptados para los enlaces establecidos
                List<BigDecimal> crosstalkFSList; //Lista Auxiliar de Crosstalk que se asigna a la ruta por nucelo elegido 
                crosstalkFSList = new ArrayList<>();  // los slots con crosstlak (se va actualizando por demanda,core, es una lista auxiliar)
                  
                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {  
                    //Inicializa con el valor cero la lista , con BigDecimal para que sea mas preciso 
                    crosstalkFSList.add(BigDecimal.ZERO);
                }
                for (Link link : ksp.getEdgeList()) { //obtiene los enlaces de los camino que se esta analizando, iterando
                    
                   /* for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {  
                        //Inicializa con el valor cero la lista , con BigDecimal para que sea mas preciso 
                        crosstalkFSList.add(BigDecimal.ZERO);
                    }  */

                    // inicializo nuevamente ya que para cada enlace se debe ubicar la demanda.                   
                    CrosstalkMinCore = BigDecimal.ZERO; // cambia por enlace
                    //inicializa la variable que guarda el crosstalk del enlace por core
                    
                    List<BigDecimal> CrosstalkRutaporCore = new ArrayList<>() ;// auxiliar para guardar el crosstalk auxiliar del enlace de un core
                   
                    for (int aux = 0 ; aux<cores; aux++)
                        CrosstalkRutaporCore.add(BigDecimal.ZERO); //aumenta tamanho , cambiar...
                    
                    List<Integer> Coresaux = new ArrayList<>() ; // lista auxiliar donde se guardan los nucleos candidatos para elegir el min

                    for (int core = 0; core < cores; core++) { // itera los nucleos del enlace
                        // Para cada core debo inicializar mis auxiliar de crosstalk por Fs ???
                       // crosstalkFSList = new ArrayList<>(); // los slots con crosstlak (se va actualizando por demanda,core, es una lista auxiliar)
                       
                        if (i < capacity - demand.getFs()) { // si el indice que recorre todavia no salio de rango (por los saltos , para poder hacer le bloque)
                            /*Del core analizado , va a obtener la porcion de FS que necesita la demanda 
                            y verificar si esos FS estan libres y si el crosstalk es aceptable para establecer la demanda.*/ 
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(i, i + demand.getFs());// obtiene la porcion de slots que necesita la demanda               
                            // Controla si está ocupado por una demanda
                            if (isFSBlockFree(bloqueFS)) {  //si se puede utilizar el bloque de slots, controla el crosstalk, pero si uno de ellos no se puede usar, pasa al siguiente core
                                // Control de crosstalk
                                if (isFsBlockCrosstalkFree(bloqueFS, maxCrosstalk, crosstalkFSList)) {
                                    if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs(), crosstalkPerUnitLength)) {
                                        Coresaux.add(core); //core aceptado 
                
                                        for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {
                                            BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance()))); //auxiliar
                                            crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta); // Calcular el crosstalk real sobre el FS y asigna en la lista de los valores de crosstalk de los fs
                                        
                                        }
                                        
                                        //para el core n se obtiene el crosstalk en la ruta que es igual a el valor en la variable suma
                                        CrosstalkRutaporCore.set(core,Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance())));//en la lista , asigna el crosstalk total del enlace por core
                                    }
                                }

                            }
                        }
                    }
                    //luego de analizar todos los nucleos del enlace, se verifica cual es el que tiene menor crosstalk
                    //analiza solo si es que encontro cores candidatos 
                    if(!Coresaux.isEmpty()){
                        // se obtiene el core min para elegir
                        for(int c = 0; c < Coresaux.size(); c++ ){
                            //convierte a double el valor para imprimir
                            //System.out.printf("Se analiza el core : %d con el crosstalk igual a %.3f\n",c,CrosstalkRutaporCore.get(c).doubleValue());
                            
                            if(c == 0){
                                CrosstalkMinCore = CrosstalkRutaporCore.get(c);
                                MinCoreIndex = c;
                            } 
                            else if (CrosstalkMinCore.compareTo(CrosstalkRutaporCore.get(c)) > 0)
                            {
                                CrosstalkMinCore = CrosstalkRutaporCore.get(c);
                                MinCoreIndex = c;
                            }
                        }
                        
                       // System.out.printf("Eligio el c:%d y XT:%.3f\n",MinCoreIndex, CrosstalkMinCore.doubleValue());

                        enlacesLibres.add(link); //enlace libre de crosstalk, 
                        

                        kspCores.add(MinCoreIndex); //Se agrega el core min a la lista de los cores aceptados para la ruta
                        fsIndexBegin = i;  // el indice donde empiezan los bloques de FS
                        selectedIndex = k; //se guarda el camino escogido
                        // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a la lista de rutas establecidas.
                        
                        //Luego de seleccionar cual sera el nucleo utilizado, se asigna el crosstalk a sus FS
                       
                        if (enlacesLibres.size() == ksp.getEdgeList().size()) {
                            System.out.printf("La ruta se establece cuando hay %d enlaces\n",ksp.getEdgeList().size());
                            kspPlaced.add(kspaths.get(selectedIndex));
                            kspPlacedCores.add(kspCores);
                            k = kspaths.size(); //para que salga del bucle, al encontrar la ruta, va a utilizar la primera ruta candidata
                            i = capacity;
                        }
                    }
                     //Asumiendo que si en uno de los enlaces hay bloqueo , se pasa al siguiente camino
                    if(Coresaux.isEmpty()){
                        k++;
                        break;
                    }
                }
                
            } //si termina de analizar los cores de un enlace y no encuentra un bloque no deberia pasar al siguiente camino ?
            k++;
        }
        //Se usa los datos obtenidos del RSA para indicar la ruta establecida.
        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlaced.isEmpty()) {
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(),
                    fsIndexBegin, demand.getFs(), demand.getLifetime(),
                    demand.getSource(), demand.getDestination(), kspPlacedCores.get(0));
        } else {
            //System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }
    // Aqui se controla si un bloque de slots esta libre , no solo uno es decir n slots estan libres
    //donde n es la cantidad de fs requerida por la demanda 
    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Analiza un bloque de slots , si esta libre del crosstalk maximo tolerado.
     @param fss es el bloque de FS que utilizara la demanda
     @param maxCrosstalk es el crosstalk maximo tolerado  -25 db
     @param crosstalkRuta es una lista auxiliar donde se almacena el crosstalk de cada fs del enlace.
     @return booleano true, false si es que se cumple que todos los fs tienen crosstalk menor al crosstalk maximo 
     */ 

    private static Boolean isFsBlockCrosstalkFree(List<FrequencySlot> fss, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        for (int i = 0; i < fss.size(); i++) {
            //va agregando los valores de los crosstalk en la lista auxiliara para ir comparando)
            BigDecimal crosstalkActual = crosstalkRuta.get(i).add(fss.get(i).getCrosstalk());
            if (crosstalkActual.compareTo(maxCrosstalk) > 0) { //compara la sumatoria de los crosstalks
                return false;
            }
        }
        return true;
    }

/**
  *Funcion que analiza el crosstalk generado sobre un core , respecto a los cores vecinos 
  @param link enlace del core analizado
  @param maxCrosstalk es el maximo crosstalk tolerado
  @param core es el core analizado
  @param fsIndexBegin indice del fs analizado en el algoritmo RSA
  @param fsWidth  indice final del bloque de fs de la demanda
  @param crosstalkPerUnitLength Crosstalk por unidad de longitud (h) de la

  @return booleano si cumple la condicion de que el crosstalk del fs del vecino es tolerado con respecto al crosstalk maximo, no va a crear un bloqueo sobre el fs del core analizado
 */


    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core); //dependiendo de que nro de core es, obtiene sus vecinos
        for (Integer coreVecino : vecinos) { //se itera los vecinos
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) { 
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i); // Compara con el mismo Fs pero en el core vecino
                if (!fsVecino.isFree()) { //si el Fs en el core vecino tiene demanda, se calcula el crosstalk en decibelios
                    BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar); // variable auxiliar para comparar con el crosstalk maximo, ademas le asigna al fs vecino , el crosstalk que genera
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) { //compara con el valor maximo tolerado, si es mayor o igual , bloqueo
                        return false; //si encuentra que en alguno de los cores hay un fs que supera el crosstalk , ya se rechaza
                    }
                }
            }
        }
        return true;
    }
}
