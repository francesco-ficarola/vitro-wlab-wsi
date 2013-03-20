#!/bin/bash
echo "\n*** Starting the ppp modem..."
sudo xterm -e pppd debug passive noauth nodetach 115200 /dev/ttyUSB0 nocrtscts nocdtrcts lcp-echo-interval 0 noccp noip ipv6 ::23,::24 &
sleep 15
echo "*** DONE!"
echo "\n*** Adding the 6lowpan net to ppp0..."
sudo ifconfig ppp0 add fec0:0:0:1::100/64
echo "*** DONE!"
echo "------------------------------------"
ifconfig ppp0
echo "------------------------------------"
echo "\n*** Starting JCoAP Proxy..."
java -cp ".:../lib/*" vitro.wlab.wsi.proxy.Proxy
