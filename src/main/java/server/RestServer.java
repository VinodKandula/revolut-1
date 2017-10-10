package server;

import com.google.gson.Gson;
import model.*;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;
import service.AccountsService;
import service.TransfersService;
import spark.ResponseTransformer;

import java.math.BigDecimal;

import static spark.Spark.*;

public class RestServer {
    //TODO Use in-memory database when development is finished
    private static final String MEM_DB_URL = "jdbc:h2:mem:transfers;MULTI_THREADED=1;";
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

        afterAfter((request, response) -> {
            response.header("Content-Encoding", "gzip");
            response.type("application/json");
        });

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

            JsonTransformer json = new JsonTransformer();
            response.body(json.render(error));
        });
    }

    //TODO Move to tests
    private void testTx() {
        Thread t1 = new Thread(() -> {
            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 1;
            tr.toAcc = 2;
            tr.amount = BigDecimal.valueOf(100);

            System.out.println(Thread.currentThread() + " start " + tr.amount);
            Transfer transfer = transfersService.transferAmount(tr);
            System.out.println(Thread.currentThread() + " finished " + tr.amount);
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.currentThread().sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            TransferRequest tr = new TransferRequest();

            tr.fromAcc = 20;
            tr.toAcc = 1;
            tr.amount = BigDecimal.valueOf(200);

            System.out.println(Thread.currentThread() + " start " + tr.amount);
            Transfer transfer = transfersService.transferAmount(tr);
            System.out.println(Thread.currentThread() + " finished " + tr.amount);
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();

            System.out.println("A: " + accountsService.getAccount(1).balance);
            System.out.println("B: " + accountsService.getAccount(2).balance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

