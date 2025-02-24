package org.opengroup.osdu.entitlements.v2.validation;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartitionHeaderValidationService {

    public static final String INVALID_DP_HEADER_ERROR = "Invalid data partition header provided";
    public static final String INVALID_ARGUMENT_ERROR = "Illegal or inappropriate argument found";

    @Autowired
    private JaxRsDpsLog log;

    public void validateSinglePartitionProvided(final String partitionHeader) {
        if (partitionHeader.split(",").length > 1) {
            log.error(String.format("Single data partition expected in header, found: %s", partitionHeader));
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), INVALID_DP_HEADER_ERROR);
        }
    }

    /**
     * For historical reason, list Group API should support single partition as well as a special multi partition case (primary,common)
     */
    public void validateIfSpecialListGroupPartitionIsProvided(final List<String> partitionIds) {
        if (partitionIds.size() > 2) {
            log.error(String.format("Multiple data partition ids provided: %s", partitionIds));
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), INVALID_DP_HEADER_ERROR);
        }
        if (partitionIds.size() == 2 && !partitionIds.contains("common")) {
            log.error(String.format("common data partition is expected, when two provided, actual: %s", partitionIds));
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), INVALID_DP_HEADER_ERROR);
        }
    }
}
