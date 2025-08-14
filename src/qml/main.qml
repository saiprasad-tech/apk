import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15

ApplicationWindow {
    id: window
    width: 1024
    height: 768
    visible: true
    title: "Pixhawk GCS"
    
    property bool isConnected: false
    property bool isArmed: false
    
    // Main layout
    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 20
        
        // Header
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 60
            color: "#2E3440"
            radius: 8
            
            RowLayout {
                anchors.centerIn: parent
                
                Text {
                    text: "Pixhawk GCS"
                    font.pixelSize: 24
                    font.bold: true
                    color: "#ECEFF4"
                }
                
                Rectangle {
                    width: 12
                    height: 12
                    radius: 6
                    color: isConnected ? "#A3BE8C" : "#BF616A"
                }
                
                Text {
                    text: isConnected ? "Connected" : "Disconnected"
                    font.pixelSize: 14
                    color: "#D8DEE9"
                }
            }
        }
        
        // Connection panel
        GroupBox {
            title: "Connection"
            Layout.fillWidth: true
            
            RowLayout {
                TextField {
                    id: hostField
                    placeholderText: "Host (0.0.0.0 for listen)"
                    text: "0.0.0.0"
                    Layout.preferredWidth: 200
                }
                
                TextField {
                    id: portField
                    placeholderText: "Port"
                    text: "14550"
                    Layout.preferredWidth: 100
                    validator: IntValidator { bottom: 1; top: 65535 }
                }
                
                Button {
                    text: isConnected ? "Disconnect" : "Connect"
                    onClicked: {
                        if (isConnected) {
                            // Call disconnect
                            isConnected = false
                        } else {
                            // Call connect
                            isConnected = true
                        }
                    }
                }
            }
        }
        
        // Flight controls
        GroupBox {
            title: "Flight Controls"
            Layout.fillWidth: true
            enabled: isConnected
            
            RowLayout {
                Button {
                    text: isArmed ? "DISARM" : "ARM"
                    color: isArmed ? "#BF616A" : "#5E81AC"
                    onClicked: {
                        isArmed = !isArmed
                        // Call arm/disarm command
                    }
                }
                
                Button {
                    text: "Takeoff"
                    color: "#A3BE8C"
                    enabled: isArmed
                    onClicked: {
                        // Call takeoff command
                        console.log("Takeoff requested")
                    }
                }
                
                Button {
                    text: "RTL"
                    color: "#EBCB8B"
                    onClicked: {
                        // Call return to launch
                        console.log("Return to Launch")
                    }
                }
            }
        }
        
        // Vehicle status
        GroupBox {
            title: "Vehicle Status"
            Layout.fillWidth: true
            Layout.fillHeight: true
            
            GridLayout {
                columns: 2
                anchors.fill: parent
                
                Text { text: "System ID:" }
                Text { text: "1"; id: systemIdText }
                
                Text { text: "Mode:" }
                Text { text: "STABILIZE"; id: modeText }
                
                Text { text: "Battery:" }
                Text { text: "12.4V (85%)"; id: batteryText }
                
                Text { text: "GPS:" }
                Text { text: "3D Fix (12 sats)"; id: gpsText }
                
                Text { text: "Position:" }
                Text { text: "37.123456, -122.345678"; id: positionText }
                
                Text { text: "Altitude:" }
                Text { text: "125.3 m"; id: altitudeText }
            }
        }
    }
}