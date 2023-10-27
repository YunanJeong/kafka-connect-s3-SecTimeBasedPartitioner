package io.github.yunanjeong.custom;

import org.apache.kafka.connect.sink.SinkRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.connect.storage.errors.PartitionException;
import io.confluent.connect.storage.partitioner.TimeBasedPartitioner;

public class SecTimeBasedPartitioner<T> extends TimeBasedPartitioner<T> {
    
    private static final Logger log = LoggerFactory.getLogger(TimeBasedPartitioner.class);

    // 상위 클래스의 init() 메소드에서 초기화가 된다.
    private long partitionDurationMs;
    private DateTimeFormatter formatter;
    
    // 핵심 변경 로직
    @Override
    public String encodePartition(SinkRecord sinkRecord) {
      Long timestamp = timestampExtractor.extract(sinkRecord);
      timestamp = timestamp * 1000L;
      return encodedPartitionForTimestamp(sinkRecord, timestamp);
    }
   
    // 상위 클래스와 동일한 메소드 (private이라 오버라이드 불가)
    private String encodedPartitionForTimestamp(SinkRecord sinkRecord, Long timestamp) {
    if (timestamp == null) {
      String msg = "Unable to determine timestamp using timestamp.extractor "
          + timestampExtractor.getClass().getName()
          + " for record: "
          + sinkRecord;
      log.error(msg);
      throw new PartitionException(msg);
    }
    DateTime bucket = new DateTime(
        getPartition(partitionDurationMs, timestamp, formatter.getZone())
    );
    return bucket.toString(formatter);
  }
}
