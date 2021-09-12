#!/bin/sh
#
logger -p daemon.info -t LightSwitch "Start script"

#JAVA_HOME="/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt"
scriptFile=$(readlink -fn $0)                              # the absolute, dereferenced path of this script file
scriptDir=$(dirname $scriptFile)                           # absolute path of the script directory
applDir="$scriptDir"                                       # home directory of the service application
serviceName="LightSwitch"                                  # service name
serviceNameLo="lightswitch"                                # service name with the first letter in lowercase
serviceUser="root"                                         # OS user name for the service
serviceUserHome="$applDir"                                 # home directory of the service user
serviceGroup="Light"                                        # OS group name for the service
serviceLogFile="$applDir/$serviceNameLo.log"               # log file for StdOut/StdErr
maxShutdownTime=15                                         # maximum number of seconds to wait for the daemon to terminate normally
pidFile="/var/run/$serviceNameLo.pid"                      # name of PID file (PID = process ID number)
javaCommand="java"
                                                           # name of the Java launcher without the path
#javaExe="$JAVA_HOME/bin/$javaCommand"                      # file name of the Java application launcher executable
#javaArgs=" -cp /opt/pi4j/lib/'*':LightSwitch.jar jb.light.switch_.SwitchLight"                                                # arguments for Java launcher
javaArgs=" -jar LightSwitch.jar"                           # arguments for Java launcher
javaCommandLine="$javaCommand $javaArgs"                       # command line to start the Java service application
#javaCommandLine="$javaExe $javaArgs"                       # command line to start the Java service application
javaCommandLineKeyword="LightSwitch"                       # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others
rcFileBaseName="rc$serviceNameLo"                          # basename of the "rc" symlink file for this script
#rcFileName="/usr/local/sbin/$rcFileBaseName"               # full path of the "rc" symlink file for this script
#etcInitDFile="/etc/init.d/$serviceNameLo"                  # symlink to this script from /etc/init.d
# echo "JAVA_HOME: $JAVA_HOME"
# echo "scriptFile: $scriptFile"
# echo "scriptDir: $scriptDir"
# echo "applDir: $applDir"
# echo "serviceName: $serviceName"
# echo "serviceNameLo: $serviceNameLo"
# echo "serviceUser: $serviceUser"
# echo "serviceUserHome: $serviceUserHome"
# echo "serviceGroup: $serviceGroup"
# echo "serviceLogFile: $serviceLogFile"
# echo "maxShutdownTime: $maxShutdownTime"
# echo "pidFile: $pidFile"
# echo "javaCommand: $javaCommand"
# echo "javaExe: $javaExe"
# echo "javaArgs: $javaArgs"
# echo "javaCommandLine: $javaCommandLine"
# echo "javaCommandLineKeyword: $javaCommandLineKeyword"
# echo "rcFileBaseName: $rcFileBaseName"
# echo "rcFileName: $rcFileName"
# echo "etcInitDFile: $etcInitDFile"

# Makes the file $1 writable by the group $serviceGroup.
makeFileWritable () {
#    echo "start makeFileWritable $1"
   local filename="$1"
   touch $filename || return 1
   chgrp $serviceGroup $filename || return 1
   chmod g+w $filename || return 1
   return 0; }

# Returns 0 if the process with PID $1 is running.
checkProcessIsRunning () {
#   echo "start checkProcessIsRunning $1"
   local pid="$1"
   if [ -z "$pid" ]; then
#     echo "Proces loopt niet"
     return 1
   fi
#   echo "Proces loopt"
   if [ ! -e /proc/$pid ]; then
#     echo "Proces niet in /Proc"
     return 1
   fi
#   echo "Ook in /Proc!"
   return 0; }

# Returns 0 if the process with PID $1 is our Java service process.
checkProcessIsOurService () {
#   echo "start checkProcessIsOurService $1"
   local pid="$1"
   local cmd="$(ps -p $pid --no-headers -o comm)"
   if [ "$cmd" != "$javaCommand" -a "$cmd" != "$javaCommand.bin" ]; then
#     echo "Geen Java!"
     return 1
   fi
   grep -q --binary -F "$javaCommandLineKeyword" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then
#     echo "Onjuist keyword"
     return 1
   fi
#   echo "Proces loopt inderdaad"
   return 0; }

