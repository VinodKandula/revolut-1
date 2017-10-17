package db;

import org.hsqldb.jdbc.JDBCPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class MemoryDatabase {
    private static final String DEFAULT_URL = "jdbc:hsqldb:res:/hsqldb/transfers";

    private final DSLContext dslContext;

    public MemoryDatabase() {
        JDBCPool pool = new JDBCPool();
        pool.setUrl(DEFAULT_URL);

        dslContext = DSL.using(pool, SQLDialect.HSQLDB);
    }

    public DSLContext ctx() {
        return dslContext;
    }
}
