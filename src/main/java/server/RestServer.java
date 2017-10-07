package server;

import com.google.gson.Gson;
import model.Account;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.util.List;

import static db.tables.Account.ACCOUNT;
import static spark.Spark.get;
import static spark.Spark.staticFiles;

public class RestServer {
    private static final String DB_URL = "jdbc:h2:mem:transfers;" +
            "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/data.sql';";

    public static void main(String[] args) {
        JdbcConnectionPool pool = JdbcConnectionPool.create(DB_URL, "sa", "");
        DSLContext ctx = DSL.using(pool, SQLDialect.H2);

        staticFiles.location("/public");

        Gson gson = new Gson();
        get("/accounts", (req, res) -> {
            List<Account> accs = ctx.selectFrom(ACCOUNT).fetchInto(Account.class);

            return accs;
        }, gson::toJson);

        get("/list", (req, res) -> "Hello JRebel");
    }

    private static void print() {

        System.out.println(456);
    }
}