/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.test.util.ext4cmp;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: bimber
 * Date: 11/7/12
 * Time: 8:48 PM
 */
public class Ext4GridRefWD extends Ext4CmpRefWD
{
    private int _clicksToEdit = 2;

    public Ext4GridRefWD(String id, BaseWebDriverTest test)
    {
        super(id, test);
    }

    public void setClicksToEdit(int clicksToEdit)
    {
        _clicksToEdit = clicksToEdit;
    }

    public static Locator locateExt4GridRow(int rowIndex, String parentId)
    {
        String base = "//table[contains(@class, 'x4-grid-table')]";

        if(parentId != null)
            base = "//*[@id='" + parentId + "']" + base;

        return Locator.xpath("(" + base + "//tr[contains(@class, 'x4-grid-row')])[" + rowIndex + "]");
    }

    public Locator getRow(int rowIndex)
    {
        return Ext4GridRefWD.locateExt4GridRow(rowIndex, _id);
    }

    public static Locator locateExt4GridCell(int rowIdx, int cellIndex, String parentId)
    {
        Locator row = Ext4GridRefWD.locateExt4GridRow(rowIdx, parentId);
        return Locator.xpath("(" + ((Locator.XPathLocator) row).getPath() + "//td[contains(@class, 'x4-grid-cell')])[" + cellIndex + "]").append(Locator.tag("div")).notHidden();
    }

    //1-based rowIdx
    public Object getFieldValue(int rowIdx, String fieldName)
    {
        rowIdx--;
        return getEval("store.getAt('" + rowIdx + "').get('" + fieldName + "')");
    }

    //1-based rowIdx
    public Date getDateFieldValue(int rowIdx, String fieldName)
    {
        rowIdx--;
        Long val = (Long)getFnEval("return this.store.getAt('" + rowIdx+ "').get('" + fieldName + "') ? this.store.getAt('" + rowIdx + "').get('" + fieldName + "').getTime() : null");
        return val == null ? null : new Date(val);
    }

    public static Locator locateExt4GridCell(String contents)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text() = '" + contents + "']");
    }

    //uses 1-based coordinates
    public void setGridCellJS(int rowIdx, String columnName, Object value)
    {
        rowIdx--;
        getEval("store.getAt('" + rowIdx + "').set('" + columnName + "', arguments[0])", value);
    }

    public boolean isColumnPresent(String colName, boolean visibleOnly)
    {
        return getIndexOfColumn(colName, visibleOnly, false) > -1;
    }

    //1-based result for consistency w/ other methods
    public int getIndexOfColumn(String column, boolean visibleOnly)
    {
        return getIndexOfColumn(column, visibleOnly, true);
    }

    protected int getIndexOfColumn(String column, boolean visibleOnly, boolean assertPresent)
    {
        int idx = getIndexOfColumn(column, "name", visibleOnly);
        if (idx == -1)
            idx = getIndexOfColumn(column, "dataIndex", visibleOnly);

        if (assertPresent)
            Assert.assertTrue("Unable to find column where either name or dataIndex has value: " + column, idx >= 0);

        return idx;
    }

    //1-based result for consistency w/ other methods
    protected int getIndexOfColumn(String value, String propName, boolean visibleOnly)
    {
        Long idx = (Long)getFnEval("for (var i=0;i<this.columns.length;i++){if (this.columns[i]['"+propName+"'] == '" + value + "') return " + (visibleOnly ? "this.columns[i].getVisibleIndex()" : "i") + ";}; return -1");

        return idx.intValue() > -1 ? idx.intValue() + 1 : -1;
    }

    //uses 1-based coordinates
    @LogMethod
    public void setGridCell(@LoggedParam int rowIdx, @LoggedParam String colName, @LoggedParam String value)
    {
        // NOTE: sometimes this editor is picky about appearing
        // for now, solve this by repeating.  however, it would be better to resolve this issue.
        // one theory is that we need to shift focus prior to the doubleclick
        WebElement el = null;
        int i = 0;
        while (el == null)
        {
            el = startEditing(rowIdx, colName);
            assert i < 4 : "Unable to trigger editor after " + i + " attempts";
            i++;
        }

        Locator moreSpecific = Locator.id(el.getAttribute("id"));
        _test.setFormElement(moreSpecific, value);

        //if the editor is still displayed, try to close it
        if (el.isDisplayed())
        {
            completeEdit();
        }

        Assert.assertFalse("Grid input should not be visible", el.isDisplayed());
        waitForGridEditorToDisappear();
    }

    public void waitForGridEditor()
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return getActiveGridEditor() != null;

            }
        }, "Unable to find element", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForGridEditorToDisappear()
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return getActiveGridEditor() == null;

            }
        }, "Unable to find element", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public int getRowCount()
    {
        return ((Long)getEval("store.getCount()")).intValue();
    }

    public int getSelectedCount()
    {
        return ((Long)getEval("getSelectionModel().getSelection().length;")).intValue();
    }

    public void waitForRowCount(final int count)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return getRowCount() == count;
            }
        }, "Expected row count did not appear", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForSelected(final int count)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return getSelectedCount() == count;
            }
        }, "Expected selected row count did not appear", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public WebElement getActiveGridEditor()
    {
        //TODO: we need a more specific selector
        String selector = "div.x4-grid-editor input";

        List<WebElement> visible = new ArrayList<>();
        for (WebElement element : _test.getDriver().findElements(By.cssSelector(selector)))
        {
            if (element.isDisplayed())
            {
                visible.add(element);
            }
        }

        if (visible.size() > 1)
        {
            throw new RuntimeException("Incorrect number of grid cells found: " + visible.size());
        }

        return visible.size() == 1 ? visible.get(0) : null;
    }

    public Locator getTbarButton(String label)
    {
        return Locator.id(_id).append(Locator.tag("a").withClass("x4-toolbar-item")).append(Locator.tag("span").withText(label));
    }

    public void clickTbarButton(String label)
    {
        _test.waitAndClick(getTbarButton(label));
    }

    //uses 1-based coordinates
    public WebElement startEditing(int rowIdx, String colName)
    {
        int cellIdx = getIndexOfColumn(colName, true);  //NOTE: Ext 4.2.1 seems to not render hidden columns, unlike previous ext versions
        Locator cell = Ext4GridRefWD.locateExt4GridCell(rowIdx, cellIdx, _id);
        _test.assertElementPresent(cell);

        WebElement el = getActiveGridEditor();
        if (el == null)
        {
            if (_clicksToEdit > 1)
                _test.doubleClick(cell);
            else
                _test.click(cell);

            _test.sleep(200);
            el = getActiveGridEditor();
        }

        return el;
    }

    //1-based
    public WebElement startEditingJS(int rowIdx, String colName)
    {
        Integer colIdx = getIndexOfColumn(colName, false);
        completeEdit();

        Boolean didStart = (Boolean)getFnEval("return this.editingPlugin.startEdit(" + (rowIdx-1) + ", " + (colIdx-1) + ");");
        Assert.assertTrue("Unable to start grid edit", didStart);

        waitForGridEditor();

        return getActiveGridEditor();
    }

    public void cancelEdit()
    {
        getFnEval("this.editingPlugin.cancelEdit();");
        waitForGridEditorToDisappear();
    }

    public void completeEdit()
    {
        getFnEval("this.editingPlugin.completeEdit();");
        waitForGridEditorToDisappear();
    }
}
