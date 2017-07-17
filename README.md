# sabproxy [![Build Status](https://travis-ci.org/pgnunes/sabproxy.svg)](https://travis-ci.org/pgnunes/sabproxy) [![Coverage Status](https://coveralls.io/repos/github/pgnunes/sabproxy/badge.svg?branch=master)](https://coveralls.io/github/pgnunes/sabproxy?branch=master) 
## Simple Ad Blocker Proxy
SABProxy is a DNS filtering adblocker (and track-blocker) proxy based on [LittleProxy](https://github.com/adamfisk/LittleProxy). It can run locally on your box or on a remote server (including a [RaspberryPI](https://www.raspberrypi.org/)). It only requires Java.

### Screenshot
![alt text]( https://github.com/pgnunes/sabproxy/raw/develop/screenshot/screenshot.png "Homepage")

### Installation (Linux)
SABProxy can be installed on any Linux OS (running systemd) using a simple bash script:

<small>1.</small>`wget https://raw.githubusercontent.com/pgnunes/sabproxy/master/install/install-sabproxy.sh`
<br/><small>2.</small>`sudo bash install-sabproxy.sh`

### Update
SABProxy supports auto-updating. Once started please navigate to the 'Check Update' tab and update directly from the web interface.

### Configuration
Configure your browser/network proxy settings to the host where SABProxy is running like (running locally):
- Host: `127.0.0.1`
- Port: `3129`

### Web Interface
Open your browser and point to the IP where SABProxy is running:
`http://127.0.0.1:8080/` (user: `admin`, password: `admin`)

### Help / Requests
If you need help or want to request a new feature please [open an issue](https://github.com/pgnunes/sabproxy/issues) and it will be sorted as soon as possible.

