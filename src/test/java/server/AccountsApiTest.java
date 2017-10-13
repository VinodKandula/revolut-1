package server;

import com.google.gson.Gson;
import model.ErrorMessage;
import model.Transfer;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountsApiTest {
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
    void testGetAccounts_ReturnAllAccounts() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/accounts");

        assertEquals(
                "[{\"id\":1,\"number\":\"acc1\",\"balance\":300.00}," +
                        "{\"id\":2,\"number\":\"acc2\",\"balance\":400.00}," +
                        "{\"id\":3,\"number\":\"acc3\",\"balance\":500.00}]",
                res.getContentAsString());
    }

    @Test
    void testGetAccountById_ReturnAccountWithSameId() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/accounts/1");

        assertEquals("{\"id\":1,\"number\":\"acc1\",\"balance\":300.00}", res.getContentAsString());
    }



    //TODO Get account with missing id:
    //TODO Get transfers for account with missing id

    //TODO Get account with wrong id
    //TODO Get transfers for account with wrong id

    @Test
    void testGetAllTransfersForAccountById_ReturnAccountTransfers() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/accounts/2/transfers");

        assertEquals(
                "[{\"id\":3,\"timestamp\":\"Oct 11, 2017 1:00:00 PM\",\"fromAcc\":{\"id\":2,\"number\":\"acc2\"},\"toAcc\":{\"id\":3,\"number\":\"acc3\"},\"amount\":50.00}," +
                        "{\"id\":2,\"timestamp\":\"Oct 9, 2017 12:00:00 PM\",\"fromAcc\":{\"id\":2,\"number\":\"acc2\"},\"toAcc\":{\"id\":3,\"number\":\"acc3\"},\"amount\":200.00}," +
                        "{\"id\":1,\"timestamp\":\"Oct 8, 2017 11:00:00 AM\",\"fromAcc\":{\"id\":1,\"number\":\"acc1\"},\"toAcc\":{\"id\":2,\"number\":\"acc2\"},\"amount\":100.00}]",
                res.getContentAsString());
    }

    @Test
    void testGetAllTransfersForAccountById_ReturnTransfersSortedByDateDesc() throws Exception {
        ContentResponse res = TEST_ENV.httpClient().GET("http://localhost:4567/accounts/2/transfers");
        Gson gson = new Gson();
        Transfer[] trs = gson.fromJson(res.getContentAsString(), Transfer[].class);

        Assertions.assertTrue(trs[0].timestamp.compareTo(trs[1].timestamp) >= 0);
        Assertions.assertTrue(trs[1].timestamp.compareTo(trs[2].timestamp) >= 0);
    }

    @Test
    void testCreateNewAccount_ReturnCreatedAccount() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"number\":\"acc4\", \"balance\":700}"));
        ContentResponse res = req.send();

        assertEquals("{\"id\":4,\"number\":\"acc4\",\"balance\":700.00}", res.getContentAsString());
    }

    @Test
    void testCreateNewAccount_ReturnCreatedHttpCode() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"number\":\"acc4\", \"balance\":700}"));
        ContentResponse res = req.send();

        assertEquals(HttpStatus.CREATED_201, res.getStatus());
    }

    @Test
    void testCreateNewAccount_WhenZeroBalance_ReturnCreatedAccount() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"number\":\"acc4\", \"balance\":0}"));
        ContentResponse res = req.send();

        assertEquals("{\"id\":4,\"number\":\"acc4\",\"balance\":0.00}", res.getContentAsString());
    }

    @Test
    void testCreateNewAccount_WhenNegativeBalance_ReturnValidationError() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"balance\":-100}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, res.getStatus());
        assertEquals(e.msg, "Validation error");
    }

    @Test
    void testCreateNewAccount_WhenMissedName_ReturnValidationError() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"balance\":700}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, res.getStatus());
        assertEquals(e.msg, "Validation error");
    }

    @Test
    void testCreateNewAccount_WhenEmptyName_ReturnValidationError() throws Exception {
        Request req = TEST_ENV.httpClient().POST("http://localhost:4567/accounts");
        req.content(new StringContentProvider("{\"number\":\"\",\"balance\":700}"));
        ContentResponse res = req.send();

        Gson gson = new Gson();
        ErrorMessage e = gson.fromJson(res.getContentAsString(), ErrorMessage.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, res.getStatus());
        assertEquals(e.msg, "Validation error");
    }
}