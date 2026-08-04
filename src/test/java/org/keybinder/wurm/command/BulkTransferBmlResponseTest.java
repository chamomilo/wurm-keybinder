package org.keybinder.wurm.command;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class BulkTransferBmlResponseTest {
    private static final String FORM =
            "border{center{text{type='bold';text=\"How many?\"}};null;scroll{"
                    + "varray{passthrough{id=\"id\";text=\"4711\"};"
                    + "text{text=\"How many items do you wish to remove?\"};"
                    + "input{text='';id='numstext';maxlength='2'};"
                    + "radio{ group='items'; id='1';text='1'};"
                    + "button{text='Send';id='submit'}}}}";

    @Test public void recognizesInstalledRemoveItemQuestionAndBuildsOneAnswer() {
        BulkTransferBmlResponse response =
                BulkTransferBmlResponse.parse("Removing items", FORM);

        assertNotNull(response);
        assertEquals("4711", response.getQuestionId());
        Map<String, String> fields = response.fieldsForOneItem();
        assertEquals("4711", fields.get("id"));
        assertEquals("1", fields.get("numstext"));
        assertEquals("1", fields.get("items"));
    }

    @Test public void writesRequestedQuantityIntoServerForm() {
        BulkTransferBmlResponse response =
                BulkTransferBmlResponse.parse("Removing items", FORM);

        Map<String, String> fields = response.fieldsForQuantity(43);
        assertEquals("43", fields.get("numstext"));
        assertEquals("1", fields.get("items"));
    }

    @Test public void refusesUnrelatedOrStructurallyDifferentBml() {
        assertNull(BulkTransferBmlResponse.parse("Village application", FORM));
        assertNull(BulkTransferBmlResponse.parse("Removing items",
                FORM.replace("numstext", "amount")));
        assertNull(BulkTransferBmlResponse.parse("Removing items",
                FORM.replace("group='items'", "group='choices'")));
    }
}
