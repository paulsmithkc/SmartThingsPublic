/**
 *  Switchboard
 *
 *  Copyright 2018 Paul Smith
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
    name: "Switchboard",
    namespace: "paulsmithkc",
    author: "Paul Smith",
    description: "Provides web services that support an Arduino based switchboard.",
    category: "Mode Magic",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home30-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png")

mappings {
	path("/switches") {
    	action: [ GET: "listSwitches" ]
    }
    path("/switches/:command") {
    	action: [ PUT: "updateSwitches" ]
    }
}

preferences {
	section ("Allow external service to control these things...") {
		input "switches", "capability.switch", multiple: true, required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
    def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}

def updateSwitches() {
    def command = params.command
    switch(command) {
        case "on":
            switches.on()
            break
        case "off":
            switches.off()
            break
        case "toggle":
        	switches.each {
      			def val = it.currentValue("switch")
                switch(val) {
                	case "on":
                        it.off()
                        break
                    case "off":
                        it.on()
                        break
                }
    		}
            break
        default:
            httpError(400, "$command is not a valid command for switches")
    }
}