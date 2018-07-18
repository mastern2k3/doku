
set PATH=%path%;C:\Users\Nitzanz\Downloads\sbt-1.1.6\sbt\bin
set JDK_HOME=C:\Program Files\Java\jdk1.8.0_151

cd %~dp0

start sbt "~backend/reStart"

cd ui

start npm run dev
