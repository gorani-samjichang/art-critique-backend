## 로컬 포팅 메뉴얼 입니다.

1. 루트 폴더에서 src\main\resources\application.yml 파일을 생성하여 다음과 같이 작성해 주세요
```
spring.application.name: art_critique
server:
  servlet:
    context-path: /api
```

2. IDE에서 실행버튼을 누르거나 루트 폴더에서
```
./gradlew.bat bootRun
```
를 실행하여 실행해 주세요
