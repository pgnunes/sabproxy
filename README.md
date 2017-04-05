# sabproxy [![Build Status](https://travis-ci.org/pgnunes/sabproxy.svg)](https://travis-ci.org/pgnunes/sabproxy) [![Coverage Status](https://coveralls.io/repos/github/pgnunes/sabproxy/badge.svg?branch=develop)](https://coveralls.io/github/pgnunes/sabproxy?branch=develop) 
## Simple Ad Blocker Proxy
SABProxy is an adblock proxy based on DNS filtering 

### Generate java jar file
`mvn clean verify`

### Start SABProxy
`java -jar target/sabproxy-0.0.1-SNAPSHOT.jar`

### Navigate Ad free
Configure your browser/network proxy settings to the host where SABProxy is running like:
- Host: `120.7.0.0.1`
- Port: `3129`
