pushd .

# Border router
cd ~/contiki-ng/examples/rpl-border-router
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 border-router.dfu-upload

# co2 + fan
cd ~/project/hybrid/airquality
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 airquality.dfu-upload

# water_pump
cd ~/project/coap/water_pump
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2 water_pump.dfu-upload

# dehumidifier
cd ~/project/coap/dehumidifier
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM3 dehumidifier.dfu-upload

# light
#cd ~/project/coap/light
#make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM4 light.dfu-upload

# float
cd ~/project/mqtt/float
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM4 float.dfu-upload

# humidity
cd ~/project/mqtt/humidity
make -j4 TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM5 humidity.dfu-upload

popd
