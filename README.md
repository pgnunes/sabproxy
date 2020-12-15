# sabproxy [![Build Status](https://travis-ci.org/pgnunes/sabproxy.svg)](https://travis-ci.org/pgnunes/sabproxy) [![Coverage Status](https://coveralls.io/repos/github/pgnunes/sabproxy/badge.svg?branch=master)](https://coveralls.io/github/pgnunes/sabproxy?branch=master) 
## Simple Ad Blocker Proxy
SABProxy is a DNS filtering adblocker (and track-blocker) proxy based on [LittleProxy](https://github.com/adamfisk/LittleProxy). It can run locally on your box or on a remote server (including a [RaspberryPI](https://www.raspberrypi.org/)). It only requires Java.

### Screenshot
![Screenshot]( https://github.com/pgnunes/sabproxy/raw/develop/screenshot/screenshot.png "Homepage")

### Docker 
[![](https://images.microbadger.com/badges/image/pgnunes/sabproxy.svg)](https://microbadger.com/images/pgnunes/sabproxy "Get your own image badge on microbadger.com") [![](https://images.microbadger.com/badges/version/pgnunes/sabproxy.svg)](https://microbadger.com/images/pgnunes/sabproxy "Get your own version badge on microbadger.com")

Run SABProxy using [Docker](https://hub.docker.com/r/pgnunes/sabproxy/ "Docker") 

`docker run -p 8080:8080 -p 3129:3129 pgnunes/sabproxy` 

### Configuration
Configure your browser/network proxy settings to the host where SABProxy is running like (running locally):
- Host: `127.0.0.1`
- Port: `3129`

### Web Interface
Open your browser and point to the IP where SABProxy is running:
`http://127.0.0.1:8080/` (user: `admin`, password: `admin`)

### Help / Requests
If you need help or want to request a new feature please [open an issue](https://github.com/pgnunes/sabproxy/issues) and it will be sorted as soon as possible.

#### Building Docker image locally
`docker image build --no-cache -t pgnunes/sabproxy .` 
