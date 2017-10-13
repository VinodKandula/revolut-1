package server.routes;

import com.google.gson.Gson;
import model.Account;
import model.AccountCreation;
import model.Validator;
import services.AccountsService;
import spark.Route;

public class AccountsRoutes {
    private final AccountsService accountsService;

    public AccountsRoutes(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    public Route getAccountsRoute() {
        return (request, response) -> accountsService.getAllAccounts();
    }

    public Route getAccountById() {
        return (request, response) -> {
            String id = request.params(":id");
            Validator.validateNumber(id);

            long accId = Long.parseLong(id);
            Validator.validateId(accId);

            return accountsService.getAccount(accId);
        };
    }

    public Route getAccountTransfersById() {
        return (request, response) ->  {
            String id = request.params(":id");
            Validator.validateNumber(id);

            long accId = Long.parseLong(id);
            Validator.validateId(accId);

            return accountsService.getAccountTransfers(accId);
        };
    }

    public Route postAccount() {
        return (request, response) -> {
            Gson gson = new Gson();
            AccountCreation acc = gson.fromJson(request.body(), AccountCreation.class);
            Validator.validateAccountCreation(acc);

            Account account = accountsService.createAccount(acc);

            response.status(201);

            return account;

        };
    }
}
