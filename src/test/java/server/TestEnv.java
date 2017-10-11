package server;

import org.eclipse.jetty.client.HttpClient;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import service.AccountsService;
import service.TransfersService;
import spark.Spark;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestEnv {
    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:transfers;" +
            "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/test-data.sql';";

    private final RestApiServer server = new RestApiServer();
    private final HttpClient httpClient = new HttpClient();

    private Connection connection;

    public void setUpAll() throws Exception {
        Class.forName(JDBC_DRIVER);

        httpClient.start();

        server.start();
        Spark.awaitInitialization();
    }

    public void setUp() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL);

        DSLContext ctx = DSL.using(connection, SQLDialect.H2);
        server.setAccountsService(new AccountsService(ctx));
        server.setTransfersService(new TransfersService(ctx));
    }

    public void tearDown() throws Exception {
        connection.close();
    }

    public void tearDownAll() throws Exception {
        httpClient.stop();

        Spark.stop();
    }

    public HttpClient httpClient() {
        return httpClient;
    }
}
