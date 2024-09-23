#!/bin/bash
 echo "> 앱이 실행중인 프로세스를 찾아요!"
 CURRENT_PID=$(pgrep -f art_critique) #실행중인 파일명으로
 echo "$CURRENT_PID"
 if [ -z $CURRENT_PID ]; then
         echo "> 실행중인 프로세스가 없어요."
 else
         echo "> kill -9 $CURRENT_PID"
         kill -9 $CURRENT_PID
         sleep 3
 fi
 echo "> 새로운 앱을 배포할게요"

 cd /home/ubuntu/deploy
 JAR_NAME=$(ls | grep -v '\-plain' | grep '.jar$' | tail -n 1) #jar파일의 이름을 정규표현식같은거로 찾는거 여기서는 -plain이 안들어있는 .jar파일을 찾느다
 echo "> JAR Name: $JAR_NAME"

 # nohup java -jar -Duser.timezone=Asia/Seoul $JAR_NAME &

 DIRECTORY="nohup"

 # 디렉터리가 존재하지 않으면 생성
 if [ ! -d "$DIRECTORY" ]; then
     mkdir "$DIRECTORY"
 fi
 nohup java -jar -Duser.timezone=Asia/Seoul $JAR_NAME 1>nohup/stdout.txt 2>nohup/stderr.txt &
 sleep 2


 # end of the script