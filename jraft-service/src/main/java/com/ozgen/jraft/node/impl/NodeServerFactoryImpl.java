package com.ozgen.jraft.node.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.model.node.NodeData;
import com.ozgen.jraft.node.NodeServer;
import com.ozgen.jraft.node.NodeServerFactory;

import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NodeServerFactoryImpl implements NodeServerFactory {

    private final Injector injector;

    @Inject
    public NodeServerFactoryImpl(Injector injector) {
        this.injector = injector;
    }

    @Override
    public NodeServer createNodeServer(String nodeId, ConcurrentHashMap<String, NodeData> cluster) {
        // Obtain dependencies from injector
        MsgToGrpcConverter msgToGrpcConverter = injector.getInstance(MsgToGrpcConverter.class);
        GrpcToMsgConverter grpcToMsgConverter = injector.getInstance(GrpcToMsgConverter.class);

        // Create a new NodeServer with all required parameters
        return new NodeServer(nodeId, cluster, msgToGrpcConverter, grpcToMsgConverter);
    }
}
