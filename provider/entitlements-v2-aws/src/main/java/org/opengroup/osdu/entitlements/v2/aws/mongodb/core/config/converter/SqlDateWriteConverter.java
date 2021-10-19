package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.sql.Date;

import static org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.MongoConfig.SQL_TIMESTAMP_REPLACER_NAME;

@WritingConverter
public class SqlDateWriteConverter implements Converter<Date, Document> {

    @Override
    public Document convert(Date date) {
        Document doc = new Document();
        doc.append(SQL_TIMESTAMP_REPLACER_NAME, date.getTime());
        return doc;
    }
}
