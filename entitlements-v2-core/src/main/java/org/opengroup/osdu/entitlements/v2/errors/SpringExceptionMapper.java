package org.opengroup.osdu.entitlements.v2.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ValidationException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class SpringExceptionMapper extends ResponseEntityExceptionHandler {

    @Autowired
    private JaxRsDpsLog log;

    @ExceptionHandler(AppException.class)
    protected ResponseEntity<Object> handleAppException(AppException e) {
        if (e.getOriginalException() instanceof PartitionException) {
            e.getError().setCode(HttpStatus.UNAUTHORIZED.value());
        }

        return this.getErrorResponse(e);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatus status, WebRequest request) {
        List<String> errorList = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        String errorMessage = String.join(", ", errorList);
        return this.getErrorResponse(new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), errorMessage, ex));
    }

    @ExceptionHandler({ValidationException.class, JsonProcessingException.class, UnrecognizedPropertyException.class})
    protected ResponseEntity<Object> handleValidationException(Exception e) {
        return this.getErrorResponse(new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), e.getMessage(), e));
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        return this.getErrorResponse(new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), ex.getMessage(), ex));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(Exception e) {
        if (e instanceof BeanCreationException && ((BeanCreationException) e).getBeanName().equalsIgnoreCase("scopedTarget.getTenantInfo")) {
            return this.getErrorResponse(new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), PartitionHeaderValidationService.INVALID_DP_HEADER_ERROR));
        }
        return this.getErrorResponse(new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "An unknown error has occurred", e));
    }

    private ResponseEntity<Object> getErrorResponse(AppException e) {
        if (e.getCause() instanceof Exception) {
            Exception original = (Exception) e.getCause();
            this.log.error(original.getMessage(), original);
        }

        if (e.getError().getCode() > 499) {
            this.log.error(e.getError().getMessage(), e);
        } else {
            this.log.warning(e.getError().getMessage(), e);
        }

        return new ResponseEntity<>(e.getError(), HttpStatus.resolve(e.getError().getCode()));
    }
}