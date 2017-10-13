package server;

import db.MemoryDatabase;
import server.handlers.ContentHandlers;
import server.handlers.ErrorsHandler;
import server.json.JsonTransformer;
import server.routes.AccountsRoutes;
import server.routes.TransfersRoutes;
import services.AccountsService;
import services.TransfersService;

import static spark.Spark.*;

public class RestApiServer {
    private final ErrorsHandler errorsHandler = new ErrorsHandler();
    private final ContentHandlers contentHandlers = new ContentHandlers();
    private final JsonTransformer json = new JsonTransformer();

    private final AccountsRoutes accRoutes;
    private final TransfersRoutes trRoutes;

    public RestApiServer(MemoryDatabase db) {
        accRoutes = new AccountsRoutes(new AccountsService(db));
        trRoutes = new TransfersRoutes(new TransfersService(db));
    }

    public void start() {
        buildAccountsApi();
        buildTransfersApi();

        registerResponseContentHandler();
        registerErrorsHandler();
    }

    private void buildAccountsApi() {
        get("/accounts", accRoutes.getAccountsRoute(), json);

        get("/accounts/:id", accRoutes.getAccountById(), json);

        get("/accounts/:id/transfers", accRoutes.getAccountTransfersById(), json);

        post("/accounts", accRoutes.postAccount(), json);
    }

    private void buildTransfersApi() {
        get("/transfers", trRoutes.getTransfers(), json);

        get("/transfers/:id", trRoutes.getTransferById(), json);

        post("/transfers", trRoutes.postTransfer(), json);
    }

    private void registerResponseContentHandler() {
        after(contentHandlers.getContentHandler());
    }

    private void registerErrorsHandler() {
        exception(Exception.class, errorsHandler.exceptionsHandler());
    }

    public static void main(String[] args) {
        MemoryDatabase db = new MemoryDatabase();
        RestApiServer server = new RestApiServer(db);

        server.start();
    }
}

