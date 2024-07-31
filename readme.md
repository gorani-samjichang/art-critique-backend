## 로컬 포팅 메뉴얼 입니다.

1. 해당 파일을 src\main\resources\keystore 디렉터리에 넣어 주세요<br>
https://drive.google.com/open?id=1Tb7JuHGzdUiCCFJLKPCB9L5twLPFsE0U&usp=drive_fs
<br><br>
2. 빌드 완료한 application.yml을 확인하려면 다음 파일을 확인해 주세요<br>
https://drive.google.com/open?id=1TiF4Q48JLtH2OpWXpe8wdO1B7yVQlX5H&usp=drive_fs
<br><br>
3. 루트 폴더에서 src\main\resources\application.yml 파일을 생성하여 다음과 같이 작성해 주세요
```
spring.application.name: art_critique
server:
  port: 9200
  servlet:
    context-path: /api

front.server.host: http://localhost:9100 #7-26 추가됨
feedback.server.host: http://localhost:9300/api/feedback # 7-18추가됨
firebaseSdkPath: keystore/art-cri**********************b9.json # (1에서 받은 파일명)
firebaseBucket: art-cri*****.appspot.com #(저희 프로젝트 명으로 수정하세요)
jwt.secret: d0a********************************************************439
token:
  verify:
    prefix: oauth@
    google: google@
    x: x@

twitter:
  consumer:
    key: qeV**************iBr6
    secret: S7z*******************************************t8TN
```
4. 자신의 로컬 환경에 맞추어 src\main\resources\application.yml에 내용을 더 추가해 주세요
```
spring:
  datasource:
    url: jdbc:postgresql://ep-orange**************oyeb.app/artcritique
    username: o***e
    password: x4U*********GVo
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    
  mail:
    host: smtp.gmail.com
    port: 587
    username: *****@gmail.com
    password: abcdefgh********
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
    auth-code-expiration-millis: 1800000
```

5. IDE에서 실행버튼을 누르세요
---
## SQLite application.yml
```yml
spring.datasource.url: jdbc:sqlite:mydb.db
spring.datasource.driver-class-name: org.sqlite.JDBC
```