const SocketServer = require('websocket').server
const http = require('http')

const GET_CONNECTIONS = 'get_connections'
const GET_ENCRYPTING_METHOD = 'get_encrypting_method'
const SET_SERVER_ENCRYPTING_METHOD = 'set_encrypting_method'
const SET_ENCRYPTING_METHOD = 'set_encrypting_method'

const server = http.createServer((req, res) => {})

server.listen(3000, ()=>{
    console.log("Listening on port 3000...")
})

var webSocketServer = new SocketServer({httpServer:server})

const connections = []
const connections_data = []

var server_encrypting_method = '-1'
var number_of_connections = 0

webSocketServer.on('request', (req) => {
    if(number_of_connections <= 1) {
        const connection = req.accept()

        console.log('own| New connection')

        var connection_dict = {'connection': connection}
        connections.push(connection)
        connections_data.push(connection_dict)
        number_of_connections += 1;

        resendKeys()
    
        connection.on('message', (json_message) => {
            var jsonObject = JSON.parse(json_message.utf8Data)
            
            if(jsonObject.hasOwnProperty('task')){
                if (jsonObject.task == GET_ENCRYPTING_METHOD) {
                    console.log('in< Get_encrypting_method')
                    sendEncryptingMethod(jsonObject, connection);

                } else if (jsonObject.task == SET_ENCRYPTING_METHOD) {
                    console.log('in< Set_encrypting_method')
                    saveKeys(jsonObject, connection)
                    resendKeys()
                    
                } else if (jsonObject.task == SET_SERVER_ENCRYPTING_METHOD) {
                    console.log('in< Set_server_encrypting_method')
                    setEncryptingMethod(jsonObject.encrypting_method)
                }

            } else {
                console.log('in< MESSAGE:'+json_message.utf8Data.message)
                printValues()

                connections.forEach(element => {
                    if (element != connection)
                        element.sendUTF(json_message.utf8Data)
                })
            }
        })
    
        connection.on('close', (resCode, des) => {
            console.log('own| connection closed')
            clearConnectionData(connection)
            number_of_connections -= 1;
            if(number_of_connections <= 0) {
                clearEncryptingMethod()
            }
        })    
    }
})

function clearConnectionData(connection) {

    connections.splice(connections.indexOf(connection), 1)
    for (var i=0; i < connections_data.length; i++) {
        if(connections_data[i].connection = connection) {
            connections_data.splice(i)
        }    
    }
    console.log('own| Removed connection')
}

function saveKeys(jsonObject, connection) {

    connections_data.forEach(connection_dict => {
        if (connection_dict['connection'] == connection) {
            if(server_encrypting_method == '-1') {
                setEncryptingMethod(jsonObject.encrypting_method)
            }

            if(jsonObject.encrypting_method == '0') {
                console.log('   public_key: '+jsonObject.public_key)
        
                connection_dict.encrypting_method = jsonObject.encrypting_method
                connection_dict.public_key = jsonObject.public_key
        
            } else if(jsonObject.encrypting_method == '1') {
                console.log('   public_p: '+jsonObject.public_p)
                console.log('   public_b: '+jsonObject.public_b)
                console.log('   public_g: '+jsonObject.public_g)
        
                connection_dict.encrypting_method = jsonObject.encrypting_method
                connection_dict.public_p = jsonObject.public_p
                connection_dict.public_b = jsonObject.public_b
                connection_dict.public_g = jsonObject.public_g
            }
        }
        console.log('own| Saveing Encrypting')
    })
}

function printValues() {
    console.log("   sem:"+server_encrypting_method)
    console.log("   noc:"+number_of_connections)

    connections_data.forEach(connection_dict => {
        console.log("   "+connection_dict.encrypting_method)
        if(connection_dict.encrypting_method == "0") {
            console.log("   "+connection_dict.public_key)
        } else {
            console.log('   public_p: '+connection_dict.public_p)
            console.log('   public_b: '+connection_dict.public_b)
            console.log('   public_g: '+connection_dict.public_g)
        }
    })
}

function setEncryptingMethod(encrypting_method) {
    console.log('own| Setting Encrypting: '+encrypting_method)
    server_encrypting_method = encrypting_method
}

function clearEncryptingMethod() {
    console.log('own| Clearing Encrypting')
    printValues()
    server_encrypting_method = '-1'
}

function sendEncryptingMethod(jsonObject, connection) {
    jsonObject.encrypting_method = server_encrypting_method
    console.log('out> Sending Enrypting')
    connection.sendUTF(JSON.stringify(jsonObject))
}

function resendKeys() {
    connections_data.forEach(connection_dict => {
        sendKeys(connection_dict['connection'])
    })
}

function sendKeys(connection) {
    connections_data.forEach(connection_dict => {
        if (connection_dict['connection'] != connection) {
            var keysDict = {};
            
            if(connection_dict.encrypting_method == "0") {
                keysDict = {task: GET_CONNECTIONS, encrypting_method: connection_dict.encrypting_method,
                    public_key: connection_dict.public_key}
            } else {
                keysDict = {task: GET_CONNECTIONS, encrypting_method: connection_dict.encrypting_method,
                    public_p: connection_dict.public_p, public_b: connection_dict.public_b, public_g: connection_dict.public_g}
            }

            var jsonObject = JSON.stringify(keysDict)
            connection.sendUTF(jsonObject);
            console.log('out> Sending Keys and Connections')
        }
    })
}
