package org.labkey.test.components.ext4.widgets;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.html.Input;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.TreeMap;

import static org.labkey.test.components.ext4.ComboBox.ComboBox;
import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.LEADING_NBSP;

public class SearchPanel extends WebDriverComponent<SearchPanel.ElementCache>
{
    private static final String LOAD_SIGNAL = "extSearchPanelLoaded";
    private final WebElement _el;
    private final WebDriver _driver;

    public SearchPanel(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
        Locators.pageSignal(LOAD_SIGNAL).waitForElement(driver, 30000);
    }

    public SearchPanel(String title, WebDriver driver)
    {
        this(Locator.tag("div").attributeStartsWith("id", "labkey-searchpanel-")
                .withDescendant(Locator.tag("div").attributeStartsWith("id", "labkey-searchpanel-").attributeEndsWith("id", "_header").withText(title))
                .waitForElement(driver, 10000), driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public void setView(String view)
    {
        elementCache().viewCombo.selectComboBoxItem(view);
    }

    public void setFilter(String fieldLabel, @Nullable String operator, String value)
    {
        if (operator != null)
        {
            elementCache().findFilterRow(fieldLabel)
                    .operator()
                    .selectComboBoxItem(operator);
        }
        elementCache().findFilterRow(fieldLabel)
                .value()
                .set(value);
    }

    public void selectValues(String fieldLabel, String... values)
    {
        elementCache().findFacetedRow(fieldLabel)
                .value()
                .selectComboBoxItem(LEADING_NBSP, values);
    }

    public DataRegionTable submit()
    {
        getWrapper().clickAndWait(elementCache().submitButton);
        return new DataRegionTable("query", getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        private final Map<String, SearchPanelFilterRow> filterRows = new TreeMap<>();
        protected SearchPanelFilterRow findFilterRow(String label)
        {
            if (!filterRows.containsKey(label))
                filterRows.put(label, new SearchPanelFilterRow(label));
            return filterRows.get(label);
        }

        private final Map<String, SearchPanelFacetedRow> facetedRows = new TreeMap<>();
        protected SearchPanelFacetedRow findFacetedRow(String label)
        {
            if (!facetedRows.containsKey(label))
                facetedRows.put(label, new SearchPanelFacetedRow(label));
            return facetedRows.get(label);
        }

        protected final ComboBox viewCombo = ComboBox(getDriver()).locatedBy(Locator.tag("table").attributeStartsWith("id", "labkey-viewcombo")).findWhenNeeded(this);
        protected final ComboBox containerCombo = null;
        protected final WebElement submitButton = Ext4Helper.Locators.ext4Button("Submit").findWhenNeeded(this);
    }

    protected abstract class SearchPanelRow extends Component
    {
        private final WebElement row;
        private final String label;

        protected SearchPanelRow(String rowLabel)
        {
            this.label = rowLabel;
            row = Locator.tagWithClass("div", "search-panel-row")
                    .withDescendant(Locator.tagWithClass("div", "search-panel-row-label").withText(rowLabel + ":"))
                    .findElement(SearchPanel.this);
        }

        @Override
        public WebElement getComponentElement()
        {
            return row;
        }

        public String getLabel()
        {
            return label;
        }
    }

    protected class SearchPanelFilterRow extends SearchPanelRow
    {
        private final ComboBox operatorCombo = ComboBox(getDriver())
                .locatedBy(Locator.tagWithClass("table", "search-panel-row-operator")).findWhenNeeded(this);
        private final Input fieldValueInput = Input(Locator.css(".search-panel-row-value input"), getDriver()).findWhenNeeded(this);

        protected SearchPanelFilterRow(String rowLabel)
        {
            super(rowLabel);
        }

        public ComboBox operator()
        {
            return operatorCombo;
        }

        public Input value()
        {
            return fieldValueInput;
        }
    }

    protected class SearchPanelFacetedRow extends SearchPanelRow
    {
        private final ComboBox valueCombo = ComboBox(getDriver())
                .locatedBy(Locator.css(".search-panel-row-value")).findWhenNeeded(this);

        protected SearchPanelFacetedRow(String rowLabel)
        {
            super(rowLabel);
        }

        public ComboBox value()
        {
            return valueCombo;
        }
    }
}