# Returns 0 when the service is running and sets the variable $servicePid to the PID.
getServicePid () {
#   echo "Start getServicePid"
   if [ ! -f $pidFile ]; then
#     echo "PidFile bestaat niet"
     return 1
   fi
#   echo "PidFile bestaat!"
   servicePid=`cat $pidFile`
#   echo "servicePid: $servicePid"
   checkProcessIsRunning $servicePid || return 1
#   echo "Proces loopt nog!"
   checkProcessIsOurService $servicePid || return 1
#   echo "Dit proces loopt nog!"
   return 0; }

startServiceProcess () {
#    echo "begin startServiceProcess"
   cd $applDir || return 1
#    echo "naar applDir gegaan"
   rm -f $pidFile
#    echo "pidFile verwijderd, nu aanmaken"
   makeFileWritable $pidFile || return 1
#    echo "pidFile aangemaakt"
   makeFileWritable $serviceLogFile || return 1
#    echo "logFile aangemaakt"
   local cmd="setsid $javaCommandLine >>$serviceLogFile 2>&1 & echo \$! >$pidFile"
#    echo $cmd
   local SCHIL="/bin/bash"
   sudo -u $serviceUser $SCHIL -c "$cmd" || return 1
#   sudo -u $serviceUser $SHELL -c "$cmd" || return 1
   sleep 0.1
   servicePid=`cat $pidFile`
   if checkProcessIsRunning $servicePid; then :; else
     echo "\n$serviceName start failed, see logfile."
     return 1
   fi
   return 0; }

stopServiceProcess () {
#   echo "Start stopServiceProcess"
   kill $servicePid || return 1
#   echo "Kill-opdracht gegeven"
   local i=0
   local iMax=`expr $maxShutdownTime \* 10`
#   echo "i: $i, iMax: $iMax"
   while [ $i -lt $iMax ]
      do 
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
#         echo "Kill gelukt. pidFile verwijderen"
         rm -f $pidFile
         return 0
         fi
      sleep 0.1
      i=`expr $i + 1`
      done
   echo "\n$serviceName did not terminate within $maxShutdownTime seconds, sending SIGKILL..."
   kill -s KILL $servicePid || return 1
   i=0
   while [ $i -lt $iMax ]
      do
      checkProcessIsRunning $servicePid
      if [ $? -ne 0 ]; then
        rm -f $pidFile
        return 0
      fi
      sleep 0.1
      done
   echo "Error: $serviceName could not be stopped within $maxShutdownTime+$killWaitTime seconds!"
   return 1; }

runInConsoleMode () {
#    echo "Start RunInConsoleMode"
   getServicePid
   if [ $? -eq 0 ]; then
     echo "$serviceName is already running"
     return 1
   fi
#    echo "Service loopt nog niet"
   cd $applDir || return 1
#    echo $PWD
#    echo "Uitvoeren!"
   sudo -u $serviceUser $javaCommandLine || return 1
   if [ $? -eq 0 ]; then
     return 1
   fi
   return 0; }

startService () {
#    echo "Begin startService"
   getServicePid
   if [ $? -eq 0 ]; then
     echo "$serviceName is already running"
     return 0
   fi
   echo "Starting $serviceName   "
#    echo "PID loopt nog niet, StartServiceProcess"
   startServiceProcess
#    echo "Service gestart"
   if [ $? -ne 0 ]; then
     return 1
   fi
   return 0; }

stopService () {
#   echo "Start stopService"
   getServicePid
   if [ $? -ne 0 ]; then
     echo "$serviceName is not running"
     return 0
   fi
   echo "Stopping $serviceName   "
   stopServiceProcess
   if [ $? -ne 0 ]; then
     return 1
   fi
   return 0; }

checkServiceStatus () {
#   echo -n "Checking for $serviceName:   "
#   if getServicePid; then :; else
#      fi
   return 0; }

main () {
   echo "Start main"
   case "$1" in
      console)                                             # runs the Java program in console mode
#          echo "Run in Console Mode"
         runInConsoleMode
         ;;
      start)                                               # starts the Java program as a Linux service
         logger -p daemon.info -t LightSwitch "Start service"
#          echo "Start als een service"
         startService
         ;;
      stop)                                                # stops the Java program service
         logger -p daemon.info -t LightSwitch "Stop service"
         echo "Stop de service"
         stopService
         ;;
      restart)                                             # stops and restarts the service
         stopService && startService
         ;;
      status)                                              # displays the service status
         checkServiceStatus
         ;;
      *)
         echo "Usage: $0 {console|start|stop|restart|status}"
         exit 1
         ;;
      esac

 }

main $1

exit 0