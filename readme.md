## 로컬 포팅 메뉴얼 입니다.

1. 해당 파일을 src\main\resources\keystore 디렉터리에 넣어 주세요<br>
https://drive.google.com/open?id=1Tb7JuHGzdUiCCFJLKPCB9L5twLPFsE0U&usp=drive_fs
<br><br>

2. 루트 폴더에서 src\main\resources\application.yml 파일을 생성하여 다음과 같이 작성해 주세요
```
spring.application.name: art_critique
server:
  servlet:
    context-path: /api

firebaseSdkPath: keystore/art-cri**********************b9.json # (1에서 받은 파일명)
firebaseBucket: art-cri*****.appspot.com #(저희 프로젝트 명으로 수정하세요)
```

3. 자신의 로컬 환경에 맞추어 src\main\resources\application.yml에 내용을 더 추가해 주세요
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
```

4. IDE에서 실행버튼을 누르거나 루트 폴더에서
```
./gradlew.bat bootRun
```
를 실행하여 실행해 주세요
