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
        <h1><fmt:message key="net.redvoiss.sms.login.header" /></h1>
        <form method="POST" action="j_security_check">
            <table align="center">
                <tr>
                    <td colspan="2"> <fmt:message key="net.redvoiss.sms.login.title" /> </td>
                </tr>
                <tr>
                    <td><fmt:message key="net.redvoiss.sms.login.name" /> </td>
                    <td><input type="text" name="j_username" /></td>
                </tr>
                <tr>
                    <td><fmt:message key="net.redvoiss.sms.login.password" /> </td>
                    <td><input type="password" name="j_password" /></td>
                </tr>
                <tr>
                    <td colspan="2"><input type="submit" value="<fmt:message key="net.redvoiss.sms.login.enter" /> " /></td>
                </tr>
            </table>
        </form>
    </body>
</html>