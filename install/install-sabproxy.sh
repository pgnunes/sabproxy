#!/bin/bash

#
# sabproxy.tk
#
# Installation script
# - Supports installation on systemd OS's
# - Operations:
#   - Create directory /opt/sabproxy
#   - Add user 'sabproxy'
#   - Creates systemd service
#   - Download application (jar) from sabproxy.tk
#   - Enable and start service
#

echo ""
echo ""
echo "|==================================================================|"
echo "| SABProxy - Installation script                                   |"
echo "|==================================================================|"
echo ""

# Check if running script as root
if [ "$EUID" -ne 0 ]
  then echo "[ERROR] Please run this script as root."
  echo ""
  exit -1
fi


# Check if this system is running systemd
if [[ $(ps --no-headers -o comm 1) = systemd ]]
    then
        echo "[INFO] systemd - OK"
        echo ""
    else
        echo "[ERROR] Installer supports only systemd. Please install SABProxy manually."
        echo ""
        exit -1
fi

# Check for java version
if (( $(java -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*"/\1\2/p;') < 18 )); then
    ...
fi




# Create user 'sabproxy'
echo "[INFO] Create user 'sabproxy'..."
useradd -m sabproxy
echo "[INFO] Done"
echo ""


# Create application dir
echo "[INFO] Create application folder [/opt/sabproxy]"
mkdir -p /opt/sabproxy
chown -R sabproxy:sabproxy /opt/sabproxy
echo "[INFO] Done"
echo ""


# Check latest version
echo "[INFO] Check latest version..."
wget http://sabproxy.tk/release/latest.txt -O /tmp/sabproxy-latest.txt
echo "[INFO] Latest version is: $(cat /tmp/sabproxy-latest.txt)"
echo ""

# Stop the service if SABProxy already is running on this host
if [[ $(systemctl is-active sabproxy.service) = active ]]; then
    echo "[INFO] SABProxy already running on this system. Stopping service..."
    systemctl stop sabproxy.service
    echo "[INFO] Done"
    echo ""
fi

# Download SABProxy
echo "[INFO] Download SABProxy..."
wget http://sabproxy.tk/release/$(cat /tmp/sabproxy-latest.txt) -O /tmp/sabproxy.jar
cp -f /tmp/sabproxy.jar /opt/sabproxy/sabproxy.jar
chown -R sabproxy:sabproxy /opt/sabproxy
# double check we actually got the latest version in place...
if [[ $(diff -s /tmp/sabproxy.jar /opt/sabproxy/sabproxy.jar | tr ' ' '\n' | tail -1) = identical ]]
    then
        echo "[INFO] Done"
        echo ""
        # cleanup...
        rm -rf /tmp/sabproxy*
    else
        echo "[ERROR] Something went wrong. Please try again or install SABProxy manually."
        echo ""
        exit -1
fi


# Create systemd script file
echo "[INFO] Create service description..."
SERVICE_FILE="/etc/systemd/system/sabproxy.service"
echo "[Unit]" > $SERVICE_FILE
echo "Description=SABProxy" >> $SERVICE_FILE
echo "After=syslog.target network.target" >> $SERVICE_FILE
echo "" >> $SERVICE_FILE
echo "[Service]" >> $SERVICE_FILE
echo "User=sabproxy" >> $SERVICE_FILE
echo "ExecStart=/usr/bin/java -jar /opt/sabproxy/sabproxy.jar" >> $SERVICE_FILE
echo "Restart=on-failure" >> $SERVICE_FILE
echo "SuccessExitStatus=143" >> $SERVICE_FILE
echo "" >> $SERVICE_FILE
echo "[Install]" >> $SERVICE_FILE
echo "WantedBy=multi-user.target" >> $SERVICE_FILE
echo "" >> $SERVICE_FILE
echo "[INFO] Done"
echo ""

echo "[INFO] Set permissions..."
chmod 664 /etc/systemd/system/sabproxy.service
echo "[INFO] Done"
echo ""

echo "[INFO] Reload systemd daemon..."
systemctl daemon-reload
echo "[INFO] Done"
echo ""

echo "[INFO] Enable SABProxy service..."
systemctl enable sabproxy.service
echo "[INFO] Done"
echo ""

# Check that service is installed and enabled
if [[ $(systemctl is-enabled sabproxy.service) != enabled ]]; then
    echo "[ERROR] Failed! Please check for error messages. Try again or install SABProxy manually."
    echo ""
    exit -1
fi

# Start the service
echo "[INFO] Start SABProxy..."
systemctl start sabproxy.service
sleep 5
if [[ $(systemctl is-active sabproxy.service) != active ]]; then
    echo "[ERROR] Failed! Please check for error messages. Try again or install SABProxy manually."
    echo ""
    exit -1
fi

echo "[INFO] SABProxy installed, up and running!"
echo "[INFO] Defaults:"
echo "[INFO]   Webserver    - http://localhost:8080"
echo "[INFO]   Proxy server - http://localhost:3129"
echo "[INFO]   (replace 'localhost' with hostname or IP address when configuring your network settings)"
echo ""
echo "[INFO] All Done!"
echo "[INFO] Visit http://sabproxy.tk"
echo ""
echo ""
