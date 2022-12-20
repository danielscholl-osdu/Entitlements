FROM google/cloud-sdk:alpine

WORKDIR /opt

RUN apk update && apk add --no-cache jq

COPY ./provider/entitlements-v2-jdbc/bootstrap/ /opt/

RUN chmod +x bootstrap.sh validate-env.sh

CMD ["/bin/bash", "-c", "/opt/bootstrap.sh"]
