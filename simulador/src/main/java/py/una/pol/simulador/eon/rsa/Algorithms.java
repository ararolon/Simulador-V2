/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon.rsa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;

import py.una.pol.simulador.eon.SimulatorTest;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.FrequencySlot;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.utils.Utils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class Algorithms {


    /**
     * Algoritmo RSA con conmutación de núcleos
     *
     * @param graph Grafo de la topología de la red
     * @param demand Demanda a insertar
     * @param capacity Capacidad de la red
     * @param cores Cantidad total de núcleos
     * @param maxCrosstalk Máximo nivel de crosstalk permitido
     * @param crosstalkPerUnitLength Crosstalk por unidad de longitud (h) de la
     * fibra
     * @return Ruta establecida, o null si hay bloqueo
     */
    public static EstablishedRoute ruteoCoreMultiple(Graph<Integer, Link> graph, Demand demand, Integer capacity, Integer cores, BigDecimal maxCrosstalk, Double crosstalkPerUnitLength) {
        int k = 0;

        List<GraphPath<Integer, Link>> kspPlaced = new ArrayList<>();
        //lista que va guardando los nucleos utilizados por enlace 
        List<List<Integer>> kspPlacedCores = new ArrayList<>();
        //Auxiliar para ir guardando el numero de vecinos que generan crosstalk por enlace
        List<Integer> kspPlacedVecinosCrosstalk = new ArrayList<>();

        Integer fsIndexBegin = null;
        Integer selectedIndex = null;
        //Se declaran los flags para verificar el tipo de bloqueo 
        Boolean flag_crosstalk =false;
        Boolean flag_frag = false;
        Boolean flag_capacidad = false;
        int contador1=0, contador2=0;
        //variable auxiliar para hallar el diametro del camino
        Integer D = 0;

        //variable para calcular la cantidad de vecinos con crosstalk
        Integer v_crosstalk = null;

        // Iteramos los KSP elegidos
        //k caminos más cortos entre source y destination de la demanda actual

        KShortestSimplePaths<Integer, Link> kspFinder = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, Link>> kspaths = kspFinder.getPaths(demand.getSource(), demand.getDestination(), 5);
        while (k < kspaths.size() && kspaths.get(k) != null) {
            contador1= 0;
            contador2 = 0;
            fsIndexBegin = null;
            GraphPath<Integer, Link> ksp = kspaths.get(k);
            // Recorremos los FS
            for (int i = 0; i < capacity - demand.getFs(); i++) {
                List<Link> enlacesLibres = new ArrayList<>();
                List<Integer> kspCores = new ArrayList<>();

                // se setean los vecinos con crosstalk a cero , por cada camino que se recorren
                //tambien cuando se cambian los fs analizados , se setea.
                kspPlacedVecinosCrosstalk = new ArrayList<>();

                List<BigDecimal> crosstalkFSList = new ArrayList<>();
                for (int fsCrosstalkIndex = 0; fsCrosstalkIndex < demand.getFs(); fsCrosstalkIndex++) {
                    crosstalkFSList.add(BigDecimal.ZERO);
                } 
                D = 0; // se setea el valor por cada camino K recorrido hasta encontrar la ruta candidata.
                for (Link link : ksp.getEdgeList()) {
                    for (int core = 0; core < cores; core++) {
                        flag_crosstalk =false;
                        flag_frag = false;
                        flag_capacidad = false;
                        if (i < capacity - demand.getFs()) {
                            List<FrequencySlot> bloqueFS = link.getCores().get(core).getFrequencySlots().subList(i, i + demand.getFs());
                            // Controla si está ocupado por una demanda
                            if (isFSBlockFree(bloqueFS)) {
                                // Control de crosstalk
                                if (isFsBlockCrosstalkFree(bloqueFS, maxCrosstalk, crosstalkFSList)) {
                                    if (isNextToCrosstalkFreeCores(link, maxCrosstalk, core, i, demand.getFs(), crosstalkPerUnitLength)) {
                                        // se obtiene la cantidad de nucleos a considerar en el crosstalk 
                                        // siempre +1 porque 1 de la ruta analizada y luego los nucelos a considerar
                                        v_crosstalk = CalculaVecinosConCrosstalk(link, core, i, demand.getFs());
                                        kspPlacedVecinosCrosstalk.add(v_crosstalk);
                                        enlacesLibres.add(link);
                                        kspCores.add(core);
                                        fsIndexBegin = i;
                                        selectedIndex = k;
                                        //calculo del crosstalk de la red y se asigna a las ranuras candidatas donde se establece la demanda.
                                        for (int crosstalkFsListIndex = 0; crosstalkFsListIndex < crosstalkFSList.size(); crosstalkFsListIndex++) {
                                            BigDecimal crosstalkRuta = crosstalkFSList.get(crosstalkFsListIndex);
                                            crosstalkRuta = crosstalkRuta.add(Utils.toDB(Utils.XT(v_crosstalk, crosstalkPerUnitLength, link.getDistance())));
                                            crosstalkFSList.set(crosstalkFsListIndex, crosstalkRuta);
                                        }
                                        core = cores;
                                        // halla el enlace de mayor longitud
                                        if (link.getDistance() > D)
                                            D = link.getDistance();
                                             
                                        // Si todos los enlaces tienen el mismo bloque de FS libre, se agrega la ruta a la lista de rutas establecidas.
                                        if (enlacesLibres.size() == ksp.getEdgeList().size()) {
                                            kspPlaced.add(kspaths.get(selectedIndex));
                                            kspPlacedCores.add(kspCores);
                                            k = kspaths.size();
                                            i = capacity;
                                        }
                                    }
                                    else{
                                        flag_crosstalk = true;
                                        contador1++;
                                        //SimulatorTest.contador_crosstalk ++;
                                    }
                                }
                                else{
                                    flag_crosstalk = true;
                                    contador2++;
                                    //SimulatorTest.contador_crosstalk ++;
                                }

                            }
                            else{
                                flag_frag = true;
                                contador1++;
                                //SimulatorTest.contador_frag ++;
                            }
                        }
                    }
                }

                if (enlacesLibres.size() != ksp.getEdgeList().size()) {
                    flag_capacidad =true;

                    
                }
 
            }
            k++;
        }
        EstablishedRoute establisedRoute;
        if (fsIndexBegin != null && !kspPlaced.isEmpty()) {
            establisedRoute = new EstablishedRoute(kspPlaced.get(0).getEdgeList(),
                    fsIndexBegin, demand.getFs(), demand.getLifetime(),
                    demand.getSource(), demand.getDestination(), kspPlacedCores.get(0),selectedIndex,D,kspPlacedVecinosCrosstalk);
        } else {


           if(flag_capacidad == true){
               System.out.println("bloqueo por capacidad\n");
               // el contador real de bloqueos de la red , porque cuando se produce un bloqueo 
               //es por no completar la cantidad de enlaces para una ruta candidata.
               SimulatorTest.contador_frag_ruta++;
           }
           if(contador1>contador2){
            SimulatorTest.contador_frag++;
            contador1 =0;
            
           }
           else if(contador1<contador2){
            SimulatorTest.contador_crosstalk++;
            contador2=0;
           }
           if(flag_crosstalk == true){
               //SimulatorTest.contador_crosstalk++;
           }
      
           if(flag_frag == true){
               //SimulatorTest.contador_frag++;
           }


            //System.out.println("Bloqueo");
            establisedRoute = null;
        }
        return establisedRoute;

    }

    private static Boolean isFSBlockFree(List<FrequencySlot> bloqueFS) {
        for (FrequencySlot fs : bloqueFS) {
            if (!fs.isFree()) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isCrosstalkFree(FrequencySlot fs, BigDecimal maxCrosstalk, BigDecimal crosstalkRuta) {
        BigDecimal crosstalkActual = crosstalkRuta.add(fs.getCrosstalk());
        return crosstalkActual.compareTo(maxCrosstalk) <= 0;
    }

    private static Boolean isFsBlockCrosstalkFree(List<FrequencySlot> fss, BigDecimal maxCrosstalk, List<BigDecimal> crosstalkRuta) {
        for (int i = 0; i < fss.size(); i++) {
            BigDecimal crosstalkActual = crosstalkRuta.get(i).add(fss.get(i).getCrosstalk());
            if (crosstalkActual.compareTo(maxCrosstalk) > 0) {
                return false;
            }
        }
        return true;
    }

    private static Boolean isNextToCrosstalkFreeCores(Link link, BigDecimal maxCrosstalk, Integer core, Integer fsIndexBegin, Integer fsWidth, Double crosstalkPerUnitLength) {
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                    BigDecimal crosstalkASumar = Utils.toDB(Utils.XT(Utils.getCantidadVecinos(core), crosstalkPerUnitLength, link.getDistance()));
                    BigDecimal crosstalk = fsVecino.getCrosstalk().add(crosstalkASumar);
                    //BigDecimal crosstalkDB = Utils.toDB(crosstalk.doubleValue());
                    if (crosstalk.compareTo(maxCrosstalk) >= 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /****
     * Funcion que retorna la cantidad de vecinos
     * afectados por el crosstalk , y que se deben tener encuenta para el calculo
     * del crosstalk de la ruta
     * 
     *@param  link , la el enlace analizado
     *@param  core , cantidad de nucleos de la fibra = 7
     *@param  fsIndexBegin , el indice de la ranura inicial del bloque de ranuras candidatas
     *@param  fsWidth, cantidad de ranuras necesarias para la demanda
     *  
     *@return cantidad de vecinos a tener en cuenta en el calculo del crosstalk de la red.
     */


    private static int CalculaVecinosConCrosstalk(Link link, Integer core, Integer fsIndexBegin, Integer fsWidth){
        //variable auxiliar donde se guarda la cantidad de vecinos que si son afectados por el crosstalk.
        Integer vecino_afectado = 0;  
        List<Integer> vecinos = Utils.getCoreVecinos(core);
        
        for (Integer coreVecino : vecinos) {
            for (Integer i = fsIndexBegin; i < fsIndexBegin + fsWidth; i++) {
                FrequencySlot fsVecino = link.getCores().get(coreVecino).getFrequencySlots().get(i);
                if (!fsVecino.isFree()) {
                   vecino_afectado ++;
                   // Salir del for interno y continuar con el siguiente coreVecino
                   break;
                }
                
            }
        }


        return (vecino_afectado + 1);
    }
}
