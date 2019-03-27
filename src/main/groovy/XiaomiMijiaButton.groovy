/**
 *  Xiaomi Mijia Smart Switch - Smart Things Handler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  This is a fork from https://github.com/bspranger/Xiaomi/blob/master/devicetypes/bspranger/xiaomi-button.src/xiaomi-button.groovy
 *  Original device handler code by a4refillpad, adapted for use with Aqara model by bspranger
 *  Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, & veeceeoh
 *
 *  Known issues: ??
 *
 */

metadata {
    definition (name: "Xiaomi Mijia Smart Switch", namespace: "claudio.dev", author: "claudio") {
        capability "Battery"
        capability "Sensor"
        capability "Button"
        capability "Holdable Button"
        capability "Actuator"
        capability "Momentary"
        capability "Configuration"
        capability "Health Check"

        attribute "lastPressed", "string"
        attribute "batteryRuntime", "string"
        attribute "button", "enum", ["pushed", "released"]

        fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_switch", deviceJoinName: "Original Xiaomi Button"

        command "resetBatteryRuntime"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"button", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
            tileAttribute ("device.button", key: "PRIMARY_CONTROL") {
                attributeState("default", label:'Released', action: "momentary.push", backgroundColor:"#ffffff", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonReleased.png")
                attributeState("released", label:'Released', action: "momentary.push", backgroundColor:"#ffffff", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonReleased.png")
                attributeState("pushed", label:'Pushed', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
            }
            tileAttribute("device.lastPressed", key: "SECONDARY_CONTROL") {
                attributeState "lastPressed", label:'Last Pressed: ${currentValue}'
            }
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
                    backgroundColors:[
                            [value: 10, color: "#bc2323"],
                            [value: 26, color: "#f1d801"],
                            [value: 51, color: "#44b621"]
                    ]
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 2) {
            state "batteryRuntime", label:'Battery Changed: ${currentValue}'
        }
        main (["button"])
        details(["button","battery","batteryRuntime"])
    }

    preferences {
        //Battery Reset Config
        input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "Changed Battery Data Reset"
        input name: "battReset", type: "bool", title: "Battery Changed?"
        //Live Logging Message Display Config
        input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "Live Logging"
        input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
        input name: "debugLogging", type: "bool", title: "Display debug log messages?"
    }
}

// METHODS START

/*
    Configuration Methods
 */

def installed() {
    displayInfoLog("Installing - Started")
    initialize()
    displayInfoLog("Installing - Completed")
}

def updated() {
    displayInfoLog("Updating - Started")
    initialize()
    displayInfoLog("Updating - Completed")
}

def initialize() {
    clearButtonStatus()
    if (!device.currentState('batteryRuntime')?.value || battReset) {
        displayInfoLog("Setting battery runtime")
        sendEvent(name: "batteryRuntime", value: getDate())
        if (battReset) {
            displayInfoLog("Reset battery setting on device")
            device.updateSetting("battReset", false)
        }
    }
    sendEvent(name: "numberOfButtons", value: 4)
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    return
}

/*
    Parse incoming device messages to generate events
 */

def parse(String description) {
    displayDebugLog(": Parsing '${description}'")
    def incomingEvent = resolveIncomingEvent(description)
    switch (incomingEvent) {
        case 'BUTTON_1':
        case 'BUTTON_2':
        case 'BUTTON_3':
        case 'BUTTON_4':
            return handleButtonEvent(parsableEvents().indexOf(incomingEvent))
        case 'CATCH_ALL':
            displayInfoLog("CATCH_ALL message: '${description}'")
            return parseCatchAllMessage(description)
        case 'READ_ATTR':
            displayInfoLog("READ_ATTR message: '${description}'")
            return parseReadAttrMessage(description)
        case 'BUTTON_1_RELEASED':
            // Not used in this handler
            return null
        default:
            displayInfoLog("Unknown message: '${description}'")
            return null
    }
}

static String resolveIncomingEvent(String description) {
    if (description == 'on/off: 0') {
        return parsableEvent(1)
    } else if (description == 'on/off: 1') {
        return parsableEvent(5)
    } else if (description.startsWith('catchall:')) {
        if (description.size() == 64) {
            def button = description[63] as int
            if (button > 1 && button < 5) {
                return parsableEvent(button)
            }
        }
        return parsableEvent(6)
    } else if (description.startsWith('read attr - raw:')) {
        def targetString = description.split(',')[0]
        if (targetString.size() == 37 &&  targetString.matches('.*0080200\\d$')) {
            def button = targetString[36] as int
            if (button > 1 && button < 5) {
                return parsableEvent(button)
            }
        }
        return parsableEvent(7)
    } else {
        return parsableEvent(0)
    }
}

// Poor Man's Enum since Smartthings doesn't support it :(
static def parsableEvents() {
    [
            "UNKNOWN",
            "BUTTON_1",
            "BUTTON_2",
            "BUTTON_3",
            "BUTTON_4",
            "BUTTON_1_RELEASED",
            "CATCH_ALL",
            "READ_ATTR"
    ]
}
static def parsableEvent(eventNumber) {
    parsableEvents()[eventNumber]
}

private def handleButtonEvent(int buttonNumber) {
    def buttonPushedEvent = createEvent(
            name           : 'button',
            value          : 'pushed',
            data           : [buttonNumber: buttonNumber],
            descriptionText: "Button ${buttonNumber} pushed",
            isStateChange  : true
    )
    def lastPressedEvent = createEvent(
            name     : "lastPressed",
            value    : getDate(),
            displayed: false
    )
    runIn(1, clearButtonStatus)
    displayInfoLog(buttonPushedEvent.descriptionText)
    return [buttonPushedEvent, lastPressedEvent]
}

def clearButtonStatus() {
    sendEvent(name: "button", value: "released", isStateChange: true, displayed: false)
}

private static def getDate() {
    new Date().format("yyyy-MM-dd HH:mm:ss:S")
}

private def displayDebugLog(message) {
    if (debugLogging)
        log.debug "${getDate()}:${device.displayName}${message}"
}

private def displayInfoLog(message) {
    if (infoLogging)
        log.info "${getDate()}:${device.displayName}: ${message}"
}

/*
    Battery and other supporting methods
 */

private Map parseReadAttrMessage(String description) {
    def cluster = description.split(",").find { it.split(":")[0].trim() == "cluster" }?.split(":")[1].trim()
    def attrId = description.split(",").find { it.split(":")[0].trim() == "attrId" }?.split(":")[1].trim()
    def value = description.split(",").find { it.split(":")[0].trim() == "value" }?.split(":")[1].trim()
    def data = ""
    def modelName = ""
    def model = value
    Map resultMap = [:]

    // Process message on short-button press containing model name and battery voltage report
    if (cluster == "0000" && attrId == "0005") {
        if (value.length() > 45) {
            model = value.split("02FF")[0]
            data = value.split("02FF")[1]
            if (data[4..7] == "0121") {
                def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]), 16))
                resultMap = getBatteryResult(BatteryVoltage)
            }
            data = ", data: ${value.split("02FF")[1]}"
        }

        // Parsing the model name
        for (int i = 0; i < model.length(); i += 2) {
            def str = model.substring(i, i + 2);
            def NextChar = (char) Integer.parseInt(str, 16);
            modelName = modelName + NextChar
        }
        displayDebugLog(" reported model: $modelName$data")
    }

    if (resultMap?.descriptionText){
        displayDebugLog(": '${resultMap.descriptionText}'")
    }
    return resultMap
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def catchall = zigbee.parse(description)

    if (catchall.clusterId == 0x0000) {
        def MsgLength = catchall.data.size()
        // Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
        if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
            for (int i = 4; i < (MsgLength-3); i++) {
                if (catchall.data.get(i) == 0x21) { // check the data ID and data type
                    // next two bytes are the battery voltage
                    resultMap = getBatteryResult((catchall.data.get(i+2)<<8) + catchall.data.get(i+1))
                    break
                }
            }
        }
    }

    if (resultMap?.descriptionText){
        displayDebugLog(": '${resultMap.descriptionText}'")
    }
    return resultMap
}

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private Map getBatteryResult(rawValue) {
    // raw voltage is normally supplied as a 4 digit integer that needs to be divided by 1000
    // but in the case the final zero is dropped then divide by 100 to get actual voltage value
    def rawVolts = rawValue / 1000
    def minVolts = 2.5
    def maxVolts = 3.0
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))
    def descText = "Battery at ${roundedPct}% (${rawVolts} Volts)"
    displayInfoLog(descText)
    return [
            name: 'battery',
            value: roundedPct,
            unit: "%",
            isStateChange:true,
            descriptionText : "$device.displayName $descText"
    ]
}
