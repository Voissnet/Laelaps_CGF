package net.redvoiss.sms.upload;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.regex.Matcher;

public class CampaignUploaderTest {
	
	@Test
	public void validateContentIncludingComma()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("56950145373,Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310");
        assertTrue(contentMatcher.matches());
        assertEquals(2, contentMatcher.groupCount());
        final String destination = contentMatcher.group(1);
        final String message = contentMatcher.group(2);
        System.out.println(String.format("Destination is {%s}. Message is {%s}", destination, message ));
    }
    
    @Test
	public void validateContentInludingSemiColon()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("56950145373;Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310");
        assertTrue(contentMatcher.matches());
        assertEquals(2, contentMatcher.groupCount());
        final String destination = contentMatcher.group(1);
        final String message = contentMatcher.group(2);
        System.out.println(String.format("Destination is {%s}. Message is {%s}", destination, message ));
    }
    
    @Test
	public void validateContentShorterNumberSemiColon()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("5695014537;Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310");
        assertFalse(contentMatcher.matches());       
    }
    
    @Test
	public void validateContentLargerMessageSemiColon()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("56950145373;Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310 -------");
        assertFalse(contentMatcher.matches());       
    }
    
    @Test
	public void validateContentShorterNumberComma()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("5695014537,Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310");
        assertFalse(contentMatcher.matches());       
    }
    
    @Test
	public void validateContentLargerMessageComma()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("56950145373,Estimado Cliente recuerde que usted realizo un compromiso de pago para el 12/01.pague en www.clarochile.cl - centros atencion Claro o llame al 0227134310 -------");
        assertFalse(contentMatcher.matches());       
    }
    
    @Test
	public void validateQuote()  {
        Matcher contentMatcher = CampaignUploader.CONTENT_PATTERN.matcher("56999499457;Informamos de Clínica Vespucio que su cuenta está morosa y debe ser cancelada. Contáctenos al WhatsApp +56941547076. Si ha regularizado su \"");
        assertTrue(contentMatcher.matches());       
    }
    
}