package server;

import com.google.gson.Gson;
import model.Account;
import model.ErrorMessage;
import model.Transfer;
import model.TransferRequest;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import service.AccountsService;
import service.TransfersService;
import spark.ResponseTransformer;

import java.math.BigDecimal;
import java.util.Optional;

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
        //TODO Add pagination for accounts
        get("/accounts",
                (request, response) -> accountsService.getAllAccounts(),
                new JsonTransformer());

        get("/accounts/:id",
                (request, response) -> {
                    long accId = Long.parseLong(request.params(":id"));
                    Optional<Account> account = accountsService.getAccount(accId);

                    if (account.isPresent())
                        return account.get();
                    else {
                        response.status(404);

                        return new ErrorMessage("Account with id: " + accId + " is not found");
                    }
                },
                new JsonTransformer());

        //TODO Add pagination for account transfers
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

        //TODO Add pagination for transfers
        get("/transfers",
                (request, response) -> transfersService.getAllTransfers(),
                new JsonTransformer());

        get("/transfers/:id",
                (request, response) -> {
                    long trId = Long.parseLong(request.params(":id"));
                    Optional<Transfer> transfer = transfersService.getTransfer(trId);

                    if (transfer.isPresent())
                        return transfer.get();
                    else {
                        response.status(404);

                        return new ErrorMessage("Transfer with id: " + trId + " is not found");
                    }
                },
                new JsonTransformer());

        post("/transfers",
                (request, response) -> {
                    Gson gson = new Gson();
                    TransferRequest trReq = gson.fromJson(request.body(), TransferRequest.class);

                    if (trReq.amount.compareTo(BigDecimal.ZERO) <= 0) {
                        response.status(422);

                        return new ErrorMessage("Transfer amount: " + trReq.amount + " must be positive");
                    }

                    if (trReq.fromAcc == trReq.toAcc) {
                        response.status(422);

                        return new ErrorMessage("Transfer fromAcc: " + trReq.fromAcc + " toAcc: " + trReq.toAcc +
                                " can not be done between same accounts");
                    }

                    Transfer transfer = transfersService.transferAmount(trReq);

                    response.status(201);

                    return transfer;
                },
                new JsonTransformer());


        after((request, response) -> {
            response.header("Content-Encoding", "gzip");
            response.type("application/json");
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

            System.out.println("A: " + accountsService.getAccount(1).get().balance);
            System.out.println("B: " + accountsService.getAccount(2).get().balance);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }

    public static void main(String[] args) {
        RestServer server = new RestServer();

        server.start();

        //server.testTx();
    }

    private static class JsonTransformer implements ResponseTransformer {
        private final Gson gson = new Gson();

        @Override
        public String render(Object model) {
            return gson.toJson(model);
        }
    }
}

