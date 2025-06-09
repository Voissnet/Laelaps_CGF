<fmt:setBundle basename="net.redvoiss.sms.upload.messages" var="e"/>

<c:if test="${!empty report}">
<ul>
<c:forEach items="${report.messages}" var="message">
    <c:choose>
        <c:when test="${message.error}">
            <c:choose>
            <c:when test="${empty message.arguments}">
                <li style="color:red;"><fmt:message key="${message.key}" bundle="${e}"/> 
            </c:when>
            <c:otherwise>
                <li style="color:red;">
                <fmt:message key="${message.key}" bundle="${e}">
                    <c:forEach items="${message.arguments}" var="detail">
                        <fmt:param value="${detail}"/>
                    </c:forEach>
                </fmt:message>
            </c:otherwise>
            </c:choose>
        </c:when>    
        <c:otherwise>        
            <c:choose>
            <c:when test="${empty message.arguments}">
                <li><fmt:message key="${message.key}" bundle="${e}"/> 
            </c:when>
            <c:otherwise>
                <li>
                <fmt:message key="${message.key}" bundle="${e}">
                    <c:forEach items="${message.arguments}" var="detail">
                        <fmt:param value="${detail}"/>
                    </c:forEach>
                </fmt:message>
            </c:otherwise>
            </c:choose>            
        </c:otherwise>
    </c:choose>            
</c:forEach>
<c:choose>
<c:when test="${report.erroneus}">
    <li style="color:red;"><b> <fmt:message key="net.redvoiss.sms.upload.file.overall.error" /> </b> 
</c:when>
<c:otherwise>
    <li><b> <fmt:message key="net.redvoiss.sms.upload.file.overall.ok" /> </b>
</c:otherwise>
</c:choose> 
<c:remove var="report"/>
</ul>
</c:if>