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

public class RestServer {
    private static final String DB_URL = "jdbc:h2:mem:transfers;" +
            "INIT=RUNSCRIPT FROM 'classpath:/h2/schema.sql'\\;RUNSCRIPT FROM 'classpath:/h2/data.sql';";

    private final AccountsService accountsService;
    private final TransfersService transfersService;

    public RestServer() {
        JdbcConnectionPool pool = JdbcConnectionPool.create(DB_URL, "sa", "");
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

        post("/transfers",
                (request, response) -> {
                    Gson gson = new Gson();

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

