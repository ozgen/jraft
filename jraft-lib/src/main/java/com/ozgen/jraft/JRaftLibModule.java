package com.ozgen.jraft;

import com.google.inject.AbstractModule;
import com.ozgen.jraft.converter.GrpcToMsgConverter;
import com.ozgen.jraft.converter.MsgToGrpcConverter;
import com.ozgen.jraft.service.MessageHandlerService;
import com.ozgen.jraft.service.impl.MessageHandlerServiceImpl;

public class JRaftLibModule extends AbstractModule {

    @Override
    protected void configure() {
        // Bind converters
        bind(GrpcToMsgConverter.class).asEagerSingleton();
        bind(MsgToGrpcConverter.class).asEagerSingleton();

        // Bind service implementation to its interface
        bind(MessageHandlerService.class).to(MessageHandlerServiceImpl.class).asEagerSingleton();
    }
}
