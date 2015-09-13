<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
    <title>STOMP Test Using Spring 4 WebSocket</title>
    <script src="<c:url value="/res/javascript/lib/stomp.js"/>" type="text/javascript"></script>
    <script type="text/javascript">
        var stompClient = null; 
        function setConnected(connected) {
            document.getElementById('connect').disabled = connected;
            document.getElementById('disconnect').disabled = !connected;
            document.getElementById('calculationDiv').style.visibility = connected ? 'visible' : 'hidden';
            document.getElementById('calResponse').innerHTML = '';
        }
        function connect() {
        	var uri = "ws://localhost:8080/GMM/chat";
            var socket = new WebSocket(uri);
			stompClient = Stomp.over(socket);
            stompClient.connect({}, function(frame) {
                setConnected(true);
                console.log('Connected: ' + frame);
                stompClient.subscribe('/user/queue/echoResult', function(answer){
                	showResult(answer.body);
                });
                stompClient.subscribe('/topic/allResult', function(answer){
                	showResult(answer.body);
                });
            });
        }
        function disconnect() {
            stompClient.disconnect();
            setConnected(false);
            console.log("Disconnected");
        }
        function sendEcho() {
            var chatInput = document.getElementById('num1').value;
            stompClient.send("/app/chat/echo", {}, chatInput);
        }
        function sendAll() {
            var chatInput = document.getElementById('num1').value;
            stompClient.send("/app/chat/all", {}, chatInput);
        }
        function showResult(message) {
            var response = document.getElementById('calResponse');
            var p = document.createElement('p');
            p.style.wordWrap = 'break-word';
            p.appendChild(document.createTextNode(message));
            response.appendChild(p);
        }
    </script>
</head>
<body>
<noscript><h2>Enable Java script and reload this page to run Websocket Demo</h2></noscript>
<h1>Calculator App Using Spring 4 WebSocket</h1>
<div>
    <div>
        <button id="connect" onclick="connect();">Connect</button>
        <button id="disconnect" disabled="disabled" onclick="disconnect();">Disconnect</button><br/><br/>
    </div>
    <div id="calculationDiv">
        <label>Message:</label><input type="text" id="num1" /><br/>
        <button id="sendEcho" onclick="sendEcho();">Echo</button><br/>
        <button id="sendAll" onclick="sendAll();">ToAll</button>
        <p id="calResponse"></p>
    </div>
</div>
</body>
</html> 