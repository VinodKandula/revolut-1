package server.handlers;

import model.ErrorMessage;
import model.Validator;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.json.JsonTransformer;
import spark.ExceptionHandler;

public class ErrorsHandler {
    private final static Logger logger = LoggerFactory.getLogger(ErrorsHandler.class);

    private final JsonTransformer json = new JsonTransformer();

    public ExceptionHandler exceptionsHandler() {
        return (e, request, response) -> {
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

            logger.error("Handled server exception", e);

            response.body(json.render(error));
        };
    }
}
