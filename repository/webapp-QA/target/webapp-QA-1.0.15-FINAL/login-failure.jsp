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
        <h1><fmt:message key="net.redvoiss.sms.landing_page.header" /></h1>
        <p>
            <fmt:message key="net.redvoiss.sms.login.failure" />
        </p>
        <p>
            <a href="<c:url value="/"/>"> <fmt:message key="net.redvoiss.sms.login.failure.back" /> </a>
        </p>
    </body>
</html>