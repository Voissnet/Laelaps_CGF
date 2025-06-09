package net.redvoiss.jsp.custom.taglib;
 
import java.io.IOException;
 
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
 
public class SubmitTagHandler extends TagSupport {
    private String m_input;
     
    @Override
    public int doStartTag() throws JspException {         
        try {
            JspWriter out = pageContext.getOut();
 
            //Perform substr operation on string.
            out.println("<input type=\"submit\" value=\"%s\" onclick=\"this.disabled=true; this.form.submit();\"/>");
 
        } catch (IOException e) {
            e.printStackTrace();
        }
        return SKIP_BODY;
    }
    public String getInput() {
        return m_input;
    }
    public void setInput(String input) {
        m_input = input;
    }
}