/**
 *  Smart Entryway
 *
 *  Copyright 2017 Paul Smith
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smart Entryway",
    namespace: "paulsmithkc",
    author: "Paul Smith",
    description: "Simple automation for entryways, doors, and outdoor lighting.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png")


preferences {
    page(name: "page1", install: true, uninstall: true)
}

def page1() {
	dynamicPage(name: "page1") {
        def installed = (app.installationState == "COMPLETE");
        def indoorHidden = installed && !indoorLights;
        def outdoorHidden = installed && !outdoorLights;
        def doorHidden = installed && !doorOpen && !doorKnock;

        section(hideable: installed, hidden: indoorHidden, "Indoor Lights") {
            input "indoorSwitch", "capability.switch", multiple: true, required: false, title: "What switch(s) controls the indoor lights?"
            //input "indoorButton", "capability.button", multiple: true, required: false, title: "What button(s) controls the indoor lights?"
            input "indoorLights", "capability.switch", multiple: true, required: false, title: "Which indoor lights do you want to control with this app?"
            input "indoorMotion", "capability.motionSensor", multiple: true, required: false, title: "Whould you like the indoor lights to turn on when motion is detected?"
            input "indoorTimeout", "number", required: false, title: "How many minutes do you want to wait to turn the indoor lights off, when nothing is happening?"
        }
        section(hideable: installed, hidden: outdoorHidden, "Outdoor Lights") {
            input "outdoorSwitch", "capability.switch", multiple: true, required: false, title: "What switch(s) controls the outdoor lights?"
            //input "outdoorButton", "capability.button", multiple: true, required: false, title: "What button(s) controls the outdoor lights?"
            input "outdoorLights", "capability.switch", multiple: true, required: false, title: "Which outdoor lights do you want to control with this app?"
            input "outdoorMotion", "capability.motionSensor", multiple: true, required: false, title: "Whould you like the outdoor lights to turn on when motion is detected?"
            input "outdoorTimeout", "number", required: false, title: "How many minutes do you want to wait to turn the outdoor lights off, when nothing is happening?"
        }
    	section(hideable: installed, hidden: false, "Sunset") {
            input "sunsetOffset", "number", required: true, title: "How many minutes before sunset do you want to turn the lights on?"
            input "sunsetLights", "capability.switch", multiple: true, required: false, title: "Which lights do you want to turn on at sunset?"
            input "sunriseOffset", "number", required: true, title: "How many minutes before sunrise do you want to turn the lights off?"
        }
    	section(hideable: installed, hidden: doorHidden, hideWhenEmpty: true, "Door") {
            input "doorOpen", "capability.contactSensor", multiple: true, required: false, title: "Whould you like the lights to turn on when the door is opened?"
            input "doorKnock", "capability.accelerationSensor", multiple: true, required: false, title: "Whould you like the lights to turn on when the door is knocked?"
            input "knockDayLights", "capability.switch", multiple: true, required: false, title: "Which lights do you want to turn on when the door is knocked during the day?"
            input "knockNightLights", "capability.switch", multiple: true, required: false, title: "Which lights do you want to turn on when the door is knocked during the night?"
        }
        /*
        section(hideable: installed, "Sound") {
            //input "soundMedia", "capability.mediaController"
            //input "soundMusic", "capability.musicPlayer"
            //input "soundTone", "capability.tone"
            //input "soundAlarm", "capability.alarm"
            //input "soundNotification", "capability.audioNotification"
            //input "soundSpeech", "capability.speechSynthesis"
        }
        */
        section(mobileOnly: true, "Name") {
			label title: "Do you want to name this entryway app?", required: false
		}
    }
}

def installed() {
	log.debug("Installed with settings: ${settings}");
	initialize();
}

def updated() {
	log.debug("Updated with settings: ${settings}");
	unsubscribe();
	initialize();
}

