package org.opengroup.osdu.entitlements.v2.util;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RequestInfoUtilService {

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private JaxRsDpsLog log;


    public String getAppId(final DpsHeaders dpsHeaders) {
        String appId = dpsHeaders.getAppId();
        log.debug(String.format("Getting App Id from Headers: %s", appId));
        return appId;
    }

    public String getUserId(final DpsHeaders dpsHeaders) {
        String userId = dpsHeaders.getUserId();
        log.debug(String.format("Getting User Id from Headers: %s", userId));
        return userId;
    }

    public String getImpersonationTarget(final DpsHeaders dpsHeaders){
        String onBehalfOf = dpsHeaders.getOnBehalfOf();
        log.debug("Getting on behalf User Id from Headers: {}", onBehalfOf);
        return onBehalfOf;
    }

    public String getDomain(final String partitionId) {
        return String.format("%s.%s", partitionId, appProperties.getDomain());
    }

    public List<String> getPartitionIdList(final DpsHeaders dpsHeaders) {
        final String partitionIdHeader = dpsHeaders.getPartitionId();
        if (StringUtils.isBlank(partitionIdHeader)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Invalid data partition header provided");
        }
        return Arrays.asList(partitionIdHeader.trim().split("\\s*,\\s*"));
    }
}
