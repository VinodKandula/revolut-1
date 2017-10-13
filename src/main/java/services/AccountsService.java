package services;

import db.MemoryDatabase;
import db.tables.records.AccountRecord;
import model.Account;
import model.AccountCreation;
import model.Transfer;
import org.jooq.impl.DSL;

import java.util.List;

import static db.tables.Account.ACCOUNT;
import static db.tables.Transfer.TRANSFER;

public class AccountsService {
    private final MemoryDatabase db;

    public AccountsService(MemoryDatabase db) {
        this.db = db;
    }

    public List<Account> getAllAccounts() {
        return db.ctx().selectFrom(ACCOUNT).fetchInto(Account.class);
    }

    public Account createAccount(AccountCreation acc) {
        return db.ctx().transactionResult(configuration -> {
            AccountRecord accRec = DSL.using(configuration).insertInto(ACCOUNT, ACCOUNT.NUMBER, ACCOUNT.BALANCE)
                    .values(acc.number, acc.balance)
                    .returning(ACCOUNT.ID)
                    .fetchOne();

            return DSL.using(configuration).selectFrom(ACCOUNT)
                    .where(ACCOUNT.ID.eq(accRec.getId()))
                    .fetchSingle().into(Account.class);
        });
    }

    public Account getAccount(long accId) {
        return db.ctx().selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(accId)).fetchSingleInto(Account.class);
    }

    public List<Transfer> getAccountTransfers(long accId) {
        db.tables.Account fromAcc = ACCOUNT.as("fromAcc");
        db.tables.Account toAcc = ACCOUNT.as("toAcc");

        Account acc = getAccount(accId);

        return db.ctx().selectFrom(TRANSFER.join(fromAcc).onKey(TRANSFER.FROM_ACC).join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(fromAcc.ID.eq(accId).or(toAcc.ID.eq(acc.id)))
                .orderBy(TRANSFER.DATE.desc())
                .fetch(record -> {
                    Transfer t = new Transfer();
                    t.fromAcc = new Account();
                    t.toAcc = new Account();

                    t.id = record.get(TRANSFER.ID);
                    t.amount = record.get(TRANSFER.AMOUNT);
                    t.timestamp = record.get(TRANSFER.DATE);

                    t.fromAcc.id = record.get(fromAcc.ID);
                    t.fromAcc.number = record.get(fromAcc.NUMBER);

                    t.toAcc.id = record.get(toAcc.ID);
                    t.toAcc.number = record.get(toAcc.NUMBER);

                    return t;
                });
    }
}