def initialize() {
    //subscribe(app, appTouch)

    // Sunset and Sunrise
    subscribe(location, "sunsetTime", sunsetTimeHandler);
    subscribe(location, "sunriseTime", sunriseTimeHandler);
    def sunset = scheduleSunset(location.currentValue("sunsetTime"));
    def sunrise = scheduleSunrise(location.currentValue("sunriseTime"));
    def now = new Date();
    state.night = timeOfDayIsBetween(sunset, sunrise, now, location.timeZone);
    log.debug(state.night ? "it is currently night" : "it is currently day");
	setLastActivatedIndoor("on")
    setLastActivatedOutdoor("on")

    // Door sensors
    if (doorOpen) {
		subscribe(doorOpen, "contact.open", doorOpenHandler);
    }
    if (doorKnock) {
    	subscribe(doorKnock, "acceleration.active", doorKnockHandler);
    }
    
    // Indoor Lights and Switches
    if (indoorSwitch) {
        subscribe(indoorSwitch, "switch", indoorSwitchHandler);
        subscribe(indoorSwitch, "level", indoorDimHandler);
    }
    //if (indoorButton) {
    //    subscribe(indoorButton, "button", indoorButtonHandler);
    //}
    if (indoorMotion) {
    	subscribe(indoorMotion, "motion", indoorMotionHandler);
    }
    
    // Outdoor Lights and Switches
    if (outdoorSwitch) {
    	subscribe(outdoorSwitch, "switch", outdoorSwitchHandler);
    	subscribe(outdoorSwitch, "level", outdoorDimHandler);
    }
    //if (outdoorButton) {
    //    subscribe(outdoorButton, "button", outdoorButtonHandler);
    //}
    if (outdoorMotion) {
    	subscribe(outdoorMotion, "motion", outdoorMotionHandler);
    }
}

def setLastActivatedIndoor(value) {
	state.lastActivatedIndoor = now();
    if (indoorTimeout && indoorLights && value == "on") {
        runIn(60 * indoorTimeout, insideTimeoutHandler)
    }
}

def setLastActivatedOutdoor(value) {
	state.lastActivatedOutdoor = now();
    if (outdoorTimeout && outdoorLights && value == "on") {
        runIn(60 * outdoorTimeout, outsideTimeoutHandler)
    }
}

def insideTimeoutHandler() {
    /*if (indoorMotion) {
        // Don't timeout if there is still motion detected
    	def motionState = indoorMotion.currentState("motion");
        if (motionState.value == "active") { return; }
    }*/
    
    def elapsed = now() - state.lastActivatedIndoor;
    def timeout = (indoorTimeout - 1) * 60 * 1000;
    if (elapsed >= timeout) {
        log.debug("indoor timeout");
        turnOffIndoorLights()
        state.lastActivatedIndoor = now();
    }
}

def outsideTimeoutHandler() {
    /*if (outdoorMotion) {
        // Don't timeout if there is still motion detected
    	def motionState = outdoorMotion.currentState("motion");
        if (motionState.value == "active") { return; }
    }*/
    
    def elapsed = now() - state.lastActivatedOutdoor;
    def timeout = (outdoorTimeout - 1) * 60 * 1000;
    if (elapsed >= timeout) {
        log.debug("outdoor timeout");
        turnOffOutdoorLights()
        state.lastActivatedOutdoor = now();
    }
}

//def appTouch(evt) {
//	indoorLights?.on()
//}

def sunsetTimeHandler(evt) {
    scheduleSunset(evt.value)
}

def sunriseTimeHandler(evt) {
    scheduleSunrise(evt.value)
}

def scheduleSunset(sunsetString) {
    if (sunsetOffset == null) { sunriseOffset = 0; }
    
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    def timeBeforeSunset = new Date(sunsetTime.time - (sunsetOffset * 60 * 1000))
    state.sunset = timeBeforeSunset
    log.debug("sunset is $timeBeforeSunset")
    runOnce(timeBeforeSunset, sunsetHandler)
    return timeBeforeSunset
}

def scheduleSunrise(sunriseString) {
    if (sunriseOffset == null) { sunriseOffset = 0; }
    
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    def timeBeforeSunrise = new Date(sunriseTime.time - (sunriseOffset * 60 * 1000))
    state.sunrise = timeBeforeSunrise
    log.debug("sunrise is $timeBeforeSunrise")
    runOnce(timeBeforeSunrise, sunriseHandler)
    return timeBeforeSunrise
}

