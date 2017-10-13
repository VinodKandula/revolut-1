package model;

import java.math.BigDecimal;

public class Validator {
    public static void validateTransferRequest(TransferRequest trReq) {
        if (trReq.amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidationException("Transfer amount: " + trReq.amount + " must be positive");

        if (trReq.fromAcc == trReq.toAcc)
            throw new ValidationException("Sender's account id: " + trReq.fromAcc + " can not be equal to " +
                    "recipient's account id: " + trReq.toAcc);
    }

    public static void validateAccountCreation(AccountCreation acc) {
        if (acc.number == null || acc.number.length() <= 0)
            throw new ValidationException("Account name can not be empty");

        if (acc.balance == null || acc.balance.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("Account balance can not be negative");
    }

    public static void validateId(long id) {
        if (id <= 0)
            throw new ValidationException("Reference id: " + id + " must be positive");
    }

    public static void validateNumber(String number) {
        try {
            Long.parseLong(number);
        } catch (NumberFormatException e) {
            throw new ValidationException("String: " + number + " is not a number");
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
