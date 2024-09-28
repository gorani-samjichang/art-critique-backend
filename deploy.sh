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


  # 실행된 자바 프로세스의 PID 찾기
  NEW_PID=$(pgrep -f $JAR_NAME)

  if [ -z "$NEW_PID" ]; then
      echo "> 새로운 프로세스가 실행되지 않았어요."
  else
      echo "> 새롭게 실행된 프로세스의 PID: $NEW_PID"
      echo "$(date '+%Y-%m-%d %H:%M:%S') - PID: $NEW_PID" >> nohup/memlog  # PID를 nohup/memlog 파일에 기록
  fi

  # 종료하지 않고 10분마다 메모리 사용량을 기록하는 루프 시작
  (
      while true; do
          if ps -p $NEW_PID > /dev/null; then
              MEM_USAGE=$(ps -p $NEW_PID -o %mem --no-headers)
              echo "$(date '+%Y-%m-%d %H:%M:%S') - PID $NEW_PID - MEM_USAGE: $MEM_USAGE%" >> nohup/memlog
          else
              echo "$(date '+%Y-%m-%d %H:%M:%S') - PID $NEW_PID not found. Exiting..." >> nohup/memlog
              break
          fi
          sleep 600  # 10분마다 기록
      done
  ) &
 # end of the script