package service;

import model.Account;
import model.Transfer;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static db.tables.Account.ACCOUNT;
import static db.tables.Transfer.TRANSFER;

public class AccountsService {
    private final DSLContext ctx;

    public AccountsService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<Account> getAllAccounts() {
        return ctx.selectFrom(ACCOUNT).fetchInto(Account.class);
    }

    public Account createAccount(Account acc) {
        Account createdAcc = ctx.insertInto(ACCOUNT, ACCOUNT.NAME, ACCOUNT.BALANCE)
                .values(acc.name, acc.balance)
                .returning(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.BALANCE)
                .fetchOne().into(Account.class);

        return createdAcc;
    }

    public Optional<Account> getAccount(long accId) {
        return ctx.selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(accId)).fetchOptionalInto(Account.class);
    }

    public List<Transfer> getAccountTransfers(long accId) {
        db.tables.Account fromAcc = ACCOUNT.as("fromAcc");
        db.tables.Account toAcc = ACCOUNT.as("toAcc");

        return ctx.selectFrom(TRANSFER.join(fromAcc).onKey(TRANSFER.FROM_ACC).join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(ACCOUNT.ID.eq(accId))
                .fetch(record -> {
                    Transfer t = new Transfer();
                    t.fromAcc = new Account();
                    t.toAcc = new Account();

                    t.id = record.get(TRANSFER.ID);
                    t.amount = record.get(TRANSFER.AMOUNT);
                    t.timestamp = record.get(TRANSFER.DATE);

                    t.fromAcc.id = record.get(fromAcc.ID);
                    t.fromAcc.id = record.get(toAcc.ID);

                    return t;
                });
    }
}