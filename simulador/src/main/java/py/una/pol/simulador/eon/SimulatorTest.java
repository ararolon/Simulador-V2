/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package py.una.pol.simulador.eon;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;

import py.una.pol.simulador.eon.models.AssignFsResponse;
import py.una.pol.simulador.eon.models.Demand;
import py.una.pol.simulador.eon.models.EstablishedRoute;
import py.una.pol.simulador.eon.models.Input;
import py.una.pol.simulador.eon.models.Link;
import py.una.pol.simulador.eon.models.enums.RSAEnum;
import py.una.pol.simulador.eon.models.enums.TopologiesEnum;
import py.una.pol.simulador.eon.rsa.Algorithms;
import py.una.pol.simulador.eon.utils.MathUtils;
import py.una.pol.simulador.eon.utils.Utils;
import py.una.pol.simulador.eon.utils.GraphUtils;

/**
 *
 * @author Néstor E. Reinoso Wood
 */
public class SimulatorTest {

/*
    * Variables globales
    * para identificar tipos de bloqueos
    */
    public static int contador_crosstalk = 0;
    public static int contador_frag= 0;
    public static int contador_frag_ruta = 0;


    /**
     * Configuración inicial para el simulador
     *
     * @param erlang Erlang para la simulación
     * @return Datos de entrada del simulador
     */

