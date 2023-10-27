package io.github.yunanjeong.custom;



import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import io.confluent.connect.storage.common.SchemaGenerator;
import io.confluent.connect.storage.common.StorageCommonConfig;
import io.confluent.connect.storage.common.util.StringUtils;
import io.confluent.connect.storage.errors.PartitionException;
import io.confluent.connect.storage.util.DataUtils;
import io.confluent.connect.storage.partitioner.PartitionerConfig;
import io.confluent.connect.storage.partitioner.TimeBasedPartitioner;
import io.confluent.connect.storage.partitioner.TimestampExtractor;

// public: 상속가능, 외부에서 호출 가능
// protected: 상속가능, 외부에서 호출 불가
// private: 상속불가, 외부에서 호출 불가

// static: 런타임을 따르지않고, 컴파일타임에 이미 정해진 고정값을 사용한다는 의미.
//  상속은 됨. 단 이 때는 Override가 아니라 Hiding 이라고 표현

public class SecNumberTimeBasedPartitioner<T> extends TimeBasedPartitioner<T> {


    private static final Logger log = LoggerFactory.getLogger(TimeBasedPartitioner.class);
    private static final Pattern NUMERIC_TIMESTAMP_PATTERN = Pattern.compile("^-?[0-9]{1,19}$");

    
    @Override
    public TimestampExtractor newTimestampExtractor(String extractorClassName) {
        try {
            switch (extractorClassName) {
                case "Wallclock":
                case "Record":
                case "RecordField":
                    extractorClassName = "io.confluent.connect.storage.partitioner.TimeBasedPartitioner$"
                        + extractorClassName
                        + "TimestampExtractor";
                    break;
                case "RecordFieldSecNumber":
                    extractorClassName = "io.github.yunanjeong.custom.SecNumberTimeBasedPartitioner$"
                        + extractorClassName
                        + "TimestampExtractor";
                    break;
                default:
            }
            Class<?> klass = Class.forName(extractorClassName);
            if (!TimestampExtractor.class.isAssignableFrom(klass)) {
                throw new ConnectException(
                    "Class " + extractorClassName + " does not implement TimestampExtractor"
                );
            }
            return (TimestampExtractor) klass.newInstance();
        } catch (ClassNotFoundException
                | ClassCastException
                | IllegalAccessException
                | InstantiationException e) {
            ConfigException ce = new ConfigException(
                "Invalid timestamp extractor: " + extractorClassName
            );
            ce.initCause(e);
            throw ce;
        }
    }

    public static class RecordFieldSecNumberTimestampExtractor implements TimestampExtractor {
        private String fieldName;
        private DateTimeFormatter dateTime;

        @Override
        public void configure(Map<String, Object> config) {
            fieldName = (String) config.get(PartitionerConfig.TIMESTAMP_FIELD_NAME_CONFIG);
            dateTime = ISODateTimeFormat.dateTimeParser();
        }

        @Override
        public Long extract(ConnectRecord<?> record) {
            Object value = record.value();
            if (value instanceof Struct) {
                Struct struct = (Struct) value;
                Object timestampValue = DataUtils.getNestedFieldValue(struct, fieldName);
                Schema fieldSchema = DataUtils.getNestedField(record.valueSchema(), fieldName).schema();
                    
                if (Timestamp.LOGICAL_NAME.equals(fieldSchema.name())) {
                    return ((Date) timestampValue).getTime(); //
                }

                switch (fieldSchema.type()) {
                case INT32:
                case INT64:
                    return ((Number) timestampValue).longValue() * 1000 ; // 뒤에 000붙여준다.
                case STRING:
                    return extractTimestampFromString((String) timestampValue);
                default:
                    log.error(
                        "Unsupported type '{}' for user-defined timestamp field.",
                        fieldSchema.type().getName()
                    );
                    throw new PartitionException(
                        "Error extracting timestamp from record field: " + fieldName
                    );
                }
            } else if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Object timestampValue = DataUtils.getNestedFieldValue(map, fieldName);
                if (timestampValue instanceof Number) {
                    return ((Number) timestampValue).longValue() * 1000 ; // 뒤에 000붙여준다.
                } else if (timestampValue instanceof String) {
                    return extractTimestampFromString((String) timestampValue);
                } else if (timestampValue instanceof Date) {
                    return ((Date) timestampValue).getTime(); //
                } else {
                    log.error(
                        "Unsupported type '{}' for user-defined timestamp field.",
                        timestampValue.getClass()
                    );
                    throw new PartitionException(
                        "Error extracting timestamp from record field: " + fieldName
                    );
                }
            } else {
                log.error("Value is not of Struct or Map type.");
                throw new PartitionException("Error encoding partition.");
            }
        }

        private Long extractTimestampFromString(String timestampValue) {
            if (NUMERIC_TIMESTAMP_PATTERN.matcher(timestampValue).matches()) {
                try {
                    return Long.valueOf(timestampValue);
                } catch (NumberFormatException e) {
                    // expected, ignore
                }
            }
            return dateTime.parseMillis(timestampValue);
        }
    }

}
