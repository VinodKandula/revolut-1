package model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorTest {
    @Test
    void testValidateTransferRequest_WhenCorrectTranferRequest_NothingIsThrown() {
        TransferRequest trReq = new TransferRequest();
        trReq.amount = BigDecimal.valueOf(400);
        trReq.fromAcc = 2;
        trReq.toAcc = 3;

        Validator.validateTransferRequest(trReq);
    }

    @Test
    void testValidateTransferRequest_WhenNullTranferRequest_ThrowIllegalStateEx() {
        assertThrows(IllegalStateException.class, () -> Validator.validateTransferRequest(null));
    }

    @Test
    void testValidateTransferRequest_WhenNegativeAmount_ThrowValidationEx() {
        TransferRequest trReq = new TransferRequest();
        trReq.amount = BigDecimal.valueOf(-400);
        trReq.fromAcc = 2;
        trReq.toAcc = 3;

        assertThrows(Validator.ValidationException.class, () -> Validator.validateTransferRequest(trReq));
    }

    @Test
    void testValidateTransferRequest_WhenEqualAccIds_ThrowValidationEx() {
        TransferRequest trReq = new TransferRequest();
        trReq.amount = BigDecimal.valueOf(400);
        trReq.fromAcc = 2;
        trReq.toAcc = 2;

        assertThrows(Validator.ValidationException.class, () -> Validator.validateTransferRequest(trReq));
    }

    @Test
    void testValidateTransferRequest_WhenCorrectAccountCreation_NothingIsThrown() {
        AccountCreation ac = new AccountCreation();
        ac.number = "acc";
        ac.balance = BigDecimal.valueOf(100);

        Validator.validateAccountCreation(ac);
    }

    @Test
    void testValidateTransferRequest_WhenNullAccountCreation_ThrowIllegalStateEx() {
        assertThrows(IllegalStateException.class, () -> Validator.validateAccountCreation(null));
    }

    @Test
    void validateAccountCreation_WhenNullAccNumber_ThrowValidationEx() {
        AccountCreation ac = new AccountCreation();
        ac.balance = BigDecimal.valueOf(100);

        assertThrows(Validator.ValidationException.class, () -> Validator.validateAccountCreation(ac));
    }

    @Test
    void validateAccountCreation_WhenEmptyAccNumber_ThrowValidationEx() {
        AccountCreation ac = new AccountCreation();
        ac.number = "";
        ac.balance = BigDecimal.valueOf(100);

        assertThrows(Validator.ValidationException.class, () -> Validator.validateAccountCreation(ac));
    }

    @Test
    void validateAccountCreation_WhenNegativeBalance_ThrowValidationEx() {
        AccountCreation ac = new AccountCreation();
        ac.number = "acc7";
        ac.balance = BigDecimal.valueOf(-90);

        assertThrows(Validator.ValidationException.class, () -> Validator.validateAccountCreation(ac));
    }

    @Test
    void validateId_WhenPositiveId_NothingIsThrown() {
        Validator.validateId(100);
    }

    @Test
    void validateId_WhenNegativeId_ThrowValidationEx() {
        assertThrows(Validator.ValidationException.class, () -> Validator.validateId(-100));
    }

    @Test
    void validateId_WhenZeroId_ThrowValidationEx() {
        assertThrows(Validator.ValidationException.class, () -> Validator.validateId(0));
    }

    @Test
    void validateId_WhenNumberAsString_NothingIsThrown() {
        Validator.validateNumber("890");
    }

    @Test
    void validateNumber_WhenUnparsableNumber_ThrowValidationEx() {
        assertThrows(Validator.ValidationException.class, () -> Validator.validateNumber("123xyz"));
    }
}