    private Input getTestingInput(Integer erlang) {
        Input input = new Input();
        /*
         * Declaro las variables iniciales 
         * 
         */


        input.setDemands(100000);
        input.setTopologies(new ArrayList<>());
        input.getTopologies().add(TopologiesEnum.NSFNET);
        //input.getTopologies().add(TopologiesEnum.USNET);
        //input.getTopologies().add(TopologiesEnum.JPNNET);
        input.setFsWidth(new BigDecimal("12.5"));
        input.setFsRangeMax(8);
        input.setFsRangeMin(2);
        input.setCapacity(325); 
        input.setCores(7);
        input.setLambda(5);
        input.setErlang(erlang);
        input.setAlgorithms(new ArrayList<>());
        //input.getAlgorithms().add(RSAEnum.CORE_UNICO);
        input.getAlgorithms().add(RSAEnum.MULTIPLES_CORES);
        input.setSimulationTime(MathUtils.getSimulationTime(input.getDemands(), input.getLambda()));
        input.setMaxCrosstalk(new BigDecimal("0.003162277660168379331998893544")); // XT = -25 dB
        //input.setMaxCrosstalk(new BigDecimal("0.031622776601683793319988935444")); // XT = -15 dB
        input.setCrosstalkPerUnitLenghtList(new ArrayList<>());
        input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0035, 2) * 0.080) / (4000000 * 0.000045));
        //input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.00040, 2) * 0.050) / (4000000 * 0.000040));
        //input.getCrosstalkPerUnitLenghtList().add((2 * Math.pow(0.0000316, 2) * 0.055) / (4000000 * 0.000045));
        //input.setNumero_h("h1");
        //input.setNumero_h("h2");
        input.setNumero_h("h3");

        return input;
    }

    /**
     * Simulador
     *
     * @param args Argumentos de entrada (Vacío)
     */
    public static void main(String[] args) {
        try {
            //Bases de datos
            //createTable();
            CreateDataBase();
            // Datos de entrada
            for (int erlang = 2000; erlang <= 2000; erlang = erlang + 1000) {
               
                Input input = new SimulatorTest().getTestingInput(erlang);
                for (TopologiesEnum topology : input.getTopologies()) {

                    // Se genera la red de acuerdo a los datos de entrada
                    Graph<Integer, Link> graph = Utils.createTopology(topology,
                            input.getCores(), input.getFsWidth(), input.getCapacity());

                    GraphUtils.createImage(graph, topology.label());
                    // Contador de demandas utilizado para identificación
                    Integer demandsQ = 1;
                    List<List<Demand>> listaDemandas = new ArrayList<>();
                    for (int i = 0; i < input.getSimulationTime(); i++) {
                        List<Demand> demands = Utils.generateDemands(input.getLambda(),
                                input.getSimulationTime(), input.getFsRangeMin(),
                                input.getFsRangeMax(), graph.vertexSet().size(),
                                input.getErlang() / input.getLambda(), demandsQ, i);

                        demandsQ += demands.size();
                        listaDemandas.add(demands);
                    }

                    for (Double crosstalkPerUnitLength : input.getCrosstalkPerUnitLenghtList()) {
                        for (RSAEnum algorithm : input.getAlgorithms()) {
                            graph = Utils.createTopology(topology,
                                    input.getCores(), input.getFsWidth(), input.getCapacity());
                            // Lista de rutas establecidas durante la simulación
                            List<EstablishedRoute> establishedRoutes = new ArrayList<>();
                            System.out.println("Inicializando simulación del RSA " + algorithm.label() + " para erlang: " + (erlang) + " para la topología " + topology.label() + " y H = " + crosstalkPerUnitLength.toString());
                            int demandaNumero = 1;
                            Integer camino = null;
                            int rutas = 0; 
                            int bloqueos = 0;
                            //Declaro las variables auxiliares para verificar el camino tomado 
                             Integer k1 = 0,k2 = 0, k3 = 0 , k4 = 0, k5 =0;

                            // Diametro del grafo
                            Integer Diametro = 0 ;
                            //Variables para calcular el promedio del grado del grafo
                            int prom_grado = 0; //valor promedio del grado del grafo
                            int grado_grafo = 0; //grado del grafo

                            for(int vertex = 0; vertex < graph.vertexSet().size(); vertex++){
                                grado_grafo = grado_grafo + graph.degreeOf(vertex);
                            }

                            prom_grado = (grado_grafo/graph.vertexSet().size());

                            
                            


                            // Iteración de unidades de tiempo
                            for (int i = 0; i < input.getSimulationTime(); i++) {
                                System.out.println("Tiempo: " + (i + 1));
                                // Generación de demandas para la unidad de tiempo
                                List<Demand> demands = listaDemandas.get(i);
                                //System.out.println("Demandas a insertar: " + demands.size());
                                for (Demand demand : demands) {
                                    demandaNumero++;
                                    //System.out.println("Insertando demanda " + demandaNumero++);
                                    //k caminos más cortos entre source y destination de la demanda actual

                                    EstablishedRoute establishedRoute;
                                    switch (algorithm) {

                                        case MULTIPLES_CORES -> {
                                            establishedRoute = Algorithms.ruteoCoreMultiple(graph, demand, input.getCapacity(), input.getCores(), input.getMaxCrosstalk(), crosstalkPerUnitLength);
                                        }
                                        default -> {
                                            establishedRoute = null;
                                        }
                                    }

                                    if (establishedRoute == null || establishedRoute.getFsIndexBegin() == -1) {
                                        //Bloqueo
                                        System.out.println("BLOQUEO");
                                        demand.setBlocked(true);
                                        insertData(algorithm.label(), topology.label(), "" + i, "" + demand.getId(), "" + erlang, crosstalkPerUnitLength.toString());
                                        bloqueos++;
                                    } else {

                                       camino = establishedRoute.getK_elegido();
                                    
                                       switch (camino) {
                                           case 0 -> k1++;
                                              
                                           case 1 -> k2++;
                                            
                                           case 2 -> k3++;
                                           
                                           case 3 -> k4++;
                                           
                                           default -> k5++;
                                       }
                                        
                                        // va buscando y guardando el diametro mayor entre las rutas.

                                        if(establishedRoute.getDiametro() > Diametro)
                                            Diametro = establishedRoute.getDiametro();

                                        rutas++;
                                        System.out.println("Ruta");
                                        //System.out.println("Cores: " + establishedRoute.getPathCores());
                                        AssignFsResponse response = Utils.assignFs(graph, establishedRoute, crosstalkPerUnitLength);
                                        establishedRoute = response.getRoute();
                                        graph = response.getGraph();
                                        establishedRoutes.add(establishedRoute);
                                    }

                                }

                                for (EstablishedRoute route : establishedRoutes) {
                                    route.subLifeTime();
                                }

                                for (int ri = 0; ri < establishedRoutes.size(); ri++) {
                                    EstablishedRoute route = establishedRoutes.get(ri);
                                    if (route.getLifetime().equals(0)) {
                                        Utils.deallocateFs(graph, route, crosstalkPerUnitLength);
                                        establishedRoutes.remove(ri);
                                        ri--;
                                    }
                                }
                            }

                            //Determina los datos para ingresar a la base de datos

                            String tipo_erlang;

                            if(erlang<1000){
                                tipo_erlang = "BAJO";
                            }
                            else if( erlang>1000 && erlang< 2000){
                                tipo_erlang = "MEDIO";
                            }
                            else{
                                tipo_erlang = "ALTO";

                            }

                            // los motivos de bloqueos

                            String motivo_bloqueo = MotivoBloqueo(contador_frag, contador_crosstalk);

                            String porcentaje = PorcentajeBloqueo(bloqueos, contador_frag, contador_crosstalk);

                        
                                

                            //InsertaDatos(topology.label(), "" + erlang, tipo_erlang, input.getNumero_h(), crosstalkPerUnitLength.toString(), "" + bloqueos, motivo_bloqueo, porcentaje, "" + rutas, "" + Diametro, "" + prom_grado);
                            
                            System.out.println("---------------------------------");
                            System.out.println("\nTopologia" + input.getTopologies()+"\n");
                            System.out.println("TOTAL DE BLOQUEOS: " + bloqueos);
                            System.out.println("TOTAL DE RUTAS: " + rutas);
                            System.out.println("Cantidad de demandas: " + demandaNumero);
                            System.out.println("\nRESUMEN DE DATOS \n");
                            System.out.printf("Resumen de caminos:\nk1:%d\nk2:%d\nk3:%d\nk4:%d\nk5:%d\n",k1,k2,k3,k4,k5);
                            System.out.printf("Resumen de bloqueos:\n fragmentacion = %d \n crosstalk = %d\n fragmentacion de camino = %d",contador_frag,contador_crosstalk,contador_frag_ruta);
                            System.out.printf("\nEl diametro del grafo es :  %d kms\n",Diametro);
                            System.out.printf("\nEl grado promedio: %d",prom_grado);
                            System.out.printf("\n entra en crosstalk %d",SimulatorTest.contador_crosstalk);
                            System.out.println(System.lineSeparator());
                        }
                    }
                }
            }
        }catch (IOException | IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }


    /**
     * Funcion que retorna el motivo de fragmentacion de la red
     * @param  contador1 es el contador de cantidades de bloqueos por fragmentacion
     * @param  contador2 es el contador de cantidades de bloqueos por crosstalk
     * @return  Motivo de fragmentacion de la red
     * 
     */

    public static String MotivoBloqueo(int contador1, int contador2){

        String motivo_bloqueo;

        if(contador1 > contador2){
            motivo_bloqueo = "Fragmentacion";
        }
        else if (contador1 < contador2){
            motivo_bloqueo = "Crosstalk";
        }
        else{
            motivo_bloqueo = "Crosstalk y Fragmentacion";
        }

        return motivo_bloqueo;

     }

   
    /***
     * Funcion que devuelve el porcentaje de bloqueo de la red respecto al motivo de bloqueo
     * @param  bloqueos Cantidad de bloqueos de la red
     * @param contador1 Cantidad de bloqueos por fragmentacion en la red
     * @param contador2 Cantidad de bloqueos por crosstalk en la red
     * 
     * @return  el porcentaje de bloqueo.
     * 
     */

    public static String  PorcentajeBloqueo(int bloqueos,int contador1, int contador2 ){

        String porcentaje = "";
        float p_frag,p_crosstalk;
        
        if(contador1 == 0 || contador2 == 0){
            porcentaje = "100";
        }
        else if( contador1 > 0 && contador2 > 0){
        
        p_frag = (contador1*100)/bloqueos;
        p_crosstalk = (contador2*100)/bloqueos;

        porcentaje = "" + p_frag + " fragmentacion "+ " y " + p_crosstalk + "crosstalk";

        }

        return porcentaje;

    }

    /**
     * Inserta los datos en la BD
     *
     * @param rsa Algoritmo RSA utilizado
     * @param topologia Topología de la red
     * @param tiempo Tiempo del bloqueo
     * @param demanda Demanda bloqueada
     * @param erlang Erlang de la simulación
     * @param h Crosstalk por unidad de longitud de la simulación
     */
    public static void insertData(String rsa, String topologia, String tiempo, String demanda, String erlang, String h) {
        Connection c;

        Statement stmt;

        try {

            Class.forName("org.sqlite.JDBC");

            c = DriverManager.getConnection("jdbc:sqlite:simulador.db");

            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "INSERT INTO Bloqueos (rsa, topologia, tiempo, demanda, erlang, h) "
                    + "VALUES ('" + rsa + "','" + topologia + "', '" + tiempo + "' ,'" + demanda + "', " + "'" + erlang + "', " + "'" + h + "')";
            stmt.executeUpdate(sql);
            stmt.close();
            c.commit();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    /**
     * Generación de la tabla de resultados
     */
    public static void createTable() {
        Connection c;

        Statement stmt;

        try {

            Class.forName("org.sqlite.JDBC");

            c = DriverManager.getConnection("jdbc:sqlite:simulador.db");

            System.out.println("Database Opened...\n");

            stmt = c.createStatement();

            String dropTable = "DROP TABLE Bloqueos ";

            String sql = "CREATE TABLE IF NOT EXISTS Bloqueos "
                    + "("
                    + "erlang TEXT NOT NULL, "
                    + "rsa TEXT NOT NULL, "
                    + " topologia TEXT NOT NULL, "
                    + " h TEXT NOT NULL, "
                    + " tiempo TEXT NOT NULL, "
                    + " demanda TEXT NOT NULL) ";
            try {
                stmt.executeUpdate(dropTable);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }


        
    /*Funcion para crear la base de datos donde se van a guardar los Resumenes obtenidos
    de simulacion de las diferentes topologias*/
    public static void CreateDataBase() {
        
        Connection conexion;

        Statement stmt;

        try {

            Class.forName("org.sqlite.JDBC");

            conexion = DriverManager.getConnection("jdbc:sqlite:Resumen.db");

            System.out.println("\n...Creando Base de datos para resumen de datos...\n");

            stmt = conexion.createStatement();

            //String dropTable = "DROP TABLE Resumen ";


            String sql = "CREATE TABLE IF NOT EXISTS Resumen "
                    + "("
                    + "topologia TEXT NOT NULL, "
                    + "erlang TEXT NOT NULL, "
                    + "tipo_erlang TEXT NOT NULL, "
                    + "h TEXT NOT NULL, "
                    + "valor_h tiempo TEXT NOT NULL, "
                    + "bloqueos TEXT NOT NULL, "
                    + "motivo_Bloqueo TEXT NOT NULL, "
                    + "porcentaje_Bloqueo TEXT NOT NULL, "
                    + "rutas TEXT NOT NULL, "
                    + "diametro TEXT NOT NULL, "
                    + "grado TEXT NOT NULL) ";
            /*try {
                stmt.executeUpdate(dropTable);
            }catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }*/
            stmt.executeUpdate(sql);
            stmt.close();
            conexion.close();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
   
    /**
     * Funcion que inserta los datos en la Base de datos de resumen
     * @param  topologia
     * 
     * 
     */

    public static void InsertaDatos(String topologia, String erlang, String tipo_erlang, String h, String valor_h,
                                    String bloqueos, String motivo_Bloqueo, String porcentaje_Bloqueo,
                                    String rutas, String diametro, String grado) {
        Connection conexion = null;
        PreparedStatement stmt = null;

        try {
            // Cargar el driver de SQLite
            Class.forName("org.sqlite.JDBC");

            // Establecer conexión con la base de datos SQLite
            conexion = DriverManager.getConnection("jdbc:sqlite:Resumen.db");

            // Desactivar auto-commit para control de transacciones
            conexion.setAutoCommit(false);

            // Consulta SQL con placeholders (?) para evitar errores de sintaxis e inyección SQL
            String sql = "INSERT INTO Resumen (topologia, erlang, tipo_erlang, h, valor_h, bloqueos, motivo_Bloqueo, porcentaje_Bloqueo, rutas, diametro, grado) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Crear el PreparedStatement y asignar valores
            stmt = conexion.prepareStatement(sql);
            stmt.setString(1, topologia);
            stmt.setString(2, erlang);
            stmt.setString(3, tipo_erlang);
            stmt.setString(4, h);
            stmt.setString(5, valor_h);
            stmt.setString(6, bloqueos);
            stmt.setString(7, motivo_Bloqueo);
            stmt.setString(8, porcentaje_Bloqueo);
            stmt.setString(9, rutas);
            stmt.setString(10, diametro);
            stmt.setString(11, grado);

            // Ejecutar la inserción
            stmt.executeUpdate();

            // Confirmar la transacción
            conexion.commit();

            System.out.println("¡Datos insertados correctamente!");

        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error: " + e.getMessage());
        
            try {
                if (conexion != null) {
                    conexion.rollback();  // Deshacer cambios en caso de error
                }
            } catch (SQLException rollbackEx) {
                    System.out.println("Error al hacer rollback: " + rollbackEx.getMessage());
            }
        } 
        finally {
            try {
                if (stmt != null) stmt.close();
                if (conexion != null) conexion.close();
            } catch (SQLException closeEx) {
                System.out.println("Error al cerrar la conexión: " + closeEx.getMessage());
            }
        }
    
    }

}

