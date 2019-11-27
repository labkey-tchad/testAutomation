/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.glassLibrary.components.MultiMenu;
import org.labkey.test.components.glassLibrary.components.OmniBox;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

public class GridBar extends WebDriverComponent<GridBar.ElementCache>
{
    final private WebElement _gridBarElement;
    final private WebDriver _driver;
    final private ResponsiveGrid _responsiveGrid;

    protected GridBar(WebElement buttonBar, ResponsiveGrid responsiveGrid, WebDriver driver)
    {
        _gridBarElement = buttonBar;
        _responsiveGrid = responsiveGrid;  // The responsive grid that is associated with this bar.
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _gridBarElement;
    }

    public File exportData(ExportType exportType)
    {

        WebElement downloadBtn = Locator.tagWithClass("span", "fa-download").findElement(this);

        if(!downloadBtn.isDisplayed())
            throw new ElementNotVisibleException("File export button is not visible.");

        downloadBtn.click();

        WebElement exportButton = Locator.css("li > a > span").withClass(exportType.buttonCssClass()).findElement(this);
        return getWrapper().doAndWaitForDownload(()->exportButton.click());
    }

    public int getRecordCount()
    {
        if(getWrapper().isElementPresent(Locators.pagingCountsSpan) && getWrapper().isElementVisible(Locators.pagingCountsSpan))
        {
            return Integer.parseInt(Locators.pagingCountsSpan.findElement(getDriver()).getAttribute("data-total"));
        }

        // return the number of rows present
        return _responsiveGrid.getRows().size();
    }

    public boolean isOnFirstPage()
    {

        try
        {
            return !Locators.pgLeftButton.findElement(getDriver()).isEnabled();
        }
        catch(NoSuchElementException nse)
        {
            // If there is no left button we are on the first/last/only page.
            return true;
        }
    }

    public boolean isOnLastPage()
    {
        try
        {
        return !Locators.pgRightButton.findElement(getDriver()).isEnabled();
        }
        catch(NoSuchElementException nse)
        {
            // If there is no right button we are on the first/last/only page.
            return true;
        }
    }

    // Should this really return a grid object?
    public ResponsiveGrid getNextPage()
    {
        WebElement nextButton = Locators.pgRightButton.waitForElement(getDriver(), 2500);
        if (nextButton.isEnabled())
        {
            _responsiveGrid.doAndWaitForUpdate(()->nextButton.click());
        }

        return _responsiveGrid;
    }

    // Should this really return a grid object?
    public ResponsiveGrid getPreviousPage()
    {
        WebElement prevButton = Locators.pgLeftButton.waitForElement(getDriver(), 2500);
        if (prevButton.isEnabled())
        {
            _responsiveGrid.doAndWaitForUpdate(()->prevButton.click());
        }
        return _responsiveGrid;
    }

