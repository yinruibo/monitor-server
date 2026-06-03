@echo off
:: 在命令行窗口中显示输出信息 为 UTF-8格式
chcp 65001 >nul
cd /d D:\monitor

echo 正在启动 windows硬件监控 程序...
:: 标注 nacos中获取的 yml 文件为 UTF-8 格式
java -Dfile.encoding=utf-8 -jar monitor-server-0.0.1-SNAPSHOT.jar

pause