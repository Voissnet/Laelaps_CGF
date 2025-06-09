package net.redvoiss.sms.smpp;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ShortMessageDBStoreTest {
    @Test
    public void testIncomingDestination() throws Exception {
        assertEquals( "56asdkasdl9340234215310askdjalsd215310",
            ShortMessageDBStore.incomingMovistarDestination( "215310asdkasdl9340234215310askdjalsd215310" ));
        assertEquals( "56asdka220310342153220310djalsd215310",
            ShortMessageDBStore.incomingEntelDestination( "220310asdka220310342153220310djalsd215310" ));
        assertEquals( "56431asdka220310342153220310djalsd215310",
            ShortMessageDBStore.incomingEntelDestination( "22031asdka220310342153220310djalsd215310" ));
        assertEquals( "56asd2353100234215235310alsd215310",
            ShortMessageDBStore.incomingVTRDestination( "235310asd2353100234215235310alsd215310" ));
    }
}