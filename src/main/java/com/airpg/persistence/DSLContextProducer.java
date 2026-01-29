package com.airpg.persistence;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * CDI producer for jOOQ DSLContext.
 * Creates a DSLContext using the Quarkus-managed datasource.
 */
@ApplicationScoped
public class DSLContextProducer {

    @Inject
    AgroalDataSource dataSource;

    @Produces
    @ApplicationScoped
    public DSLContext produceDSLContext() {
        return DSL.using(dataSource, SQLDialect.H2);
    }
}