def sunsetHandler() {
    log.debug("sunset")
    state.night = true
    setLastActivatedIndoor("on")
    setLastActivatedOutdoor("on")
    
    sunsetLights?.on()
    turnOnOutdoorLights()
}

def sunriseHandler() {
    log.debug("sunrise")
    state.night = false
    setLastActivatedIndoor("on")
    setLastActivatedOutdoor("on")
    
    sunsetLights?.off()
    turnOffIndoorLights()
    turnOffOutdoorLights()
}

def doorOpenHandler(evt) {
    log.debug("door open detected")
    if (state.night) {
		turnOnIndoorLights()
    	turnOnOutdoorLights()
    }
}

def doorKnockHandler(evt) {
    log.debug("door knock detected")
    if (state.night) {
        if (knockNightLights) {
            setLastActivatedIndoor("on")
    		setLastActivatedOutdoor("on")
            knockNightLights?.on()
        }
    } else {
        if (knockDayLights) {
            setLastActivatedIndoor("on")
    		setLastActivatedOutdoor("on")
            knockDayLights?.on()
        }
    }
}

def indoorSwitchHandler(evt) {
    if (evt.value == "on") {
        log.debug("indoor switch turned on")
    	turnOnIndoorLights();
    } else if (evt.value == "off") {
    	log.debug("indoor switch turned off")
    	turnOffIndoorLights();
    }
}

def outdoorSwitchHandler(evt) {
    if (evt.value == "on") {
        log.debug("outdoor switch turned on")
    	turnOnOutdoorLights();
    } else if (evt.value == "off") {
    	log.debug("outdoor switch turned off")
    	turnOffOutdoorLights();
    }
}

/*
def indoorButtonHandler(evt) {
	setLastActivatedIndoor("on")
    if (evt.value == "pushed") {
        log.debug("indoor button pushed")
        if (indoorSwitch) { 
        	indoorSwitch?.toggle() 
        } else {
    		indoorLights?.toggle()
        }
    }
}

def outdoorButtonHandler(evt) {
	setLastActivatedOutdoor("on")
    if (evt.value == "pushed") {
        log.debug("outdoor button pushed")
        if (outdoorSwitch) { 
        	outdoorSwitch?.toggle() 
        } else {
    		outdoorLights?.toggle()
        }
    }
}
*/

def indoorDimHandler(evt) {
    log.debug("indoor dimmer: $evt.value")
    setLastActivatedIndoor("on")
    indoorLights?.setLevel(evt.value)
}

def outdoorDimHandler(evt) {
    log.debug("outdoor dimmer: $evt.value")
    setLastActivatedOutdoor("on")
    outdoorLights?.setLevel(evt.value)
}

def indoorMotionHandler(evt) {
	setLastActivatedIndoor("on")
    if (evt.value == "active") {
        log.debug("indoor motion detected")
        if (state.night) {
            turnOnIndoorLights()
        }
    }
}

def outdoorMotionHandler(evt) {
	setLastActivatedOutdoor("on")
    if (evt.value == "active") {
        log.debug("outdoor motion detected")
        if (state.night) {
            turnOnOutdoorLights()
        }
    }
}

def turnOnIndoorLights() {
	setLastActivatedIndoor("on");
    def sw = indoorSwitch;
    if (sw) {
        def switchValue = sw.currentValue("switch");
        if (switchValue != "on") { sw?.on(); }
    }
    indoorLights?.on();
}

def turnOffIndoorLights() {
	setLastActivatedIndoor("off");
    def sw = indoorSwitch;
    if (sw) {
        def switchValue = sw.currentValue("switch");
        if (switchValue != "off") { sw?.off(); }
    }
    indoorLights?.off();
}

def turnOnOutdoorLights() {
	setLastActivatedOutdoor("on");
    def sw = outdoorSwitch;
    if (sw) {
        def switchValue = sw.currentValue("switch");
        if (switchValue != "off") { sw?.off(); }
    }
    outdoorLights?.on();
}

def turnOffOutdoorLights() {
	setLastActivatedOutdoor("off");
    def sw = outdoorSwitch;
    if (sw) {
        def switchValue = sw.currentValue("switch");
        if (switchValue != "off") { sw?.off(); }
    }
    outdoorLights?.off();
}