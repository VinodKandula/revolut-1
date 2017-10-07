import db.tables.records.AccountRecord;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import static db.tables.Account.ACCOUNT;
import static spark.Spark.get;

public class TransfersServer {
    private static final String DB_URL = "jdbc:h2:mem:transfers;" +
            "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/data.sql';";

    public static void main(String[] args) {
        JdbcConnectionPool pool = JdbcConnectionPool.create(DB_URL, "sa", "");
        DSLContext ctx = DSL.using(pool, SQLDialect.H2);

        Result<AccountRecord> rs = ctx.selectFrom(ACCOUNT).fetch();

        System.out.println(rs.formatJSON());

        get("/create", (req, res) -> {
            print();

            return "Hello World";
        });

        get("/list", (req, res) -> "Hello JRebel");
    }

    private static void print() {

        System.out.println(456);
    }
}