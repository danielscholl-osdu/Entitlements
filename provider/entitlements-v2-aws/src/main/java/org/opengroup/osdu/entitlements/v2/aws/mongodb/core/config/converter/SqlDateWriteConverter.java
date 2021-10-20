// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
