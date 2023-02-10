/*
 * Copyright 2020-2023 Google LLC
 * Copyright 2020-2023 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opengroup.osdu.entitlements.v2.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
class ListGroupApiResponseModifierAspect {
  private final DpsHeaders dpsHeaders;

  @Around("execution(* org.opengroup.osdu.entitlements.v2.api.ListGroupApi.listGroups(..))")
  public Object modifyListGroupResponseBody(ProceedingJoinPoint joinPoint) throws Throwable {
    Object proceed = joinPoint.proceed();
    String onBehalfOfValue = dpsHeaders.getOnBehalfOf();

    if (Objects.nonNull(onBehalfOfValue)) {
      ListGroupResponseDto newListGroupResponseDto = new ListGroupResponseDto();
      HttpStatus newHttpStatus = HttpStatus.OK;
      if (proceed instanceof ResponseEntity) {
        Object body = ((ResponseEntity) proceed).getBody();
        if (body instanceof ListGroupResponseDto) {
          ListGroupResponseDto oldResponse = (ListGroupResponseDto) body;
          newListGroupResponseDto.setGroups(oldResponse.getGroups());
          newListGroupResponseDto.setMemberEmail(onBehalfOfValue);
          newListGroupResponseDto.setDesId(onBehalfOfValue);
        }
        Object statusCode = ((ResponseEntity) proceed).getStatusCode();
        if (statusCode instanceof HttpStatus) {
          newHttpStatus = (HttpStatus) statusCode;
        }
      }
      ResponseEntity newResponseEntity = new ResponseEntity(newListGroupResponseDto, newHttpStatus);
      return newResponseEntity;
    }
    return joinPoint.proceed();
  }
}
