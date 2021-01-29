package org.opengroup.osdu.entitlements.v2.util;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.JwtClaimExtractor;
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
    private JwtClaimExtractor jwtClaimExtractor;


    public String getAppId(final DpsHeaders dpsHeaders) {
        return jwtClaimExtractor.extract(dpsHeaders.getAuthorization()).getAppId();
    }

    public String getUserId(final DpsHeaders dpsHeaders) {
        return jwtClaimExtractor.extract(dpsHeaders.getAuthorization()).getUserId();
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
