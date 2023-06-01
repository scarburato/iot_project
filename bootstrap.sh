#!/bin/bash

CONTAINER_NAME=trusting_sammet

docker stop $CONTAINER_NAME

gnome-terminal \
	--tab-with-profile=IoT --title="cooja" -e "docker start -ai $CONTAINER_NAME" \
	--tab-with-profile=IoT --title="mosquitto" -e "docker exec -it $CONTAINER_NAME bash -c 'mosquitto -v -c ~/project/mosquitto.conf'" \
	--tab-with-profile=IoT --title="bridge" -e "docker exec -it $CONTAINER_NAME bash -c 'cd ~/contiki-ng/examples/rpl-border-router; bash'" \
	--tab-with-profile=IoT --title="server" -e "docker exec -it $CONTAINER_NAME bash -c 'cd ~/project/jcoordinator/target; bash'" \
	--tab-with-profile=IoT --title="altra roba" -e "docker exec -it $CONTAINER_NAME bash -c 'cd ~/project; bash'"
