# kafka-connect-s3-SecNumberTimeBasedPartitioner

SecNumberTimeBasedPartitioner: Custom Partitioner for S3 Sink Connector.

## Motivation: Unix timestamps in seconds or milliseconds?

- TimeBasedPartitioner doesn't support Unix timestamps `in seconds`.
- My raw data time means Unix timestamps in seconds.
- I want to keep the time value unchanged and upload it to s3.
- Use this partitioner if you don't want to modify previous pipeline steps, such as rawdata, kafka-client, source connector's smt, kstreams, etc.

```sh
#Event Time in Raw Data
{"time": 1234567890}

# TimeBasedPartitioner # 1234567s and 890ms
s3://mybucket/year=1970/month=01/day=15/... 

# SecNumberTimeBasedPartitioner # 1234567890s and 000ms
s3://mybucket/year=2019/month=02/day=14/...  
```

- SecNumberTimeBasedPartitioner supports `Number` type only.

```sh
# Supported
{"time": 1234567890}

# Unsupported
{"time": "1234567890"}
```


## How to use

### 1. Install [S3 Sink Connector](https://www.confluent.io/hub/confluentinc/kafka-connect-s3)

### 2. Add []() into the S3 sink connector directory in your connect plugin-path

- This jar file doesn't contain the S3 sink connector, only the partitioner.

```tree
e.g.
.(connect plugin-path)
└── confluentinc-kafka-connect-s3-10.5.0
    ├── assets
    ├── doc
    ├── etc
    ├── lib
    ├── manifest.json
    └── secnumber-timebasedpartitioner.jar
```
  
### 3. In your S3 sink connector configuration, Write:

```properties
"timestamp.extractor": "RecordFieldSecNumber"
"partitioner.class": "io.github.yunanjeong.custom.SecNumberTimeBasedPartitioner"
```

## How to build sources (Maven)

### intelliJ 기준, jar 파일 (Artifact) 빌드 방법

- `File-Project Structure` 진입
- `Project Settings-Artifacts-Add(+)-Jar`에서 `Empty` 선택
  - `From modules with dependencies`를 선택하는 것이 일반적이지만, 이미 원본 S3 sink connector에 중복되는 dependency가 모두 있으므로 상관없음
- `Name`에 Jar파일 이름을 적고, `Output Layout-Available Element`에서 'sec-timebasedpartitioner' compile output만 jar파일 포함대상으로 선택
- 이후 `intellij 메인 창-Build-Build Artifacts ...` 선택하여 빌드를 진행한다.
- `{project root path}/out/` 에서 생성된 jar파일을 확인

### ubuntu cli 기준 빌드 방법

  ```sh
  # maven 설치
  sudo apt install mvn
  
  # 빌드
  mvn install -f pom.xml -Dcheckstyle.skip -DskipTests
  
  #{project root path}/target/에서 생성된 jar 파일 확인
  ```
