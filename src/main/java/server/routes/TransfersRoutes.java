package server.routes;

import com.google.gson.Gson;
import model.Transfer;
import model.TransferRequest;
import model.Validator;
import service.TransfersService;
import spark.Route;

public class TransfersRoutes {
    private final TransfersService transfersService;

    public TransfersRoutes(TransfersService transfersService) {
        this.transfersService = transfersService;
    }

    public Route getTransfers() {
        return (request, response) -> transfersService.getAllTransfers();
    }

    public Route getTransferById() {
        return (request, response) -> {
            String id = request.params(":id");
            Validator.validateNumber(id);

            long trId = Long.parseLong(id);
            Validator.validateId(trId);

            return transfersService.getTransfer(trId);
        };
    }

    public Route postTransfer() {
        return (request, response) -> {
            Gson gson = new Gson();
            TransferRequest trReq = gson.fromJson(request.body(), TransferRequest.class);
            Validator.validateTransferRequest(trReq);

            Transfer transfer = transfersService.transferAmount(trReq);

            response.status(201);

            return transfer;
        };
    }
}
