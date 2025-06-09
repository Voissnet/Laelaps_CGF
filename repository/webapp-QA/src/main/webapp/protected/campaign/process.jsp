<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="static net.redvoiss.sms.upload.CampaignUploader.DATE_FORMAT" %>
<%@ page import="static net.redvoiss.sms.upload.CampaignUploader.COMMERCIAL_VALUE" %>
<%@ page import="static net.redvoiss.sms.upload.CampaignUploader.REPLY_VALUE" %>
<%@include file="../header.inc"%>

<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="es_CL" />
<fmt:setBundle basename="net.redvoiss.i18n.text" />
<html>
    <head> 
        <meta http-equiv="Pragma" content="no-cache">
        <title><fmt:message key="net.redvoiss.sms.upload.title" /></title>
        <script>
            "use strict";
            //http://zinoui.com/blog/ajax-request-progress-bar
            //http://stackoverflow.com/questions/18592679/xmlhttprequest-to-post-html-form
            //http://stackoverflow.com/questions/18310038/xmlhttprequest-upload-onprogress-instantly-complete
            function submitProcessForm(oFormElement)
            {
                oFormElement.submit();
            }

            function submitUploadForm(oFormElement)
            {
                console.log("About to show progress");
                var progressBar = document.getElementById("progress");
                var xhr = new XMLHttpRequest();
                xhr.onload = function () {
                    document.write(xhr.responseText);
                    document.close();
                };
                xhr.upload.onprogress = function (e) {
                    if (e.lengthComputable) {
                        progressBar.max = e.total;
                        progressBar.value = e.loaded;
                    }
                };
                xhr.upload.onloadstart = function (e) {
                    progressBar.value = 0;
                };
                xhr.upload.onloadend = function (e) {
                    progressBar.value = e.loaded;
                };
                xhr.open(oFormElement.method, oFormElement.action, true);
                xhr.send(new FormData(oFormElement));
                return false;
            }
        </script>
    </head>
    <body>
        <div>
            <form action="upload" enctype="multipart/form-data" method="post" >
                <div>
                    <input type="file" name="uploadedFile" />
                    <input type="button" onclick="this.disabled = true; submitUploadForm(this.form);" value="<fmt:message key="net.redvoiss.sms.upload.submit.label" />"/>
                    <progress id="progress" value="0"></progress>
                </div>
            </form>
        </div>
        <c:if test="${not empty uploadedCampaigns}">
            <div>
                <form action="process" enctype="multipart/form-data" method="post" >
                    <div>
                        <c:forEach items="${uploadedCampaigns}" var="campaign">
                            <input type="radio" name="campaign" id="${campaign.hash}" value="${campaign.hash}" />
                            <label for="${campaign.hash}"><c:out value="${campaign.name}" /> [<c:out value="${campaign.size}" />b]</label>
                            <br/>
                        </c:forEach>
                    </div>
                    <div>
                        <select name="encoding">
                            <option value="ISO-8859-1">ISO-8859-1</option>
                            <option selected="true" value="UTF-8">UTF-8</option>
                        </select>
                    </div>
                    <div>
                        <input type="checkbox" id="commercial" name="commercial" value="<%=COMMERCIAL_VALUE%>"/>
                        <label for="commercial"><fmt:message key="net.redvoiss.sms.store.commercial" /> </label>
                    </div>
                    <div>
                        <input type="checkbox" id="reply" name="reply" value="<%=REPLY_VALUE%>"/>
                        <label for="reply"><fmt:message key="net.redvoiss.sms.store.reply" /> </label>
                    </div>
                    <c:set var = "now" value="<%=new java.util.Date()%>" />
                    <div>
                        <input type="datetime-local" name="date" value="<fmt:formatDate pattern = "<%=DATE_FORMAT%>" value = "${now}" />"/>
                    </div>
                    <div>
                        <input type="button" onclick="this.disabled = true; submitProcessForm(this.form);" value="<fmt:message key="net.redvoiss.sms.process.submit.label" />"/>
                    </div>
                </form>
            </div>
        </c:if>
        <%@include file="errors.inc"%>
        <hr>
        <p><a href="<c:url value="/protected"/>"> <fmt:message key="net.redvoiss.sms.home" /> </a>
    </body> 
</html>