/**
 *  Always On
 *
 *  Copyright 2019 Paul Smith
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
    name: "Always On",
    namespace: "paulsmithkc",
    author: "Paul Smith",
    description: "Keep a light on all the time",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png")


preferences {
	section("Setup") {
		input "lightSwitch", "capability.switch", multiple: false, required: false, title: "What switch do you want to keep on?"
	}
    section(mobileOnly: true, "Name") {
        label title: "Do you want to name this app?", required: false
        //icon title: "Do you want to give this app a special icon?", required: false
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
	if (lightSwitch) {
        subscribe(lightSwitch, "switch", switchHandler);
        
        def switchValue = lightSwitch.currentValue("switch");
    	if (switchValue != "on") {
        	log.debug("turning light on");
        	lightSwitch?.on(); 
        }
    }
}

def switchHandler(evt) {
    if (evt.value != "on") {
    	log.debug("turning light on");
        lightSwitch?.on();
    }
}