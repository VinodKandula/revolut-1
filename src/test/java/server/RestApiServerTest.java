package server;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;
import service.AccountsService;
import service.TransfersService;
import spark.Spark;

import java.sql.Connection;
import java.sql.DriverManager;

class RestApiServerTest {
    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:transfers;" +
            "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/test-data.sql';";

    private static final RestApiServer server = new RestApiServer();
    private static final HttpClient httpClient = new HttpClient();

    private Connection connection;

    @BeforeAll
    static void setUpAll() throws Exception {
        Class.forName(JDBC_DRIVER);

        httpClient.start();

        server.start();
        Spark.awaitInitialization();

    }

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL);

        DSLContext ctx = DSL.using(connection, SQLDialect.H2);
        server.setAccountsService(new AccountsService(ctx));
        server.setTransfersService(new TransfersService(ctx));
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        httpClient.stop();

        Spark.stop();
    }

    @Test
    void testInitial() throws Exception {
        System.out.println("testInitial1");
        ContentResponse response = httpClient.GET("http://localhost:4567/accounts");
        System.out.println(response.getContentAsString());
    }

    @Test
    void testAddNewAccount() throws Exception {
        System.out.println("testAddNewAccount");
        ContentResponse response = httpClient.GET("http://localhost:4567/accounts");
        System.out.println(response.getContentAsString());

        Request r = httpClient.POST("http://localhost:4567/accounts");
        r.header(HttpHeader.CONTENT_TYPE, "application/json");
        r.content(new StringContentProvider("{\"number\":\"acc4\",\"balance\":400}"), "application/json");
        r.send();

        response = httpClient.GET("http://localhost:4567/accounts");
        System.out.println(response.getContentAsString());
    }

    @Test
    void testInitial2() throws Exception {
        System.out.println("testInitial2");
        ContentResponse response = httpClient.GET("http://localhost:4567/accounts");
        System.out.println(response.getContentAsString());
    }

}