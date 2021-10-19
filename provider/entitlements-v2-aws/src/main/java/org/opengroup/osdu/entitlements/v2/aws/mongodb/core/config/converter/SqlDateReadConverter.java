package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.sql.Date;

import static org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.MongoConfig.SQL_TIMESTAMP_REPLACER_NAME;


@ReadingConverter
public class SqlDateReadConverter implements Converter<Document, Date> {

    @Override
    public Date convert(Document date) {
        return new Date(date.getLong(SQL_TIMESTAMP_REPLACER_NAME));
    }
}