    public GridBar selectAllInSet(boolean checked)
    {
        _responsiveGrid.selectAllOnPage(checked);
        if (checked)
        {
            Locator selectBtn = Locator.xpath("//button[contains(text(), 'Select all')]");      // Select all n
            Locator selectedText = Locator.xpath("//span[contains(text(),'Selected all ')]");   // Selected all n on this page
            Locator allSelected = Locator.xpath("//span[contains(text(), 'All ')]");            // All n selected
            WebElement btn = selectBtn.waitForElement(this, 4000);
            btn.click();

            getWrapper().waitFor(() -> allSelected.findOptionalElement(this).isPresent() ||
                            selectBtn.findOptionalElement(this).isEmpty() &&
                                    selectedText.findOptionalElement(this).isPresent() ,
                    WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public OmniBox getOmniBox()
    {
        return elementCache().omniBox;
    }

    public void doMenuAction(String buttonText, List<String> menuActions)
    {
        MultiMenu multiMenu = null;
        boolean found = false;
        int tries = 1;

        // Sometimes the grid and query bar will load, and even the menu button will render but the text will
        // take just a few ms to render, so if at first you don't succeed try again.
        while(!found && tries <= 3)
        {
            try
            {
                multiMenu = elementCache().findMenu(buttonText);
                found = true;
            }
            catch (NoSuchElementException nse)
            {
                getWrapper().log("Couldn't find menu button with caption '" + buttonText + "', trying again.");
                tries++;
                sleep(500);
            }
        }

        if(found)
        {
            multiMenu.doMenuAction(menuActions);
        }
        else
        {
            throw new NoSuchElementException("Couldn't find menu button with caption '" + buttonText + "'.");
        }
    }

    public List<String> getMenuButtonsText()
    {
        // Because this is not a search for a specific menu button let's pause for a moment to give the buttons a
        // chance to render if they haven't done so already.
        sleep(1_500);

        List<MultiMenu> menuButtons = new MultiMenu.MultiMenuFinder(getDriver()).findAll(this);
        List<String> menuButtonText = new ArrayList<>();

        for(MultiMenu multiMenu : menuButtons)
        {
            menuButtonText.add(multiMenu.getButtonText());
        }

        return menuButtonText;
    }

    public List<String> getMenuText(String buttonText)
    {
        MultiMenu multiMenu = null;
        boolean found = false;
        int tries = 1;

        // Sometimes the grid and query bar will load, and even the menu button will render but the text will
        // take just a few ms to render, so if at first you don't succeed try again.
        while(!found && tries <= 3)
        {
            try
            {
                multiMenu = elementCache().findMenu(buttonText);
                found = true;
            }
            catch (NoSuchElementException nse)
            {
                getWrapper().log("Couldn't find menu button with caption '" + buttonText + "', trying again.");
                tries++;
                sleep(500);
            }
        }

        if(found)
            return multiMenu.getMenuText();

        throw new NoSuchElementException("Couldn't find menu button with caption '" + buttonText + "'.");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        public OmniBox omniBox = new OmniBox.OmniBoxFinder(_driver).findWhenNeeded(this);
        private final Map<String, MultiMenu> menus = new HashMap<>();
        protected MultiMenu findMenu(String buttonText)
        {
            if (!menus.containsKey(buttonText))
                menus.put(buttonText, new MultiMenu.MultiMenuFinder(_driver).withText(buttonText).find(this));

            return menus.get(buttonText);
        }
    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator gridBar()
        {
            return Locator.xpath("//div[contains(@class, 'table-responsive')]/ancestor::div[@class='panel-body']//div[contains(@class,'hidden-md')]/parent::div");
        }

        static public Locator.XPathLocator queryGridBar()
        {
            return Locator.xpath("//div[contains(@class, 'table-responsive')]/ancestor::div[@class='panel-body']//div[@class='query-grid-bar']");
        }

        static public Locator.XPathLocator gridBar(String gridId)
        {
            return Locator.xpath("//div[contains(@class, 'table-responsive')][@data-gridid='" + gridId + "']/ancestor::div[@class='panel-body']//div[contains(@class,'hidden-md')]/parent::div");
        }

        static final Locator pgRightButton = Locator.tagWithClassContaining("div", "paging")
                .descendant(Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-right")));
        static final Locator pgLeftButton =Locator.tagWithClassContaining("div", "paging")
                .descendant(Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-left")));

        static final Locator pagingCountsSpan = Locator.xpath("//div[contains(@class, 'paging')]/span[@data-min]");
        static final Locator.XPathLocator viewSelectorButtonGroup = Locator.tagWithClass("div", "dropdown")
                .withChild(Locator.button("Grid Views"));
        static final Locator.XPathLocator viewSelectorToggleButton = Locator.button("Grid Views");
        static final Locator viewSelectorMenu = Locator.tagWithAttributeContaining("ul", "aria-labelledby", "viewselector");

        static final Locator navTab(String partialTabText)
        {
            return Locator.tagWithClass("ul", "nav nav-tabs").child(Locator.tag("li")
                    .withChild(Locator.linkContainingText(partialTabText)));
        }

    }

    public static class GridBarFinder extends WebDriverComponentFinder<GridBar, GridBarFinder>
    {
        private Locator _locator;
        private WebDriver _driver;
        private ResponsiveGrid _responsiveGrid;

        public GridBarFinder(WebDriver driver, ResponsiveGrid responsiveGrid)
        {
            super(driver);
            _driver = driver;
            _locator= Locators.gridBar();
            _responsiveGrid = responsiveGrid;
        }

        public GridBarFinder withQueryGrid()
        {
            _locator= Locators.queryGridBar();
            return this;
        }

        public GridBarFinder withGridId(String gridId)
        {
            _locator= Locators.gridBar(gridId);
            return this;
        }

        @Override
        protected GridBar construct(WebElement el, WebDriver driver)
        {
            return new GridBar(el, _responsiveGrid, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

    public enum ExportType
    {
        CSV("fa-file-o"),
        EXCEL("fa-file-excel-o"),
        TSV("fa-text-o");

        private final String _cssClass;

        ExportType(String cssClass)
        {
            _cssClass = cssClass;
        }
        public String buttonCssClass()
        {
            return _cssClass;
        }
    }
}