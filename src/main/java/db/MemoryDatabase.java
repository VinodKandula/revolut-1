package db;

import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class MemoryDatabase {
    private static final String DEFAULT_URL = "jdbc:h2:mem:transfers;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;";
    private static final String SCHEMA_SCRIPT = "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;";

    private final DSLContext dslContext;

    public MemoryDatabase() {
        this("/h2/data.sql");
    }

    public MemoryDatabase(String dataFile) {
        this(DEFAULT_URL, dataFile);
    }

    public MemoryDatabase(String connectionUrl, String dataFile) {
        String composedUrl = connectionUrl + SCHEMA_SCRIPT;
        if (dataFile != null && dataFile.length() > 0)
                composedUrl += "RUNSCRIPT FROM 'classpath:" + dataFile + "';";

        JdbcConnectionPool pool = JdbcConnectionPool.create(composedUrl, "sa", "");
        dslContext = DSL.using(pool, SQLDialect.H2);
    }

    public DSLContext ctx() {
        return dslContext;
    }
}
