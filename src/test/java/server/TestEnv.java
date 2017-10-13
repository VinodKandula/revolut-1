package server;

import db.MemoryDatabase;
import org.eclipse.jetty.client.HttpClient;
import org.jooq.DSLContext;
import spark.Spark;

public class TestEnv {
    //TODO Rename server --> restServer
    private final TestMemoryDatabaseWrapper db = new TestMemoryDatabaseWrapper();
    private final RestApiServer server = new RestApiServer(db);
    private final HttpClient httpClient = new HttpClient();

    public void setUpAll() throws Exception {
        httpClient.start();

        server.start();
        Spark.awaitInitialization();
    }

    public void setUp() throws Exception {
        db.wrap(new MemoryDatabase("jdbc:h2:mem:test;", "/h2/test-data.sql"));
    }

    public void tearDown() throws Exception {
        db.shutdown();
    }

    public void tearDownAll() throws Exception {
        httpClient.stop();

        Spark.stop();
        //Wait, until Spark server is fully stopped
        Thread.sleep(2000);
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public static class TestMemoryDatabaseWrapper extends MemoryDatabase {
        private MemoryDatabase holded;

        public TestMemoryDatabaseWrapper() {}

        public TestMemoryDatabaseWrapper(MemoryDatabase memoryDatabase) {
            holded = memoryDatabase;
        }

        public void wrap(MemoryDatabase memoryDatabase) {
            this.holded = memoryDatabase;
        }

        public void shutdown() {
            holded.ctx().execute("SHUTDOWN IMMEDIATELY");
        }

        @Override
        public DSLContext ctx() {
            return holded.ctx();
        }
    }
}
