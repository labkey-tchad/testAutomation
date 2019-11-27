package org.labkey.test.components.domain;

import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;

public class UnsavedChangesModalDialog extends ModalDialog
{

    public UnsavedChangesModalDialog(ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
    }

    public DomainDesignerPage discardChanges()  // should land the user on a domain designer page
    {
        dismiss("No, Discard Changes");
        return new DomainDesignerPage(getDriver());
    }

    public void saveChanges()   // if successful, should navigate away with same effect as clicking 'save' on domain designer page
    {
        dismiss("Yes, Save Changes");
    }

}