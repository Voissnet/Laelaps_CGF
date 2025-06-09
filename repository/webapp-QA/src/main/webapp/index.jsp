<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="es_CL" />
<fmt:setBundle basename="net.redvoiss.i18n.text" />

<html>
    <head>
        <title><fmt:message key="net.redvoiss.sms.landing_page.title" /></title>
        <meta http-equiv="refresh" content="3;url=protected">
    </head>
    <body>
        <h1><fmt:message key="net.redvoiss.sms.landing_page.header" /></h1>
        <fmt:message key="net.redvoiss.sms.landing_page.message" />
    </body>
</html>