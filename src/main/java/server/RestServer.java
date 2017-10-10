package server;

import com.google.gson.Gson;
import model.Account;
import model.Transfer;
import model.TransferRequest;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import service.AccountsService;
import service.TransfersService;
import spark.ResponseTransformer;

import java.util.Optional;

import static spark.Spark.*;

//jdbc:h2:file:/data/sample
//jdbc:h2:tcp://localhost/mem:test
public class RestServer {
    //TODO Use in-memory database when development is finished
    private static final String MEM_DB_URL = "jdbc:h2:mem:transfers;";
    private static final String FILE_DB_URL = "jdbc:h2:file:/Users/ilya/Work/Job Search/Revolut/transfers;";
    private static final String DB_INIT = "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/data.sql';";

    private final AccountsService accountsService;
    private final TransfersService transfersService;

    public RestServer() {
        JdbcConnectionPool pool = JdbcConnectionPool.create(FILE_DB_URL + DB_INIT, "sa", "");
        DSLContext ctx = DSL.using(pool, SQLDialect.H2);

        accountsService = new AccountsService(ctx);
        transfersService = new TransfersService(ctx);
    }

    private void start() {
        get("/accounts",
                (request, response) -> accountsService.getAllAccounts(),
                new JsonTransformer());

        get("/accounts/:id",
                (request, response) -> {
                    Optional<Account> account = accountsService.getAccount(Long.parseLong(request.params(":id")));

                    if (account.isPresent())
                        return account.get();
                    else {
                        response.status(404);

                        //TODO Error message
                        return "";
                    }
                },
                new JsonTransformer());

        get("/accounts/:id/transfers",
                (request, response) -> accountsService.getAccountTransfers(Long.parseLong(request.params(":id"))),
                new JsonTransformer());

        post("/accounts", (request, response) -> {
            Gson gson = new Gson();
            Account acc = gson.fromJson(request.body(), Account.class);

            acc = accountsService.createAccount(acc);

            response.status(201);

            return acc;

        }, new JsonTransformer());

        get("/transfers",
                (request, response) -> transfersService.getAllTransfers(),
                new JsonTransformer());

        get("/transfers/:id",
                (request, response) -> {
                    Optional<Transfer> transfer = transfersService.getTransfer(Long.parseLong(request.params(":id")));

                    if (transfer.isPresent())
                        return transfer.get();
                    else {
                        response.status(404);

                        //TODO Error message
                        return "";
                    }
                },
                new JsonTransformer());

        post("/transfers",
                (request, response) -> {
                    Gson gson = new Gson();

                    //TODO Validate balance > 0
                    //TODO Validate fromAcc != toAcc

                    TransferRequest transferRequest = gson.fromJson(request.body(), TransferRequest.class);

                    Transfer transfer = transfersService.transferAmount(transferRequest);

                    response.status(201);

                    return transfer;
                },
                new JsonTransformer());

        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
            response.type("application/json");
        });
    }

    public static void main(String[] args) {
        RestServer server = new RestServer();

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

