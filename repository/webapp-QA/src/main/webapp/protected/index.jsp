<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="es_CL" />
<fmt:setBundle basename="net.redvoiss.i18n.text" />

<html>
    <head>
        <title><fmt:message key="net.redvoiss.sms.title" /></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    </head>
    <body>
        <h1><fmt:message key="net.redvoiss.sms.header" /></h1>
        <p> <a href="<c:url value="/protected/campaign/process.jsp"/>"> <fmt:message key="net.redvoiss.sms.campaign.start" /> </a></p>
        <hr/>
        <p align="right">
            <c:if test="${not empty pageContext.request.remoteUser}">
                <table border="0">
                    <tr>
                        <td>
                            <fmt:message key="net.redvoiss.sms.landing_page.user" > 
                                <fmt:param value="${pageContext.request.remoteUser}"/>
                            </fmt:message>
                        </td>
                        <td>
                            (<a href="<c:url value="/protected/logout.jsp"/>"> <fmt:message key="net.redvoiss.sms.landing_page.logout" /> </a>)
                        </td>
                    </tr>
                </table>
            </c:if>
        </p>
    </body>
</html>
