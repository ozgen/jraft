package com.ozgen.jraft;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.jraft.MessageHandlerServiceGrpc;
import com.ozgen.jraft.node.NodeServerFactory;
import com.ozgen.jraft.node.impl.NodeServerFactoryImpl;
import com.ozgen.jraft.service.MessageHandlerService;
import com.ozgen.jraft.service.NodeHandlerService;
import com.ozgen.jraft.service.impl.DefaultMessageService;
import com.ozgen.jraft.service.impl.DefaultNodeHandlerService;
import jraft.NodeServiceGrpc;

public class JraftServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new JRaftLibModule());

        bind(MessageHandlerServiceGrpc.MessageHandlerServiceImplBase.class).to(GrpcMessageHandlerServiceImpl.class).in(Scopes.SINGLETON);
        bind(NodeServiceGrpc.NodeServiceImplBase.class).to(GrpcNodeHandlerServiceImpl.class).in(Scopes.SINGLETON);
        bind(MessageHandlerService.class).to(DefaultMessageService.class).in(Scopes.SINGLETON);
        bind(NodeHandlerService.class).to(DefaultNodeHandlerService.class).in(Scopes.SINGLETON);
        bind(NodeServerFactory.class).to(NodeServerFactoryImpl.class).in(Scopes.SINGLETON);
    }
}
