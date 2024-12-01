FROM google/cloud-sdk:alpine

WORKDIR /opt

RUN apk update && apk add --no-cache jq

COPY ./provider/entitlements-v2-core-plus/bootstrap/ /opt/

RUN chmod +x bootstrap.sh validate-env.sh

RUN addgroup -g 10001 -S nonroot \
  && adduser -h /opt -G nonroot -S -u 10001 nonroot

RUN chown -R 10001:10001 /opt

USER 10001:10001

CMD ["/bin/bash", "-c", "/opt/bootstrap.sh"]
