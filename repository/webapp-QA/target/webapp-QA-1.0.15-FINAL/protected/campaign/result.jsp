<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="es_CL" />
<fmt:setBundle basename="net.redvoiss.i18n.text" />

<html>
    <head>
        <meta http-equiv="Pragma" content="no-cache">
        <title> <fmt:message key="net.redvoiss.sms.upload.result_page.title" /> </title>
        <script>
            "use strict";
            var refreshIntervalId = window.setInterval(function () {
                var xhr = new XMLHttpRequest();
                xhr.onreadystatechange = function () {
                    if (xhr.readyState === 4 && xhr.status === 200) {
                        var response = JSON.parse(xhr.responseText);
                        if (response.action === 'desist') {
                            window.clearInterval(refreshIntervalId);
                        } else {
                            var progressDiv = document.getElementById('progress');
                            progressDiv.innerHTML = response.progress;
                            for (var index = 0; index < response.elements.length; ++index) {
                                var message = response.elements[index];                                
                                if (message.text === null) {
                                    console.log('No info');
                                } else {
                                    var messagesDiv = document.getElementById('messages');
                                    messagesDiv.innerHTML += message.text;
                                }
                            }
                        }
                    }
                };
                xhr.open("GET", "process?taskId=<c:out value='${requestScope.taskId}'/>", true);
                xhr.send(null);
            }, 3000);
        </script>
    </head>
    <body>
        <h1> <fmt:message key="net.redvoiss.sms.upload.result_page.header" /> </h1>
        <p><label for="progress"><fmt:message key="net.redvoiss.sms.process.progress" /></label><div id="progress"></div></p>
    <p><label for="messages"><fmt:message key="net.redvoiss.sms.process.messages" /></label><div id="messages"></div></p>
<hr>
<p><a href="<c:url value="/protected/campaign/process.jsp"/>"> <fmt:message key="net.redvoiss.sms.upload.new" /> </a>
<p><a href="<c:url value="/protected"/>"> <fmt:message key="net.redvoiss.sms.home" /> </a>
</body>
</html>