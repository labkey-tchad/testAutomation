/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test;

public abstract class Locators
{
    public static final Locator.XPathLocator ADMIN_MENU = Locator.xpath("id('adminMenuPopupLink')[@onclick]");
    public static final Locator.XPathLocator UX_ADMIN_MENU_TOGGLE = Locator.xpath("//li[contains(@class,'dropdown dropdown-rollup') and ./a/i[@class='fa fa-cog']]");
    //public static final Locator.XPathLocator UX_ADMIN_MENU = Locator.xpath()
    public static final Locator.IdLocator USER_MENU = Locator.id("userMenuPopupLink");
    public static final Locator.XPathLocator UX_USER_MENU = Locator.xpath("//ul[@class='navbar-nav-lk' and ./li/a/i[@class='fa fa-user']]");
    public static final Locator.IdLocator DEVELOPER_MENU = Locator.id("devMenuPopupLink");
    public static final Locator.IdLocator projectBar = Locator.id("projectBar");
    public static final Locator.XPathLocator UX_PROJECT_LIST_CONTAINER = Locator.xpath("//div[contains(@class, 'project-list-container iScroll') and ./p[@class='title' and contains(text(), 'Projects')]]");
    public static final Locator.XPathLocator UX_PROJECT_LIST = Locator.xpath("//div[@class='project-list']");
    public static final Locator.XPathLocator UX_FOLDER_LIST_CONTAINER = Locator.xpath("//div[contains(@class, 'folder-list-container') and ./p[@class='title' and contains(text(), 'Project Folders & Pages')]]");
    public static final Locator.XPathLocator UX_FOLDER_LIST = Locator.xpath("//div[@class='folder-tree_wrap']");
    public static final Locator.IdLocator folderMenu = Locator.id("folderBar");
    public static final Locator.XPathLocator labkeyError = Locator.tagWithClass("*", "labkey-error");
    public static final Locator.XPathLocator alertWarning = Locator.tagWithClass("*", "alert alert-warning");
    public static final Locator signInLink = Locator.tagWithAttributeContaining("a", "href", "login.view");
    public static final Locator.XPathLocator folderTab = Locator.tagWithClass("*", "labkey-folder-header").append(Locator.tagWithClass("ul", "tab-nav")).childTag("li");
    public static final Locator.XPathLocator UX_PAGE_NAV = Locator.xpath("//nav[@class='labkey-page-nav']");
    public static final Locator.XPathLocator UX_FOLDER_TAB = Locator.xpath("//li[@data-webpart='BetaNav']//i[contains(@class, 'fa-folder-open')]");
    public static final Locator.CssLocator labkeyHeader = Locator.css(".labkey-main .header-block");
    public static final Locator.CssLocator labkeyBody = Locator.css(".labkey-main .body-block");

    public static Locator.XPathLocator bodyPanel()
    {
        return LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT ?
                Locator.tagWithClass("div", "lk-body-ct") :
                Locator.id("bodypanel");
    }

    public static Locator.XPathLocator bodyTitle()
    {
        return Locator.tagWithClass("div", "lk-body-title").childTag("h3");
    }
    public static Locator.XPathLocator bodyTitle(String title)
    {
        return bodyTitle().withText(title);
    }

    public static Locator footerPanel()
    {
        return Locator.css(".footer-block");
    }

    public static Locator.XPathLocator bootstrapMenuItem(String text)
    {
        return Locator.xpath("//li/a[contains(text(), " + Locator.xq(text) + ")]");
    }
    public static Locator pageSignal(String signalName)
    {
        return Locator.css("#testSignals > div[name=" + Locator.cq(signalName) + "]");
    }
    public static Locator pageSignal(String signalName, String value)
    {
        return Locator.css("#testSignals > div[name=" + Locator.cq(signalName) + "][value=" + Locator.cq(value) + "]");
    }

    public static Locator termsOfUseCheckbox()
    {
        return Locator.id("approvedTermsOfUse");
    }
}
