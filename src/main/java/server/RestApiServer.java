package server;

import com.google.gson.Gson;
import model.*;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AccountsService;
import service.TransfersService;
import spark.ResponseTransformer;

import static spark.Spark.*;

public class RestApiServer {
    private final static Logger logger = LoggerFactory.getLogger(RestApiServer.class);

    private static final String MEM_DB_URL = "jdbc:h2:mem:transfers;DB_CLOSE_DELAY=-1;MULTI_THREADED=1;";
    private static final String DB_INIT = "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/data.sql';";

    private AccountsService accountsService;
    private TransfersService transfersService;

    public RestApiServer() {
        JdbcConnectionPool pool = JdbcConnectionPool.create(MEM_DB_URL + DB_INIT, "sa", "");
        DSLContext ctx = DSL.using(pool, SQLDialect.H2);

        accountsService = new AccountsService(ctx);
        transfersService = new TransfersService(ctx);
    }

    public void setAccountsService(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    public void setTransfersService(TransfersService transfersService) {
        this.transfersService = transfersService;
    }

    public void start() {
        accountsApi();
        transfersApi();

        afterHandler();
        exceptionsHandler();
    }

    private void accountsApi() {
        get("/accounts",
                (request, response) -> accountsService.getAllAccounts(),
                new JsonTransformer());

        get("/accounts/:id",
                (request, response) -> {
                    String id = request.params(":id");
                    Validator.validateNumber(id);

                    long accId = Long.parseLong(id);
                    Validator.validateId(accId);

                    return accountsService.getAccount(accId);
                },
                new JsonTransformer());

        get("/accounts/:id/transfers",
                (request, response) ->  {
                    String id = request.params(":id");
                    Validator.validateNumber(id);

                    long accId = Long.parseLong(id);
                    Validator.validateId(accId);

                    return accountsService.getAccountTransfers(accId);
                },
                new JsonTransformer());

        post("/accounts", (request, response) -> {
            Gson gson = new Gson();
            AccountCreation acc = gson.fromJson(request.body(), AccountCreation.class);
            Validator.validateAccountCreation(acc);

            Account account = accountsService.createAccount(acc);

            response.status(201);

            return account;

        }, new JsonTransformer());
    }

    private void transfersApi() {
        get("/transfers",
                (request, response) -> transfersService.getAllTransfers(),
                new JsonTransformer());

        get("/transfers/:id",
                (request, response) -> {
                    String id = request.params(":id");
                    Validator.validateNumber(id);

                    long trId = Long.parseLong(id);
                    Validator.validateId(trId);

                    return transfersService.getTransfer(trId);
                },
                new JsonTransformer());

        post("/transfers",
                (request, response) -> {
                    Gson gson = new Gson();
                    TransferRequest trReq = gson.fromJson(request.body(), TransferRequest.class);
                    Validator.validateTransferRequest(trReq);

                    Transfer transfer = transfersService.transferAmount(trReq);

                    response.status(201);

                    return transfer;
                },
                new JsonTransformer());
    }

    private void afterHandler() {
        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
            response.type("application/json");
        });
    }

    private void exceptionsHandler() {
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

    public static void main(String[] args) {
        RestApiServer server = new RestApiServer();

        server.start();
    }

    private static class JsonTransformer implements ResponseTransformer {
        private final Gson gson = new Gson();

        @Override
        public String render(Object model) {
            return gson.toJson(model);
        }
    }
}

