package com.ozgen.jraft;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.node.NodeServer;
import com.ozgen.jraft.node.NodeServerFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JraftServerMain {
    private static String NODE_ID_1 = "1";
    private static String NODE_ID_2 = "2";
    private static String NODE_ID_3 = "3";
    private static String NODE_ID_4 = "4";
    private static String NODE_ID_5 = "5";
    private static NodeData NODE_1 = new NodeData("127.0.0.1", 8080);
    private static NodeData NODE_2 = new NodeData("127.0.0.1", 8081);
    private static NodeData NODE_3 = new NodeData("127.0.0.1", 8082);
    private static NodeData NODE_4 = new NodeData("127.0.0.1", 8083);
    private static NodeData NODE_5 = new NodeData("127.0.0.1", 8084);

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10); // for 5 servers

        ConcurrentHashMap<String, NodeData> cluster = new ConcurrentHashMap<>();
        cluster.put(NODE_ID_1, NODE_1);
        cluster.put(NODE_ID_2, NODE_2);
        cluster.put(NODE_ID_3, NODE_3);
        cluster.put(NODE_ID_4, NODE_4);
        cluster.put(NODE_ID_5, NODE_5);


        executorService.execute(() -> startServer(8081, NODE_ID_2, cluster));
        executorService.execute(() -> startServer(8082, NODE_ID_3, cluster));
        executorService.execute(() -> startServer(8080, NODE_ID_1, cluster));
        executorService.execute(() -> startServer(8083, NODE_ID_4, cluster));
        executorService.execute(() -> startServer(8084, NODE_ID_5, cluster));

        executorService.shutdown();
    }

    private static void startServer(int port, String nodeId, ConcurrentHashMap<String, NodeData> cluster) {
        // Create a Guice injector
        Injector injector = Guice.createInjector(new JraftServiceModule());
        NodeServerFactory nodeServerFactory = injector.getInstance(NodeServerFactory.class);
        NodeServer nodeServer = nodeServerFactory.createNodeServer(nodeId, cluster);

        GrpcNodeHandlerServiceImpl grpcNodeHandlerService = injector.getInstance(GrpcNodeHandlerServiceImpl.class);
        grpcNodeHandlerService.setNodeServer(nodeServer);
        GrpcMessageHandlerServiceImpl grpcMessageHandlerService = injector.getInstance(GrpcMessageHandlerServiceImpl.class);
        grpcMessageHandlerService.setNodeServer(nodeServer);
        // Create and start the gRPC server
        Server server = null;
        try {
            server = ServerBuilder.forPort(port)
                    .addService(grpcNodeHandlerService)
                    .addService(grpcMessageHandlerService)
                    .build()
                    .start();


            System.out.println("Server started, listening on " + port);
            nodeServer.startElectionTask();

            // Keep the server running
            server.awaitTermination();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
