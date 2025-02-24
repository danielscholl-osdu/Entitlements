package org.opengroup.osdu.entitlements.v2.azure.error;

import org.apache.tinkerpop.gremlin.driver.exception.NoHostAvailableException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class AzureExceptionHandler  extends ResponseEntityExceptionHandler {

    @Autowired
    private JaxRsDpsLog jaxRsDpsLog;

    @ExceptionHandler(value = {NoHostAvailableException.class})
    public ResponseEntity<AppError> handleNoHostAvailableException(Exception e){
        AppError appError = AppError.builder()
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("No available upstream host, please contact support team")
                .build();
        jaxRsDpsLog.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE.value()).body(appError);
    }

}
