/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class Pager extends WebDriverComponent<Pager.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private final UpdatingComponent _pagedComponent;

    protected Pager(WebElement element, UpdatingComponent component, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _pagedComponent = component;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public Pager jumpToPage(String jumpTo)      // only works on GridPanel
    {
        _pagedComponent.doAndWaitForUpdate(()->
                elementCache().jumpToDropdown.clickSubMenu(jumpTo));
        return this;
    }

    public int getCurrentPage()                 // only works on GridPanel
    {
        return Integer.parseInt(elementCache().jumpToDropdown.getButtonText());
    }

    public Pager selectPageSize(String pageSize)    // only works on GridPanel
    {
        _pagedComponent.doAndWaitForUpdate(()->
                elementCache().pageSizeDropdown.clickSubMenu(pageSize));
        return this;
    }

    public int getPageSize()                // only works on GridPanel
    {
        return Integer.parseInt(elementCache().pageSizeDropdown.getButtonText());
    }

    public Pager clickPrevious()
    {
        elementCache().prevButton().ifPresent(button -> {
            getWrapper().scrollIntoView(button);
            button.click();
        });
        return this;
    }

    public boolean isPreviousEnabled()
    {
        if (!elementCache().prevButton().isPresent())
            return false;

        String prevBtnClass = elementCache().prevButton().get().getAttribute("class");
        if (prevBtnClass.contains("pagination-buttons__prev"))      // this is the new g
            return !elementCache().prevButton().get().getAttribute("disabled").equals("true");
        else
            return !prevBtnClass.contains("disabled-button");
    }

    public Pager clickNext()
    {
        elementCache().nextButton().ifPresent(button -> {
            getWrapper().scrollIntoView(button);
            button.click();
        });
        return this;
    }

    public boolean isNextEnabled()
    {
        if (!elementCache().nextButton().isPresent())
            return false;

        String nextBtnClass = elementCache().nextButton().get().getAttribute("class");
        if (nextBtnClass.contains("pagination-buttons__next"))
            return !elementCache().nextButton().get().getAttribute("disabled").equals("true");
        else
            return !nextBtnClass.contains("disabled-button");
    }

    public int start()
    {
        // note: the 'old' element here and in end() and total() are here to support the pager used in
        // biologicsReportTest.testReportListPaging.  Hopefully, we can consolidate dom between that pager
        // and the ones we now have in QueryGrid

        WebElement oldStart = Locator.tagWithClass("span", "pagination-info__start")
                .findElementOrNull(this);
        if (oldStart != null)
            return Integer.parseInt(oldStart.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-min");
            return Integer.parseInt(value);
        }
    }

    public int end()
    {
        WebElement oldEnd = Locator.tagWithClass("span", "pagination-info__end")
                .findElementOrNull(this);
        if (oldEnd != null)
            return Integer.parseInt(oldEnd.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-max");
            return Integer.parseInt(value);
        }
    }

    public int total()
    {
        WebElement oldTotal = Locator.tagWithClass("span", "pagination-info__total")
                .findElementOrNull(this);
        if (oldTotal != null)
            return Integer.parseInt(oldTotal.getText());
        else
        {
            String value = elementCache().counts.getAttribute("data-total");
            return Integer.parseInt(value);
        }
    }

    /**
     * if the pager does not show a total, it indicates that there are no more to the set than are shown on the current page.
     * use this to understand whether or not there are more pages of information (which
     * could be the whole set, or a filtered range of it, or both).
     * @return
     */
    public boolean hasTotal()
    {
        return elementCache().counts.getAttribute("data-total") != null;
    }

    public String summary()
    {
        return elementCache().counts.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        DropdownButtonGroup jumpToDropdown = new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver())
                .withButtonClass("current-page-dropdown").findWhenNeeded(this);
        DropdownButtonGroup pageSizeDropdown = new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver())
                .withButtonClass("page-size-dropdown").findWhenNeeded(this);

        final Locator.XPathLocator queryGridModelPagingCounts = Locator.tag("span").withAttribute("data-min");
        final Locator.XPathLocator queryModelPagingCounts = Locator.tagWithClass("span", "pagination-info");
        final Locator pagingCountsSpan = Locator.XPathLocator.union(queryGridModelPagingCounts, queryModelPagingCounts);

        Optional<WebElement> prevButton()
        {
            return Locator.XPathLocator.union(
                    Locator.tagWithClass("button", "pagination-button--previous"),     // used in GridPanel
                    Locator.tagWithClass("button", "pagination-buttons__prev"),     // used in ReportList
                    Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-left"))) // used in QueryGridPanel, here for back-support
                    .findOptionalElement(this);
        }

        Optional<WebElement> nextButton()
        {
            return Locator.XPathLocator.union(
                    Locator.tagWithClass("button", "pagination-button--next"), // used in GridPanel
                    Locator.tagWithClass("button", "pagination-buttons__next"), // used in ReportList
                    Locator.tag("button").withChild(Locator.tagWithClass("i", "fa fa-chevron-right")))
                    .findOptionalElement(this);
        }

        public WebElement counts = pagingCountsSpan
                .refindWhenNeeded(getComponentElement()).withTimeout(4000);
    }

    public static class PagerFinder extends WebDriverComponentFinder<Pager, PagerFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.XPathLocator.union(
                Locator.tagWithClass("div", "pagination-buttons"),  // used in biologics report list
                Locator.tagWithClass("span", "paging"),             // used in QueryGridPanel, here for backwards-support
                Locator.tagWithClass("div", "lk-pagination"));      // used in GridPanel
        private final UpdatingComponent _pagedComponent;

        public PagerFinder(WebDriver driver, UpdatingComponent pagedComponent)
        {
            super(driver);
            _pagedComponent = pagedComponent;
        }

        @Override
        protected Pager construct(WebElement el, WebDriver driver)
        {
            return new Pager(el, _pagedComponent, getDriver());
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
