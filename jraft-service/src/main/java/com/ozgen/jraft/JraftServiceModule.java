package com.ozgen.jraft;

import com.google.inject.AbstractModule;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.node.DefaultNodeServer;
import com.ozgen.jraft.service.MessageHandlerService;
import com.ozgen.jraft.service.NodeHandlerService;
import com.ozgen.jraft.service.impl.DefaultMessageService;
import com.ozgen.jraft.service.impl.DefaultNodeHandlerService;

public class JraftServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new JRaftLibModule());

        bind(MessageHandlerServiceGrpc.MessageHandlerServiceImplBase.class).to(GrpcMessageHandlerServiceImpl.class);
        bind(DefaultNodeServer.class).to(NodeServer.class);
        bind(MessageHandlerService.class).to(DefaultMessageService.class);
        bind(NodeHandlerService.class).to(DefaultNodeHandlerService.class);
    }
}
