package server;

import com.google.gson.Gson;
import db.MemoryDatabase;
import model.*;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.routes.AccountsRoutes;
import server.routes.TransfersRoutes;
import services.AccountsService;
import services.TransfersService;
import spark.ResponseTransformer;

import static spark.Spark.*;

public class RestApiServer {
    private final static Logger logger = LoggerFactory.getLogger(RestApiServer.class);

    private final AccountsRoutes accRoutes;
    private final TransfersRoutes trToutes;
    private final JsonTransformer json = new JsonTransformer();

    public RestApiServer(MemoryDatabase db) {
        accRoutes = new AccountsRoutes(new AccountsService(db));
        trToutes = new TransfersRoutes(new TransfersService(db));
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
        get("/transfers", trToutes.getTransfers(), json);

        get("/transfers/:id", trToutes.getTransferById(), json);

        post("/transfers", trToutes.postTransfer(), json);
    }

    //TODO Move to responses.ContentHandler
    private void registerResponseContentHandler() {
        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
            response.type("application/json");
        });
    }

    //TODO Move to responses.ErrorsHandler
    private void registerErrorsHandler() {
        exception(Exception.class, (e, request, response) -> {
            ErrorMessage error;

            if (e instanceof Validator.ValidationException) {
                response.status(422);

                error = new ErrorMessage("Validation error", e.getMessage());
            } else if (e instanceof NoDataFoundException) {
                response.status(404);

                error = new ErrorMessage("Requested entity not found", e.getMessage());
            } else if (e instanceof DataAccessException) {
                response.status(500);

                error = new ErrorMessage("Data access error", e.getMessage());
            } else {
                response.status(500);

                error = new ErrorMessage("Internal server error", e.getMessage());
            }

            logger.error("Handled server exception", e);

            JsonTransformer json = new JsonTransformer();
            response.body(json.render(error));
        });
    }

    //TODO Move to responses.JsonTransformer
    private static class JsonTransformer implements ResponseTransformer {
        @Override
        public String render(Object model) {
            Gson gson = new Gson();

            return gson.toJson(model);
        }
    }

    public static void main(String[] args) {
        MemoryDatabase db = new MemoryDatabase();
        RestApiServer server = new RestApiServer(db);

        server.start();
    }
}

