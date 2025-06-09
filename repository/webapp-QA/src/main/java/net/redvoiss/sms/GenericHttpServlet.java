package net.redvoiss.sms;
 
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet; 
import javax.servlet.http.HttpServletRequest; 
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher; 
import javax.servlet.ServletException;

import java.io.IOException;

public abstract class GenericHttpServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;
    private static final Logger LOGGER = Logger.getLogger(GenericHttpServlet.class.getName());

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ( request.getSession(false) == null ) {
            LOGGER.warning("Session is missing");
            RequestDispatcher rd = request.getRequestDispatcher("/protected/index.jsp");
            rd.forward(request, response);
        } else {
            LOGGER.fine("Session is valid");
            super.service(request, response);
        }
    }

}