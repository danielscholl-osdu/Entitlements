

if [ -n $USE_SELF_SIGNED_SSL_CERT ];
then    
    export SSL_KEY_PASSWORD=$RANDOM$RANDOM$RANDOM;
    export SSL_KEY_STORE_PASSWORD=$SSL_KEY_PASSWORD;
    export SSL_KEY_STORE_DIR=/tmp/certs;
    export SSL_KEY_STORE_NAME=osduonaws.p12;
    export SSL_KEY_STORE_PATH=$SSL_KEY_STORE_DIR/$SSL_KEY_STORE_NAME;
    export SSL_KEY_ALIAS=osduonaws;
    
    ./ssl.sh;
fi

java $JAVA_OPTS -jar /app.jar