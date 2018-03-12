#!/bin/bash

function checkAndInstall {
	echo -n "  Checking package: $1"
	if [ $(dpkg-query -W -f='${Status}' $1 2>/dev/null | grep -c "ok installed") -eq 0 ]; then
		echo -e "\t\t${red}Not installed!${reset}"
		echo "  executing: sudo apt-get install $1"
		sudo apt-get install $1
	else
		echo -e "\t\t${green}Ok${reset}"
	fi
}

checkAndInstall maven
mvn install

unset checkAndInstall
