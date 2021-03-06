package org.labkey.test.components.labkey.ui.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public abstract class SampleTypeDesigner<T extends SampleTypeDesigner<T>> extends EntityTypeDesigner<T>
{
    public final static String CURRENT_SAMPLE_TYPE = "(Current Sample Type)";

    public SampleTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected abstract T getThis();

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    public T addParentAlias(String alias)
    {
        return addParentAlias(alias, null);
    }

    public T addParentAlias(String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();
        int initialCount = elementCache().parentAliases().size();
        elementCache().addAliasButton.click();
        if (optionDisplayText == null)
        {
            optionDisplayText = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, optionDisplayText);
        return getThis();
    }

    private int getParentAliasIndex(String parentAlias)
    {
        List<Input> inputs = elementCache().parentAliases();
        for (int i = 0; i < inputs.size(); i++)
        {
            if (inputs.get(i).get().equals(parentAlias))
            {
                return i;
            }
        }
        throw new NotFoundException("No such parent alias: " + parentAlias);
    }

    public T removeParentAlias(String parentAlias)
    {
        expandPropertiesPanel();
        int aliasIndex = getParentAliasIndex(parentAlias);
        return removeParentAlias(aliasIndex);
    }

    public T removeParentAlias(int index)
    {
        expandPropertiesPanel();
        elementCache().removeParentAliasIcon(index).click();
        return getThis();
    }

    public T setParentAlias(int index, @Nullable String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();
        elementCache().parentAlias(index).setValue(alias);
        if (optionDisplayText != null)
        {
            elementCache().parentAliasSelect(index).select(optionDisplayText);
        }
        return getThis();
    }

    public T setParentAlias(String alias, String optionDisplayText)
    {
        expandPropertiesPanel();
        int index = getParentAliasIndex(alias);
        elementCache().parentAliasSelect(index).select(optionDisplayText);
        return getThis();
    }

    public String getParentAlias(int index)
    {
        expandPropertiesPanel();
        return elementCache().parentAlias(index).get();
    }

    public String getParentAliasSelectText(int index)
    {
        expandPropertiesPanel();
        return elementCache().parentAliasSelect(index).getSelections().get(0);
    }

    protected class ElementCache extends EntityTypeDesigner<T>.ElementCache
    {
        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);

        protected List<Input> parentAliases()
        {
            return Input.Input(Locator.name("alias"), getDriver()).findAll(propertiesPanel);
        }

        protected Input parentAlias(int index)
        {
            return parentAliases().get(index);
        }

        protected ReactSelect parentAliasSelect(int index)
        {
            return ReactSelect.finder(getDriver()).locatedBy(Locator.byClass("sampleset-insert--parent-select"))
                    .index(index).find(propertiesPanel);
        }

        protected WebElement removeParentAliasIcon(int index)
        {
            return Locator.tagWithClass("i","container--removal-icon").findElements(propertiesPanel).get(index);
        }
    }
}
