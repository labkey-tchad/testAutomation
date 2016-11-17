/*
 * Copyright (c) 2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.ext4;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.util.Ext4Helper.Locators.comboListItem;
import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.EXACT;
import static org.labkey.test.util.Ext4Helper.getCssPrefix;

public class ComboBox extends WebDriverComponent<ComboBox.ElementCache>
{
    WebElement _formItem;
    WebDriverWrapper _driverWrapper;

    private ComboBox(WebElement formItem, WebDriver driver)
    {
        _formItem = formItem;
        _driverWrapper = new WebDriverWrapperImpl(driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _formItem;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driverWrapper.getDriver();
    }

    public WebDriverWrapper getDriverWrapper()
    {
        return _driverWrapper;
    }

    public interface ComboListMatcher
    {
        Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String... selections)
    {
        selectComboBoxItem(EXACT, selections);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(ComboListMatcher matchTechnique, @LoggedParam String... selections)
    {
        openComboList();

        try
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }

        closeComboList();
    }

    public void openComboList()
    {
        elementCache().arrowTrigger.click();

        try
        {
            WebDriverWrapper.waitFor(() -> getComponentElement().getAttribute("class").contains("pickerfield-open"), 1000);
            getDriverWrapper().waitForElement(comboListItem());
        }
        catch (TimeoutException | NoSuchElementException retry)
        {
            elementCache().arrowTrigger.click(); // try again if combo-box doesn't open
        }

        getDriverWrapper().waitForElement(comboListItem());
    }

    public void selectItemFromOpenComboList(String itemText, ComboListMatcher matchTechnique, boolean clickAt)
    {
        selectItemFromOpenComboList(getDriverWrapper(), itemText, matchTechnique, clickAt);
    }

    public static void selectItemFromOpenComboList(WebDriverWrapper driverWrapper, String itemText, ComboListMatcher matchTechnique, boolean clickAt)
    {
        Locator.XPathLocator listItem = matchTechnique.getLocator(comboListItem(), itemText);

        WebElement element = listItem.waitForElement(driverWrapper.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
        if (!isOpenComboBoxMultiSelect(driverWrapper.getDriver()) || !elementAlreadySelected)
        {
            driverWrapper.scrollIntoView(element); // Workaround: Auto-scrolling in chrome isn't working well
            if(clickAt)
                driverWrapper.clickAt(listItem, 0, 0, 0);
            else
                driverWrapper.click(listItem);
        }
    }

    public void selectItemFromOpenComboList(String itemText, ComboListMatcher matchTechnique)
    {
        selectItemFromOpenComboList(itemText, matchTechnique, false);
    }

    public static void selectItemFromOpenComboList(WebDriverWrapper driverWrapper, String itemText, ComboListMatcher matchTechnique)
    {
        selectItemFromOpenComboList(driverWrapper, itemText, matchTechnique,false);
    }

    private boolean isOpenComboBoxMultiSelect()
    {
        return isOpenComboBoxMultiSelect(getDriver());
    }

    private static boolean isOpenComboBoxMultiSelect(WebDriver driver)
    {
        return !comboListItem().append("/span").withClass(getCssPrefix() + "combo-checker").findElements(driver).isEmpty();
    }

    public void closeComboList()
    {
        // close combo manually if it is a multi-select combo-box
        if (isOpenComboBoxMultiSelect())
            elementCache().arrowTrigger.click();

        // menu should disappear
        getDriverWrapper().waitForElementToDisappear(comboListItem());
    }

    public void clearComboBox()
    {
        openComboList();

        try
        {
            for (WebElement element : comboListItem().findElements(getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (WebElement element : comboListItem().findElements(getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }

        closeComboList();
    }

    @LogMethod(quiet=true)
    public List<String> getComboBoxOptions()
    {
        openComboList();
        return getDriverWrapper().getTexts(comboListItem().findElements(getDriver()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        protected WebElement arrowTrigger = Locator.xpath("//div[contains(@class,'arrow')]").findWhenNeeded(this);
    }

    public static ComboBoxFinder ComboBox(WebDriver driver)
    {
        return new ComboBoxFinder(driver);
    }

    public static class ComboBoxFinder extends FormItemFinder<ComboBox, ComboBoxFinder>
    {
        WebDriver _driver;

        public ComboBoxFinder(WebDriver driver)
        {
            super();
            _driver = driver;
        }

        @Override
        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("td", getCssPrefix() + "form-item-body").attributeStartsWith("id", "combobox");
        }

        @Override
        protected ComboBox construct(WebElement el)
        {
            return new ComboBox(el, _driver);
        }
    }
}
