package server;

import com.google.gson.Gson;
import model.ErrorMessage;
import model.Transfer;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransfersApiTest {
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
    void testGetAllTransfers_ReturnAllTransfers() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/transfers");

        assertEquals(
                "[{\"id\":3,\"timestamp\":\"Oct 11, 2017 1:00:00 PM\",\"fromAcc\":{\"id\":2,\"number\":\"acc2\"},\"toAcc\":{\"id\":3,\"number\":\"acc3\"},\"amount\":50.00}," +
                        "{\"id\":2,\"timestamp\":\"Oct 9, 2017 12:00:00 PM\",\"fromAcc\":{\"id\":2,\"number\":\"acc2\"},\"toAcc\":{\"id\":3,\"number\":\"acc3\"},\"amount\":200.00}," +
                        "{\"id\":1,\"timestamp\":\"Oct 8, 2017 11:00:00 AM\",\"fromAcc\":{\"id\":1,\"number\":\"acc1\"},\"toAcc\":{\"id\":2,\"number\":\"acc2\"},\"amount\":100.00}]",
                res.getContentAsString());
    }

    @Test
    void testGetAllTransfers_ReturnTransfersSortedByDateDesc() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/transfers");
        Gson gson = new Gson();
        Transfer[] trs = gson.fromJson(res.getContentAsString(), Transfer[].class);

        assertTrue(trs[0].timestamp.compareTo(trs[1].timestamp) >= 0);
        assertTrue(trs[1].timestamp.compareTo(trs[2].timestamp) >= 0);
    }

    @Test
    void testGetTransferById_ReturnTransferWithSameId() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/transfers/2");

        assertEquals("{\"id\":2,\"timestamp\":\"Oct 9, 2017 12:00:00 PM\"," +
                "\"fromAcc\":{\"id\":2,\"number\":\"acc2\"}," +
                "\"toAcc\":{\"id\":3,\"number\":\"acc3\"},\"amount\":200.00}",
                res.getContentAsString());
    }

    @Test
    void testRequestTransfer_ReturnFulfilledTransferRecord() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/transfers");
        req.content(new StringContentProvider("{\"fromAcc\":1,\"toAcc\":2,\"amount\":100}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        Transfer actual = gson.fromJson(res.getContentAsString(), Transfer.class);
        actual.timestamp = null;

        assertEquals(
                "{\"id\":4," +
                "\"fromAcc\":{\"id\":1,\"number\":\"acc1\"}," +
                "\"toAcc\":{\"id\":2,\"number\":\"acc2\"}," +
                "\"amount\":100.00}",
                gson.toJson(actual));
    }

    @Test
    void testRequestTransfer_WhenInsufficientBalance_ReturnErrorMessage() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/transfers");
        req.content(new StringContentProvider("{\"fromAcc\":1,\"toAcc\":2,\"amount\":600}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(e.msg,"Data access error");
    }

    @Test
    void testRequestTransfer_WhenTransferToItself_ReturnErrorMessage() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/transfers");
        req.content(new StringContentProvider("{\"fromAcc\":1,\"toAcc\":1,\"amount\":600}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(e.msg,"Validation error");
    }

    @Test
    void testRequestTransfer_WhenNonPositiveAmount_ReturnErrorMessage() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/transfers");
        req.content(new StringContentProvider("{\"fromAcc\":1,\"toAcc\":1,\"amount\":-20}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(e.msg,"Validation error");
    }
}
