# kafka-connect-s3-SecTimeBasedPartitioner

SecTimeBasedPartitioner: Custom Partitioner for S3 Sink Connector.

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
    └── sec-timebasedpartitioner.jar
```
  
### 3. In your S3 sink connector configuration, Write:

```properties
"partitioner.class": "io.github.yunanjeong.custom.SecTimeBasedPartitioner"
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
