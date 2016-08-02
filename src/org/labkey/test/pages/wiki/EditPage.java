package org.labkey.test.pages.wiki;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Collections;

import static org.labkey.test.components.html.EnumSelect.EnumSelect;
import static org.labkey.test.util.WikiHelper.WikiRendererType;

/**
 * TODO: Very incomplete
 */
public class EditPage extends LabKeyPage<EditPage.ElementCache>
{
    public EditPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static EditPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("wiki", containerPath, "edit"));
        return new EditPage(driver.getDriver());
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     */
    public void convertWikiFormat(WikiRendererType format)
    {
        if (getWikiFormat() == format)
            return;

        elementCache().convertButton.click();

        ChangeFormatWindow changeFormatWindow = new ChangeFormatWindow();
        changeFormatWindow.format().set(format);
        changeFormatWindow.clickConvert();
        WebElement status = waitForElement(Locator.id("status").containing("Converted."));
        shortWait().until(ExpectedConditions.invisibilityOfAllElements(Collections.singletonList(status)));
    }

    public WikiRendererType getWikiFormat()
    {
        return WikiRendererType.valueOf((String) executeScript("return LABKEY._wiki.getProps().rendererType;"));
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement convertButton = Locator.lkButton("Convert To...").findWhenNeeded(this);
    }

    public class ChangeFormatWindow extends Window
    {
        private final EnumSelect<WikiRendererType> _formatSelect = EnumSelect(Locator.id("wiki-input-window-change-format-to"), WikiRendererType.class).findWhenNeeded(this);

        public ChangeFormatWindow()
        {
            super("Change Format", getDriver());
        }

        public EnumSelect<WikiRendererType> format()
        {
            return _formatSelect;
        }

        public void clickConvert()
        {
            clickButton("Convert", true);
        }

        public void clickCancel()
        {
            clickButton("Cancel", true);
        }
    }
}