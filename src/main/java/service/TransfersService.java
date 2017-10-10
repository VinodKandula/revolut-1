package service;

import db.tables.Account;
import db.tables.records.TransferRecord;
import model.Transfer;
import model.TransferRequest;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.impl.DSL;

import java.util.List;

import static db.tables.Account.ACCOUNT;
import static db.tables.Transfer.TRANSFER;

public class TransfersService {
    private final DSLContext ctx;

    private final Account fromAcc = ACCOUNT.as("fromAcc");
    private final Account toAcc = ACCOUNT.as("toAcc");

    public TransfersService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<Transfer> getAllTransfers() {
        return ctx.selectFrom(TRANSFER
                .join(fromAcc).onKey(TRANSFER.FROM_ACC)
                .join(toAcc).onKey(TRANSFER.TO_ACC))
                .orderBy(TRANSFER.DATE.desc())
                .fetch(new TransferRecordMapper());
    }

    public Transfer getTransfer(long transferId) {
        return ctx.selectFrom(TRANSFER
                .join(fromAcc).onKey(TRANSFER.FROM_ACC)
                .join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(TRANSFER.ID.eq(transferId))
                .fetchSingle(new TransferRecordMapper());
    }

    //TODO Return Optional
    public Transfer transferAmount(TransferRequest trReq) {
        long trId = ctx.transactionResult(configuration -> {
            DSL.using(configuration)
                    .selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(trReq.fromAcc).or(ACCOUNT.ID.eq(trReq.toAcc)))
                    .forUpdate().fetchInto(model.Account.class);

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.fromAcc))
                    .execute();

            DSL.using(configuration)
                    .update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.plus(trReq.amount))
                    .where(ACCOUNT.ID.eq(trReq.toAcc))
                    .execute();

            TransferRecord transferRecord = DSL.using(configuration)
                    .insertInto(TRANSFER, TRANSFER.FROM_ACC, TRANSFER.TO_ACC, TRANSFER.AMOUNT)
                    .values(trReq.fromAcc, trReq.toAcc, trReq.amount)
                    .returning(TRANSFER.ID).fetchOne();

            return transferRecord.getId();
        });

        Transfer transfer = ctx.selectFrom(TRANSFER.join(fromAcc).onKey(TRANSFER.FROM_ACC).join(toAcc).onKey(TRANSFER.TO_ACC))
                .where(TRANSFER.ID.eq(trId))
                .fetchOne(new TransferRecordMapper());

        return transfer;
    }

    private class TransferRecordMapper implements RecordMapper<Record, Transfer> {
        @Override
        public Transfer map(Record record) {
            Transfer t = new Transfer();
            t.fromAcc = new model.Account();
            t.toAcc = new model.Account();

            t.id = record.get(TRANSFER.ID);
            t.amount = record.get(TRANSFER.AMOUNT);
            t.timestamp = record.get(TRANSFER.DATE);

            t.fromAcc.id = record.get(fromAcc.ID);
            t.fromAcc.number = record.get(fromAcc.NUMBER);

            t.toAcc.id = record.get(toAcc.ID);
            t.toAcc.number = record.get(toAcc.NUMBER);

            return t;
        }
    }
}
