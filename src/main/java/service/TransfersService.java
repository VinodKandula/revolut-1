package service;

import db.tables.Account;
import db.tables.records.TransferRecord;
import model.Transfer;
import model.TransferRequest;
import org.jooq.DSLContext;

import static db.tables.Account.ACCOUNT;
import static db.tables.Transfer.TRANSFER;

public class TransfersService {
    private final DSLContext ctx;

    public TransfersService(DSLContext ctx) {
        this.ctx = ctx;
    }

    //TODO Add transactions
    public Transfer transferAmount(TransferRequest transferRequest) {
        Account fromAcc = ACCOUNT.as("fromAcc");
        Account toAcc = ACCOUNT.as("toAcc");

        TransferRecord transferRecord = ctx.insertInto(TRANSFER, TRANSFER.FROM_ACC, TRANSFER.TO_ACC, TRANSFER.AMOUNT)
                .values(transferRequest.fromAcc, transferRequest.toAcc, transferRequest.amount)
                .returning(TRANSFER.ID).fetchOne();

        Transfer transfer = ctx.selectFrom(TRANSFER.join(fromAcc).onKey(TRANSFER.FROM_ACC).join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(TRANSFER.ID.eq(transferRecord.getId()))
                .fetchOne(record -> {
                    Transfer t = new Transfer();
                    t.fromAcc = new model.Account();
                    t.toAcc = new model.Account();

                    t.id = record.get(TRANSFER.ID);
                    t.amount = record.get(TRANSFER.AMOUNT);
                    t.timestamp = record.get(TRANSFER.DATE);

                    t.fromAcc.id = record.get(fromAcc.ID);
                    t.fromAcc.name = record.get(fromAcc.NAME);

                    t.toAcc.id = record.get(toAcc.ID);
                    t.toAcc.name = record.get(toAcc.NAME);

                    return t;
                });

        return transfer;
    }
}
