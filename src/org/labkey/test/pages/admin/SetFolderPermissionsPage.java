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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SetFolderPermissionsPage extends LabKeyPage
{
    public SetFolderPermissionsPage(WebDriver test)
    {
        super(test);
    }

    public void clickFinish()
    {
        doAndWaitForPageToLoad(() -> newElementCache().finishButton.click());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        final WebElement finishButton = Locator.button("Finish").findWhenNeeded(this);

        // TODO: Set security configuration to "My User Only"
        // TODO: "Finish and Configure Permissions"

        // See AbstractContainerHelper.createSubfolder for what it supports and replace it
    }
}