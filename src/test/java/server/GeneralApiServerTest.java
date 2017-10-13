package server;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneralApiServerTest {
    private static final TestEnv TEST_ENV = new TestEnv();

    @BeforeAll
    static void setUpAll() throws Exception {
        TEST_ENV.setUpAll();
    }

    @BeforeEach
    void setUp() throws Exception {
        TEST_ENV.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        TEST_ENV.tearDown();
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        TEST_ENV.tearDownAll();
    }

    @Test
    void testRequestToUnknownEndPoint_ReturnNotFoundHttpError() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/xxxyyyzzz");

        assertEquals(HttpStatus.NOT_FOUND_404, res.getStatus());
    }

    @Test
    void testUnknownHttpMethodRequestReturnNotFoundHttpError() throws Exception {
        ContentResponse res = TEST_ENV.httpClient()
                .newRequest("http://localhost:4567/accounts").method(HttpMethod.PUT).send();

        assertEquals(HttpStatus.NOT_FOUND_404, res.getStatus());
    }
}
