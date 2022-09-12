FROM google/cloud-sdk:slim

WORKDIR /opt

RUN apt-get update && apt-get install -yq jq

COPY ./provider/entitlements-v2-jdbc/bootstrap/ /opt/

RUN chmod +x bootstrap.sh validate-env.sh

CMD ["/bin/bash", "-c", "sleep 1m & /opt/bootstrap.sh && sleep 365d"]
