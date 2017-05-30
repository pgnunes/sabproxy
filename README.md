# sabproxy [![Build Status](https://travis-ci.org/pgnunes/sabproxy.svg)](https://travis-ci.org/pgnunes/sabproxy) [![Coverage Status](https://coveralls.io/repos/github/pgnunes/sabproxy/badge.svg?branch=develop)](https://coveralls.io/github/pgnunes/sabproxy?branch=develop) 
## Simple Ad Blocker Proxy
SABProxy is a DNS filtering adblocker (and track-blocker) proxy based on [LittleProxy](https://github.com/adamfisk/LittleProxy). It can run locally on your box or on a remote server (including a [RaspberryPI](https://www.raspberrypi.org/)). It only requires Java.

### Screenshot
![alt text]( https://github.com/pgnunes/sabproxy/raw/develop/screenshot/screenshot.png "Homepage")

### Generate java jar file
`mvn clean verify`

### Start SABProxy
`java -jar target/sabproxy-0.0.1-SNAPSHOT.jar`

### Navigate Ad free
Configure your browser/network proxy settings to the host where SABProxy is running like (running locally):
- Host: `127.0.0.1`
- Port: `3129`

### Ads Blocking Stats
Open your browser and point to the IP where SABProxy is running:
`http://127.0.0.1:8080/`
